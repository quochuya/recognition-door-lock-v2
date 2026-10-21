package com.example.lock.user.service;

import com.example.lock.repository.UserRepository;
import com.example.lock.user.entity.Role;
import com.example.lock.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String fullName, String email, String rawPassword, String phone, Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email đã tồn tại: " + email);
        }
        return userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build());
    }

    public List<User> search(String search) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return userRepository.search(normalized);
    }

    public User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + id));
    }

    @Transactional
    public User update(Long id, String fullName, String phone, Role role, boolean active) {
        User user = getOrThrow(id);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String newRawPassword) {
        User user = getOrThrow(userId);
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /**
     * Không xoá cứng — User còn được RfidCard/AccessLog/RoomPermission tham chiếu
     * (foreign key). Vô hiệu hoá (active=false) để chặn đăng nhập + tự động bị
     * CustomUserDetailsService từ chối, nhưng vẫn giữ nguyên lịch sử/thẻ liên quan.
     */
    @Transactional
    public void deactivate(Long id) {
        User user = getOrThrow(id);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void reactivate(Long id) {
        User user = getOrThrow(id);
        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Xoá cứng — chỉ dùng khi chắc chắn user này KHÔNG còn RfidCard/AccessLog/
     * RoomPermission tham chiếu (vd tài khoản test tạo nhầm). Nếu còn dữ liệu
     * liên quan, DB sẽ chặn và GlobalExceptionHandler trả lỗi rõ ràng —
     * trường hợp bình thường nên dùng deactivate() thay vì xoá cứng.
     */
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy user id=" + id);
        }
        userRepository.deleteById(id);
    }
}
