package com.example.lock.report.service;

import com.example.lock.repository.AccessLogRepository;
import com.example.lock.repository.LockDeviceRepository;
import com.example.lock.report.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Map<String, String> RULE_LABELS = Map.of(
            "RATE_LIMIT", "Nhiều lần thất bại liên tiếp (nghi dò UID)",
            "ROOM_PERMISSION", "Không có quyền truy cập phòng",
            "CARD_STATUS", "Thẻ bị khóa hoặc hết hạn",
            "TIME_WINDOW", "Truy cập ngoài khung giờ cho phép",
            "DEVICE_ONLINE", "Thiết bị mất kết nối khi điều khiển từ xa"
    );

    private final AccessLogRepository accessLogRepository;
    private final LockDeviceRepository lockDeviceRepository;

    public ReportSummaryDto getSummary(LocalDateTime from, LocalDateTime to, Long buildingId) {
        long success = accessLogRepository.countSuccess(from, to, buildingId);
        long denied = accessLogRepository.countDenied(from, to, buildingId);
        long total = success + denied;
        double successRate = total == 0 ? 100.0 : (success * 100.0 / total);

        return ReportSummaryDto.builder()
                .totalUnlock(success)
                .deniedCount(denied)
                .successRate(Math.round(successRate * 10) / 10.0)
                .lowBatteryDevices(lockDeviceRepository.countByBatteryPercentLessThan(20))
                .build();
    }

    public List<DailyActivityDto> getActivityTrend(LocalDateTime from, LocalDateTime to, Long buildingId) {
        return accessLogRepository.dailyActivity(from, to, buildingId).stream()
                .map(row -> DailyActivityDto.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .successCount(((Number) row[1]).longValue())
                        .deniedCount(((Number) row[2]).longValue())
                        .build())
                .toList();
    }

    public List<TopRoomDto> getTopRooms(LocalDateTime from, LocalDateTime to, Long buildingId, int limit) {
        return accessLogRepository.topRooms(from, to, buildingId, PageRequest.of(0, limit)).stream()
                .map(row -> TopRoomDto.builder()
                        .roomLabel((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<BuildingActivityDto> getBuildingComparison(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> activityByBuilding = new LinkedHashMap<>();
        accessLogRepository.buildingActivity(from, to)
                .forEach(row -> activityByBuilding.put((String) row[0], ((Number) row[1]).longValue()));

        Map<String, Long> offlineByBuilding = new LinkedHashMap<>();
        lockDeviceRepository.offlineCountByBuilding()
                .forEach(row -> offlineByBuilding.put((String) row[0], ((Number) row[1]).longValue()));

        return activityByBuilding.entrySet().stream()
                .map(e -> BuildingActivityDto.builder()
                        .buildingName(e.getKey())
                        .activityCount(e.getValue())
                        .offlineDeviceCount(offlineByBuilding.getOrDefault(e.getKey(), 0L))
                        .build())
                .toList();
    }

    public DeviceHealthDto getDeviceHealth() {
        return DeviceHealthDto.builder()
                .batteryGood(lockDeviceRepository.countByBatteryPercentGreaterThanEqual(50))
                .batteryMedium(lockDeviceRepository.countByBatteryPercentBetween(20, 49))
                .batteryLow(lockDeviceRepository.countByBatteryPercentLessThan(20))
                .online(lockDeviceRepository.countByConnectionStatus(
                        com.example.lock.room.entity.LockDevice.ConnectionStatus.ONLINE))
                .offline(lockDeviceRepository.countByConnectionStatus(
                        com.example.lock.room.entity.LockDevice.ConnectionStatus.OFFLINE))
                .build();
    }

    public List<SecurityEventDto> getSecurityEvents(LocalDateTime from, LocalDateTime to, Long buildingId) {
        return accessLogRepository.denyReasonBreakdown(from, to, buildingId).stream()
                .map(row -> {
                    String ruleKey = (String) row[0];
                    return SecurityEventDto.builder()
                            .ruleKey(ruleKey)
                            .label(RULE_LABELS.getOrDefault(ruleKey, ruleKey))
                            .count(((Number) row[1]).longValue())
                            .build();
                })
                .toList();
    }

    /** Helper cho controller: mặc định 7 ngày gần nhất nếu không truyền from/to */
    public LocalDateTime[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(6);
        return new LocalDateTime[]{resolvedFrom.atStartOfDay(), resolvedTo.atTime(23, 59, 59)};
    }
}
