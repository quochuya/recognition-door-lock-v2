package com.example.lock.repository;

import com.example.lock.card.entity.RfidCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RfidCardRepository extends JpaRepository<RfidCard, Long> {
    Optional<RfidCard> findByUid(String uid);

    /**
     * UID nào được phép vào 1 phòng cụ thể — dùng để đẩy "whitelist offline"
     * xuống ESP32 (RFID) gắn ở phòng đó. CHỈ áp dụng cho RFID — QR không bao
     * giờ nằm trong whitelist này vì QrToken là entity hoàn toàn khác, không
     * liên quan tới bảng rfid_cards.
     */
    @Query("""
        select c.uid from RfidCard c, RoomPermission p
        where p.room.id = :roomId
          and p.user.id = c.owner.id
          and c.status = com.example.lock.card.entity.RfidCard.CardStatus.ACTIVE
          and (c.expiresAt is null or c.expiresAt >= current_date)
          and (p.expiresAt is null or p.expiresAt > current_timestamp)
        """)
    List<String> findValidUidsForRoom(@Param("roomId") Long roomId);
}
