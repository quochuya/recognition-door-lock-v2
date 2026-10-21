package com.example.lock.card.service;

import com.example.lock.access.service.RoomPermissionService;
import com.example.lock.access.service.WhitelistService;
import com.example.lock.card.entity.PendingCardScan;
import com.example.lock.card.entity.RfidCard;
import com.example.lock.repository.PendingCardScanRepository;
import com.example.lock.repository.RfidCardRepository;
import com.example.lock.repository.UserRepository;
import com.example.lock.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardRegistrationService {

    private final RfidCardRepository rfidCardRepository;
    private final UserRepository userRepository;
    private final PendingCardScanRepository pendingCardScanRepository;
    private final WhitelistService whitelistService;
    private final RoomPermissionService roomPermissionService;

    /**
     * SỬA GỐC RỄ: đăng ký thẻ và cấp quyền phòng gộp làm 1 bước — roomIds
     * truyền vào được cấp quyền NGAY, cùng 1 giao dịch. Trước đây 2 API tách
     * rời rất dễ quên bước cấp quyền (nguyên nhân trực tiếp gây whitelist
     * offline rỗng dù thẻ đã đăng ký đầy đủ, từng xảy ra thật trong lúc test).
     */
    @Transactional
    public RfidCard registerManually(String uid, Long userId, LocalDate expiresAt, List<Long> roomIds) {
        if (rfidCardRepository.findByUid(uid).isPresent()) {
            throw new IllegalStateException("UID này đã được đăng ký cho thẻ khác");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy user id=" + userId + " — tạo user trước khi cấp thẻ"));

        RfidCard card = rfidCardRepository.save(RfidCard.builder()
                .uid(uid)
                .owner(user)
                .status(RfidCard.CardStatus.ACTIVE)
                .issuedAt(LocalDate.now())
                .expiresAt(expiresAt)
                .accessCount(0)
                .build());

        if (roomIds != null) {
            for (Long roomId : roomIds) {
                roomPermissionService.grant(userId, roomId, null);
            }
        }

        if (roomIds == null || roomIds.isEmpty()) {
            log.warn("Thẻ {} vừa đăng ký cho user {} nhưng KHÔNG kèm quyền phòng nào — "
                    + "thẻ sẽ không mở được cửa nào cho tới khi cấp quyền phòng riêng", uid, userId);
        }

        whitelistService.publishForUserRooms(userId);
        return card;
    }

    // Overload tương thích ngược cho lời gọi cũ không truyền roomIds
    @Transactional
    public RfidCard registerManually(String uid, Long userId, LocalDate expiresAt) {
        return registerManually(uid, userId, expiresAt, null);
    }

    @Transactional
    public RfidCard assignPendingScan(Long pendingId, Long userId, LocalDate expiresAt, List<Long> roomIds) {
        PendingCardScan pending = pendingCardScanRepository.findById(pendingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt quẹt chờ gán id=" + pendingId));

        if (pending.getStatus() != PendingCardScan.Status.WAITING) {
            throw new IllegalStateException("Lượt quẹt này đã được xử lý trước đó");
        }

        RfidCard card = registerManually(pending.getUid(), userId, expiresAt, roomIds);

        pending.setStatus(PendingCardScan.Status.ASSIGNED);
        pendingCardScanRepository.save(pending);

        return card;
    }

    @Transactional
    public void discardPendingScan(Long pendingId) {
        pendingCardScanRepository.findById(pendingId).ifPresent(p -> {
            p.setStatus(PendingCardScan.Status.DISCARDED);
            pendingCardScanRepository.save(p);
        });
    }

    @Transactional
    public RfidCard setStatus(Long cardId, RfidCard.CardStatus status) {
        RfidCard card = rfidCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ id=" + cardId));
        card.setStatus(status);
        RfidCard saved = rfidCardRepository.save(card);
        whitelistService.publishForUserRooms(card.getOwner().getId());
        return saved;
    }

    @Transactional
    public RfidCard revoke(Long cardId) {
        return setStatus(cardId, RfidCard.CardStatus.BLOCKED);
    }

    @Transactional
    public RfidCard update(Long cardId, Long newOwnerId, LocalDate expiresAt) {
        RfidCard card = rfidCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ id=" + cardId));

        Long oldOwnerId = card.getOwner().getId();
        if (newOwnerId != null && !newOwnerId.equals(oldOwnerId)) {
            User newOwner = userRepository.findById(newOwnerId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user id=" + newOwnerId));
            card.setOwner(newOwner);
        }
        card.setExpiresAt(expiresAt);
        RfidCard saved = rfidCardRepository.save(card);

        whitelistService.publishForUserRooms(oldOwnerId);
        if (newOwnerId != null && !newOwnerId.equals(oldOwnerId)) {
            whitelistService.publishForUserRooms(newOwnerId);
        }
        return saved;
    }

    @Transactional
    public void delete(Long cardId) {
        RfidCard card = rfidCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ id=" + cardId));
        Long ownerId = card.getOwner().getId();
        rfidCardRepository.deleteById(cardId);
        whitelistService.publishForUserRooms(ownerId);
    }
}
