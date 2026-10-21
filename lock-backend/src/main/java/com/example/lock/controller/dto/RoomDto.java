package com.example.lock.controller.dto;

import com.example.lock.room.entity.Room;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomDto {
    private Long id;
    private Integer roomNumber;
    private Long floorId;

    public static RoomDto from(Room r) {
        return RoomDto.builder().id(r.getId()).roomNumber(r.getRoomNumber()).floorId(r.getFloor().getId()).build();
    }
}
