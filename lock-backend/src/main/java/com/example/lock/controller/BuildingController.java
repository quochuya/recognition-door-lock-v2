package com.example.lock.controller;

import com.example.lock.controller.dto.*;
import com.example.lock.repository.BuildingRepository;
import com.example.lock.repository.FloorRepository;
import com.example.lock.repository.RoomRepository;
import com.example.lock.room.entity.Building;
import com.example.lock.room.entity.Floor;
import com.example.lock.room.entity.Room;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD đầy đủ cho Tòa nhà / Tầng / Phòng — nền tảng bắt buộc phải có trước
 * khi gắn thiết bị (LockDevice luôn cần 1 Room có sẵn).
 *
 * Xem (GET): bất kỳ ai đã đăng nhập. Tạo/sửa/xoá: ADMIN only — đây là cấu
 * trúc hạ tầng vật lý của toà nhà, không giao cho STAFF tự ý thay đổi.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;

    // ---------- Building ----------

    @GetMapping("/buildings")
    public List<BuildingDto> listBuildings() {
        return buildingRepository.findAll().stream().map(BuildingDto::from).toList();
    }

    @GetMapping("/buildings/{buildingId}")
    public BuildingDto getBuilding(@PathVariable Long buildingId) {
        return BuildingDto.from(findBuilding(buildingId));
    }

    @PostMapping("/buildings")
    @PreAuthorize("hasRole('ADMIN')")
    public BuildingDto createBuilding(@Valid @RequestBody BuildingRequest req) {
        Building building = buildingRepository.save(Building.builder()
                .name(req.getName()).address(req.getAddress()).build());
        return BuildingDto.from(building);
    }

    @PutMapping("/buildings/{buildingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public BuildingDto updateBuilding(@PathVariable Long buildingId, @Valid @RequestBody BuildingRequest req) {
        Building building = findBuilding(buildingId);
        building.setName(req.getName());
        building.setAddress(req.getAddress());
        return BuildingDto.from(buildingRepository.save(building));
    }

    @DeleteMapping("/buildings/{buildingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBuilding(@PathVariable Long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new IllegalArgumentException("Không tìm thấy tòa nhà id=" + buildingId);
        }
        // Không tự cascade xoá Floor/Room/LockDevice bên trong — nếu còn dữ liệu
        // con, DB sẽ chặn (khoá ngoại) và GlobalExceptionHandler trả lỗi rõ ràng.
        // Buộc admin phải xoá hết Floor bên trong trước — tránh xoá nhầm cả toà.
        buildingRepository.deleteById(buildingId);
    }

    // ---------- Floor ----------

    @GetMapping("/buildings/{buildingId}/floors")
    public List<FloorDto> listFloors(@PathVariable Long buildingId) {
        return floorRepository.findByBuildingIdOrderByLevel(buildingId).stream().map(FloorDto::from).toList();
    }

    @PostMapping("/buildings/{buildingId}/floors")
    @PreAuthorize("hasRole('ADMIN')")
    public FloorDto createFloor(@PathVariable Long buildingId, @Valid @RequestBody FloorRequest req) {
        Building building = findBuilding(buildingId);
        Floor floor = floorRepository.save(Floor.builder().level(req.getLevel()).building(building).build());
        return FloorDto.from(floor);
    }

    @PutMapping("/floors/{floorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public FloorDto updateFloor(@PathVariable Long floorId, @Valid @RequestBody FloorRequest req) {
        Floor floor = findFloor(floorId);
        floor.setLevel(req.getLevel());
        return FloorDto.from(floorRepository.save(floor));
    }

    @DeleteMapping("/floors/{floorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteFloor(@PathVariable Long floorId) {
        if (!floorRepository.existsById(floorId)) {
            throw new IllegalArgumentException("Không tìm thấy tầng id=" + floorId);
        }
        floorRepository.deleteById(floorId);
    }

    // ---------- Room ----------
    // (GET /api/floors/{floorId}/rooms trả RoomStatusDto kèm thiết bị — nằm ở LockController)

    @PostMapping("/floors/{floorId}/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public RoomDto createRoom(@PathVariable Long floorId, @Valid @RequestBody RoomRequest req) {
        Floor floor = findFloor(floorId);
        Room room = roomRepository.save(Room.builder().roomNumber(req.getRoomNumber()).floor(floor).build());
        return RoomDto.from(room);
    }

    @PutMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public RoomDto updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomRequest req) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng id=" + roomId));
        room.setRoomNumber(req.getRoomNumber());
        return RoomDto.from(roomRepository.save(room));
    }

    @DeleteMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(@PathVariable Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Không tìm thấy phòng id=" + roomId);
        }
        // Nếu phòng còn gắn LockDevice, DB sẽ chặn xoá — phải gỡ thiết bị trước
        // (DELETE /api/devices/{id}) để tránh mất dữ liệu thiết bị ngoài ý muốn.
        roomRepository.deleteById(roomId);
    }

    // ---------- Helpers ----------

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tòa nhà id=" + id));
    }

    private Floor findFloor(Long id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tầng id=" + id));
    }
}
