package com.example.lock.report.controller;

import com.example.lock.report.dto.*;
import com.example.lock.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ endpoint nhận chung 3 tham số optional:
 *   from, to (yyyy-MM-dd)  -> mặc định 7 ngày gần nhất nếu bỏ trống
 *   buildingId              -> mặc định tất cả tòa nếu bỏ trống (null)
 *
 * Khớp trực tiếp với các khối trên trang "Báo cáo & Thống kê":
 *   /summary            -> 4 thẻ KPI đầu trang
 *   /activity-trend      -> biểu đồ vùng xu hướng hoạt động
 *   /top-rooms           -> bảng xếp hạng phòng hoạt động nhiều nhất
 *   /building-comparison -> biểu đồ cột so sánh tòa nhà
 *   /device-health        -> 2 biểu đồ tròn (pin, kết nối)
 *   /security-events      -> danh sách sự kiện an ninh đáng chú ý
 *
 * ADMIN + STAFF only — số liệu toàn hệ thống không dành cho GUEST/RESIDENT.
 */
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ReportSummaryDto summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long buildingId) {
        LocalDateTime[] range = reportService.resolveRange(from, to);
        return reportService.getSummary(range[0], range[1], buildingId);
    }

    @GetMapping("/activity-trend")
    public List<DailyActivityDto> activityTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long buildingId) {
        LocalDateTime[] range = reportService.resolveRange(from, to);
        return reportService.getActivityTrend(range[0], range[1], buildingId);
    }

    @GetMapping("/top-rooms")
    public List<TopRoomDto> topRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(defaultValue = "5") int limit) {
        LocalDateTime[] range = reportService.resolveRange(from, to);
        return reportService.getTopRooms(range[0], range[1], buildingId, limit);
    }

    @GetMapping("/building-comparison")
    public List<BuildingActivityDto> buildingComparison(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDateTime[] range = reportService.resolveRange(from, to);
        return reportService.getBuildingComparison(range[0], range[1]);
    }

    @GetMapping("/device-health")
    public DeviceHealthDto deviceHealth() {
        return reportService.getDeviceHealth();
    }

    @GetMapping("/security-events")
    public List<SecurityEventDto> securityEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long buildingId) {
        LocalDateTime[] range = reportService.resolveRange(from, to);
        return reportService.getSecurityEvents(range[0], range[1], buildingId);
    }
}
