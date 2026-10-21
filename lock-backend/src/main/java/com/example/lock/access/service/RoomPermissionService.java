package com.example.lock.access.service;

import com.example.lock.access.entity.RoomPermission;
import com.example.lock.repository.RoomPermissionRepository;
import com.example.lock.repository.RoomRepository;
import com.example.lock.repository.UserRepository;
import com.example.lock.room.entity.Room;
import com.example.lock.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomPermissionService {

    private final RoomPermissionRepository roomPermissionRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final WhitelistService whitelistService;

    @Transactional
    public RoomPermission grant(Long userId, Long roomId, LocalDateTime expiresAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + userId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng id=" + roomId));

        RoomPermission permission = roomPermissionRepository.findByUserIdAndRoomId(userId, roomId)
                .orElse(RoomPermission.builder().user(user).room(room).build());

        permission.setGrantedAt(LocalDateTime.now());
        permission.setExpiresAt(expiresAt);
        RoomPermission saved = roomPermissionRepository.save(permission);
        whitelistService.publishForRoom(roomId);
        return saved;
    }

    @Transactional
    public void revoke(Long userId, Long roomId) {
        roomPermissionRepository.deleteByUserIdAndRoomId(userId, roomId);
        whitelistService.publishForRoom(roomId);
    }

    public List<RoomPermission> listForUser(Long userId) {
        return roomPermissionRepository.findByUserId(userId);
    }
}
