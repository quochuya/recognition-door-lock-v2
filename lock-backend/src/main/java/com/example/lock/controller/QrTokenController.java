package com.example.lock.controller;

import com.example.lock.access.entity.QrToken;
import com.example.lock.access.service.QrTokenService;
import com.example.lock.controller.dto.GenerateQrRequest;
import com.example.lock.controller.dto.QrTokenDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ADMIN sinh vé QR tạm thời cho 1 user vào 1 phòng cụ thể — dùng cho khách
 * vãng lai, người giao hàng, hoặc user chính quên mang thẻ.
 *
 * Nội dung trả về (`token`) chính là chuỗi cần mã hoá thành ảnh QR — dùng thư
 * viện JS phía frontend (vd `qrcode.react`) để vẽ ra, KHÔNG cần backend tự vẽ ảnh.
 */
@RestController
@RequestMapping("/api/qr-tokens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class QrTokenController {

    private final QrTokenService qrTokenService;

    @PostMapping
    public QrTokenDto generate(@Valid @RequestBody GenerateQrRequest req) {
        QrToken token = qrTokenService.generate(req.getUserId(), req.getRoomId(), req.getValidityMinutes());
        return QrTokenDto.from(token);
    }
}
