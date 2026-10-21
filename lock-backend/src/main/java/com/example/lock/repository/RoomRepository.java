package com.example.lock.repository;

import com.example.lock.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByFloorIdOrderByRoomNumber(Long floorId);
}
