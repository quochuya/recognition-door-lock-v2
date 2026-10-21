package com.example.lock.security;

import com.example.lock.repository.UserRepository;
import com.example.lock.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("[JWT] {} -> KHÔNG có header Authorization, cho qua như request ẩn danh", path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            log.warn("[JWT] {} -> Token KHÔNG HỢP LỆ hoặc ĐÃ HẾT HẠN. Đây thường là nguyên nhân 403 'giả'.", path);
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractEmail(token);
        var userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("[JWT] {} -> Token hợp lệ (email={}) nhưng KHÔNG TÌM THẤY user này trong DB.", path, email);
            filterChain.doFilter(request, response);
            return;
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("[JWT] {} -> User {} tồn tại nhưng ĐÃ BỊ VÔ HIỆU HOÁ (active=false).", path, email);
            filterChain.doFilter(request, response);
            return;
        }

        var authToken = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        request.setAttribute("currentUser", user);
        log.debug("[JWT] {} -> OK, xác thực thành công: {} (role={})", path, email, user.getRole());

        filterChain.doFilter(request, response);
    }
}
