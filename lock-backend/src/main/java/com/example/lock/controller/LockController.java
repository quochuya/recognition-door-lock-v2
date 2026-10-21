package com.example.lock.controller;

import com.example.lock.access.result.AccessResult;
import com.example.lock.access.service.AccessService;
import com.example.lock.controller.dto.LockCommandResponse;
import com.example.lock.controller.dto.RoomStatusDto;
import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.repository.RoomRepository;
import com.example.lock.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LockController {

    private final RoomRepository roomRepository;
    private final LockDeviceRepository lockDeviceRepository;
    private final AccessService accessService;
    @Transactional(readOnly = true)

    @GetMapping("/floors/{floorId}/rooms")
    public List<RoomStatusDto> getRoomsByFloor(@PathVariable Long floorId) {
        return roomRepository.findByFloorIdOrderByRoomNumber(floorId).stream()
                .map(room -> RoomStatusDto.from(room, lockDeviceRepository.findByRoomId(room.getId()).orElse(null)))
                .toList();
    }

    @PostMapping("/locks/{lockDeviceId}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','SECURITY')")
    public LockCommandResponse unlock(@PathVariable Long lockDeviceId, Authentication authentication) {
        AccessResult result = accessService.handleRemoteControl(lockDeviceId, currentUser(authentication), true);
        return LockCommandResponse.from(result);
    }

    @PostMapping("/locks/{lockDeviceId}/lock")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','SECURITY')")
    public LockCommandResponse lock(@PathVariable Long lockDeviceId, Authentication authentication) {
        AccessResult result = accessService.handleRemoteControl(lockDeviceId, currentUser(authentication), false);
        return LockCommandResponse.from(result);
    }

    /**
     * JwtAuthFilter đã set principal = User entity thật (không phải chuỗi username),
     * nên chỉ cần ép kiểu — không cần query DB lại lần nữa.
     */
    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
