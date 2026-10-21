package com.example.lock.repository;

import com.example.lock.card.entity.PendingCardScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingCardScanRepository extends JpaRepository<PendingCardScan, Long> {
    List<PendingCardScan> findByStatusOrderByScannedAtDesc(PendingCardScan.Status status);
    Optional<PendingCardScan> findFirstByUidAndStatus(String uid, PendingCardScan.Status status);
}
