package com.example.lock.user.controller;

import com.example.lock.access.entity.RoomPermission;
import com.example.lock.access.service.RoomPermissionService;
import com.example.lock.controller.dto.*;
import com.example.lock.user.entity.User;
import com.example.lock.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET  /api/users?search=       -> danh sách (tìm theo tên/email)
 * GET  /api/users/{id}          -> chi tiết + danh sách phòng được cấp quyền
 * POST /api/users               -> tạo mới
 * PUT  /api/users/{id}          -> cập nhật thông tin cơ bản
 * POST /api/users/{id}/change-password
 * POST /api/users/{id}/deactivate | /reactivate  -> vô hiệu hoá thay vì xoá cứng
 *
 * POST   /api/users/{id}/permissions            -> cấp quyền vào 1 phòng
 * DELETE /api/users/{id}/permissions/{roomId}    -> thu hồi quyền phòng đó
 *
 * Toàn bộ module này ADMIN only — quản lý tài khoản người khác và cấp quyền
 * ra vào là thao tác nhạy cảm nhất hệ thống. Tự đổi mật khẩu bản thân (không
 * cần quyền ADMIN) dùng POST /api/auth/change-password thay vì endpoint này.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final RoomPermissionService roomPermissionService;

    @GetMapping
    public List<UserDto> list(@RequestParam(required = false) String search) {
        return userService.search(search).stream().map(UserDto::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UserDetailDto detail(@PathVariable Long id) {
        User user = userService.getOrThrow(id);
        List<RoomPermission> permissions = roomPermissionService.listForUser(id);
        return UserDetailDto.from(user, permissions);
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        User user = userService.createUser(req.getFullName(), req.getEmail(), req.getPassword(), req.getPhone(), req.getRole());
        return UserDto.from(user);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        User user = userService.update(id, req.getFullName(), req.getPhone(), req.getRole(), req.isActive());
        return UserDto.from(user);
    }

    @PostMapping("/{id}/change-password")
    public void changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(id, req.getNewPassword());
    }

    @PostMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        userService.deactivate(id);
    }

    @PostMapping("/{id}/reactivate")
    public void reactivate(@PathVariable Long id) {
        userService.reactivate(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    // --- Quyền truy cập phòng ---

    @PostMapping("/{id}/permissions")
    public RoomPermissionDto grantPermission(@PathVariable Long id, @Valid @RequestBody GrantRoomPermissionRequest req) {
        RoomPermission permission = roomPermissionService.grant(id, req.getRoomId(), req.getExpiresAt());
        return RoomPermissionDto.from(permission);
    }

    @DeleteMapping("/{id}/permissions/{roomId}")
    public void revokePermission(@PathVariable Long id, @PathVariable Long roomId) {
        roomPermissionService.revoke(id, roomId);
    }
}
