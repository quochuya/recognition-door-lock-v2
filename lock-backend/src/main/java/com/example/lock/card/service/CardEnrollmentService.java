package com.example.lock.card.service;

import com.example.lock.card.entity.PendingCardScan;
import com.example.lock.repository.PendingCardScanRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * "One-touch enrollment": admin bấm 1 nút bật chế độ trên dashboard, đưa thẻ
 * mới lại gần bất kỳ đầu đọc nào — UID tự động xuất hiện trên dashboard
 * (real-time qua WebSocket) để admin chọn user gán, không cần gõ tay UID.
 *
 * Đơn giản hoá: 1 flag toàn hệ thống. Nếu cần bật riêng theo từng đầu đọc,
 * đổi enrollmentActive thành Map<deviceCode, Boolean>.
 */
@Service
@RequiredArgsConstructor
public class CardEnrollmentService {

    private final PendingCardScanRepository pendingCardScanRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final AtomicBoolean enrollmentActive = new AtomicBoolean(false);

    public void startEnrollment() {
        enrollmentActive.set(true);
    }

    public void stopEnrollment() {
        enrollmentActive.set(false);
    }

    public boolean isEnrollmentActive() {
        return enrollmentActive.get();
    }

    /** Gọi từ DeviceMessageListener khi UID chưa tồn tại và enrollment đang bật */
    @Transactional
    public void captureUnknownScan(String uid, String deviceCode) {
        if (pendingCardScanRepository.findFirstByUidAndStatus(uid, PendingCardScan.Status.WAITING).isPresent()) {
            return; // tránh tạo trùng nếu vẫn còn 1 bản ghi WAITING của UID này
        }
        PendingCardScan pending = pendingCardScanRepository.save(PendingCardScan.builder()
                .uid(uid)
                .deviceCode(deviceCode)
                .status(PendingCardScan.Status.WAITING)
                .build());

        messagingTemplate.convertAndSend("/topic/card-enrollment", PendingCardEvent.builder()
                .id(pending.getId())
                .uid(pending.getUid())
                .deviceCode(pending.getDeviceCode())
                .scannedAt(pending.getScannedAt())
                .build());
    }

    public List<PendingCardScan> listWaiting() {
        return pendingCardScanRepository.findByStatusOrderByScannedAtDesc(PendingCardScan.Status.WAITING);
    }

    @Getter
    @Builder
    public static class PendingCardEvent {
        private Long id;
        private String uid;
        private String deviceCode;
        private LocalDateTime scannedAt;
    }
}
