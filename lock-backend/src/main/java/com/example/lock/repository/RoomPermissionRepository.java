package com.example.lock.repository;

import com.example.lock.access.entity.RoomPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomPermissionRepository extends JpaRepository<RoomPermission, Long> {
    Optional<RoomPermission> findByUserIdAndRoomId(Long userId, Long roomId);
    List<RoomPermission> findByUserId(Long userId);
    void deleteByUserIdAndRoomId(Long userId, Long roomId);
}
