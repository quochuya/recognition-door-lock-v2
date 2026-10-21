package com.example.lock.config;

import com.example.lock.repository.*;
import com.example.lock.room.entity.Building;
import com.example.lock.room.entity.Floor;
import com.example.lock.room.entity.LockDevice;
import com.example.lock.room.entity.Room;
import com.example.lock.user.entity.Role;
import com.example.lock.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chạy 1 lần khi start app:
 *  1. Nếu chưa có ADMIN nào -> tự tạo tài khoản mặc định để login được ngay
 *  2. Nếu chưa có Building nào -> tự tạo 1 tòa mẫu (3 tầng, vài phòng có
 *     sẵn thiết bị) để UI có dữ liệu ngay khi mở lên test, không phải insert
 *     tay qua pgAdmin trước mới thấy được gì.
 *
 * Xoá seed phòng/tòa mẫu này khi lên môi trường thật (hoặc chỉ chạy seed khi
 * spring.profiles.active=dev — cải tiến sau nếu cần).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_EMAIL = "admin@smartlock.local";
    private static final String DEFAULT_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final UserService userService;
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;
    private final LockDeviceRepository lockDeviceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedSampleBuilding();
    }

    private void seedAdmin() {
        boolean hasAdmin = userRepository.findAll().stream().anyMatch(u -> u.getRole() == Role.ADMIN);
        if (hasAdmin) return;

        userService.createUser("Administrator", DEFAULT_EMAIL, DEFAULT_PASSWORD, null, Role.ADMIN);
        log.warn("==============================================================");
        log.warn(" Đã tạo tài khoản ADMIN mặc định — ĐỔI MẬT KHẨU NGAY:");
        log.warn(" Email:    {}", DEFAULT_EMAIL);
        log.warn(" Password: {}", DEFAULT_PASSWORD);
        log.warn("==============================================================");
    }

    private void seedSampleBuilding() {
        if (buildingRepository.count() > 0) return;

        Building buildingA = buildingRepository.save(Building.builder()
                .name("Tòa A").address("123 Đường ABC, Hà Nội").build());

        for (int level = 1; level <= 3; level++) {
            Floor floor = floorRepository.save(Floor.builder().level(level).building(buildingA).build());

            int roomsOnThisFloor = level == 1 ? 4 : 3;
            for (int i = 1; i <= roomsOnThisFloor; i++) {
                int roomNumber = level * 100 + i;
                Room room = roomRepository.save(Room.builder().roomNumber(roomNumber).floor(floor).build());

                lockDeviceRepository.save(LockDevice.builder()
                        .deviceCode("ESP32-" + roomNumber)
                        .room(room)
                        .lockState(LockDevice.LockState.LOCKED)
                        .connectionStatus(LockDevice.ConnectionStatus.OFFLINE)
                        .batteryPercent(100)
                        .build());
            }
        }

        log.info("Đã tạo dữ liệu mẫu: Tòa A, 3 tầng, 10 phòng kèm thiết bị (ESP32-101 .. ESP32-303)");
    }
}
