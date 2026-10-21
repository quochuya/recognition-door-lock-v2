package com.example.lock.controller;

import com.example.lock.controller.dto.LoginRequest;
import com.example.lock.controller.dto.LoginResponse;
import com.example.lock.controller.dto.SelfChangePasswordRequest;
import com.example.lock.repository.UserRepository;
import com.example.lock.security.JwtService;
import com.example.lock.user.entity.User;
import com.example.lock.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    @Transactional(readOnly = true)

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (Exception e) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @GetMapping("/me")
    public LoginResponse me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return LoginResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }


    @PostMapping("/change-password")
    public void changeOwnPassword(Authentication authentication, @Valid @RequestBody SelfChangePasswordRequest req) {
        User user = (User) authentication.getPrincipal();

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu cũ không đúng");
        }
        userService.changePassword(user.getId(), req.getNewPassword());
    }
}
