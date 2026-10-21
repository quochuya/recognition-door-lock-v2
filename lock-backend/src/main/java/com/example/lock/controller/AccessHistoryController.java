package com.example.lock.controller;

import com.example.lock.access.entity.AccessMethod;
import com.example.lock.controller.dto.AccessLogDto;
import com.example.lock.repository.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GET /api/access-logs?buildingId=&method=RFID&success=false&from=2025-07-06&to=2025-07-12&search=an&page=0&size=8
 *
 * Mọi tham số đều optional:
 *   - from/to     mặc định 7 ngày gần nhất nếu bỏ trống
 *   - buildingId  bỏ trống = tất cả tòa
 *   - method      RFID | PIN | APP | FACE, bỏ trống = tất cả
 *   - success     true (chỉ thành công) | false (chỉ bị từ chối) | bỏ trống = tất cả
 *   - search      tìm theo tên người dùng hoặc số phòng
 *   - page/size   phân trang chuẩn Spring (mặc định page=0, size=8 khớp UI cũ)
 */
@RestController
@RequestMapping("/api/access-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AccessHistoryController {

    private final AccessLogRepository accessLogRepository;
    @Transactional(readOnly = true)
    @GetMapping
    public Page<AccessLogDto> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) AccessMethod method,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusDays(6);
        LocalDateTime fromTs = resolvedFrom.atStartOfDay();
        LocalDateTime toTs = resolvedTo.atTime(23, 59, 59);

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();

        return accessLogRepository
                .findFiltered(fromTs, toTs, buildingId, method, success, normalizedSearch, PageRequest.of(page, size))
                .map(AccessLogDto::from);
    }
}
