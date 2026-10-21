package com.example.lock.controller;

import com.example.lock.card.entity.PendingCardScan;
import com.example.lock.card.entity.RfidCard;
import com.example.lock.card.service.CardEnrollmentService;
import com.example.lock.card.service.CardRegistrationService;
import com.example.lock.controller.dto.AssignPendingCardRequest;
import com.example.lock.controller.dto.CardDto;
import com.example.lock.controller.dto.RegisterCardRequest;
import com.example.lock.controller.dto.UpdateCardRequest;
import com.example.lock.repository.RfidCardRepository;
import com.example.lock.repository.RoomPermissionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Xem (GET): ADMIN + STAFF. Mọi thao tác thay đổi trạng thái thẻ (cấp/thu hồi/
 * enrollment): ADMIN only — thẻ RFID là quyền ra vào vật lý, không giao cho STAFF.
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardRegistrationService cardRegistrationService;
    private final CardEnrollmentService cardEnrollmentService;
    private final RfidCardRepository rfidCardRepository;
    private final RoomPermissionRepository roomPermissionRepository;

    // --- Danh sách / chi tiết ---
    // @Transactional bắt buộc: CardDto.from() đọc card.getOwner() (LAZY).

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional(readOnly = true)
    public List<CardDto> list() {
        return rfidCardRepository.findAll().stream()
                .map(card -> CardDto.from(card, roomPermissionRepository.findByUserId(card.getOwner().getId())))
                .toList();
    }

    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    @Transactional(readOnly = true)
    public CardDto detail(@PathVariable Long cardId) {
        RfidCard card = rfidCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ id=" + cardId));
        return CardDto.from(card, roomPermissionRepository.findByUserId(card.getOwner().getId()));
    }

    // --- Đăng ký thủ công ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RfidCard register(@Valid @RequestBody RegisterCardRequest req) {
        return cardRegistrationService.registerManually(req.getUid(), req.getUserId(), req.getExpiresAt(), req.getRoomIds());
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RfidCard update(@PathVariable Long cardId, @RequestBody UpdateCardRequest req) {
        return cardRegistrationService.update(cardId, req.getOwnerId(), req.getExpiresAt());
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long cardId) {
        cardRegistrationService.delete(cardId);
    }

    // --- Thu hồi / kích hoạt lại ---

    @PostMapping("/{cardId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public RfidCard revoke(@PathVariable Long cardId) {
        return cardRegistrationService.revoke(cardId);
    }

    @PostMapping("/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public RfidCard activate(@PathVariable Long cardId) {
        return cardRegistrationService.setStatus(cardId, RfidCard.CardStatus.ACTIVE);
    }

    // --- Chế độ enrollment (one-touch: quẹt thẻ lạ để lấy UID) ---

    @PostMapping("/enrollment/start")
    @PreAuthorize("hasRole('ADMIN')")
    public void startEnrollment() {
        cardEnrollmentService.startEnrollment();
    }

    @PostMapping("/enrollment/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public void stopEnrollment() {
        cardEnrollmentService.stopEnrollment();
    }

    @GetMapping("/enrollment/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public boolean enrollmentStatus() {
        return cardEnrollmentService.isEnrollmentActive();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public List<PendingCardScan> listPending() {
        return cardEnrollmentService.listWaiting();
    }

    @PostMapping("/pending/{pendingId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public RfidCard assignPending(@PathVariable Long pendingId, @Valid @RequestBody AssignPendingCardRequest req) {
        return cardRegistrationService.assignPendingScan(pendingId, req.getUserId(), req.getExpiresAt(), req.getRoomIds());
    }

    @PostMapping("/pending/{pendingId}/discard")
    @PreAuthorize("hasRole('ADMIN')")
    public void discardPending(@PathVariable Long pendingId) {
        cardRegistrationService.discardPendingScan(pendingId);
    }
}
