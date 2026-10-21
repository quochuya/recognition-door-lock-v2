package com.example.lock.controller.dto;

import com.example.lock.access.entity.RoomPermission;
import com.example.lock.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserDetailDto {
    private UserDto user;
    private List<RoomPermissionDto> permissions;

    public static UserDetailDto from(User user, List<RoomPermission> permissions) {
        return UserDetailDto.builder()
                .user(UserDto.from(user))
                .permissions(permissions.stream().map(RoomPermissionDto::from).toList())
                .build();
    }
}
