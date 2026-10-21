package com.example.lock.repository;

import com.example.lock.access.entity.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    long countByLockDeviceIdAndSuccessFalseAndOccurredAtAfter(Long lockDeviceId, LocalDateTime since);
    Page<AccessLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
    List<AccessLog> findTop5ByLockDeviceIdOrderByOccurredAtDesc(Long lockDeviceId);

    /**
     * FIX #3 (chống ghi trùng khi ESP32 gửi lại cùng batch offline-events vì
     * chưa nhận ACK): kiểm tra đã có log offline nào cho đúng thiết bị + user
     * trong khoảng thời gian gần đúng chưa (cửa sổ ±vài giây, không so khớp
     * tuyệt đối vì trường hợp timestamp là giờ server dự phòng — xem
     * DeviceMessageListener — có thể lệch nhẹ giữa các lần gửi lại).
     * So sánh user null-safe: dùng IS NULL thay vì = NULL (JPQL/SQL: so sánh
     * trực tiếp với null luôn ra false, không bắt được trường hợp thẻ lạ).
     */
    @Query("""
        select case when count(a) > 0 then true else false end
        from AccessLog a
        where a.lockDevice = :device
          and a.offline = true
          and a.occurredAt between :from and :to
          and ((:user is null and a.user is null) or a.user = :user)
        """)
    boolean existsSimilarOfflineLog(@Param("device") com.example.lock.room.entity.LockDevice device,
                                     @Param("user") com.example.lock.user.entity.User user,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Query("""
        select count(a) from AccessLog a
        where a.success = true and a.occurredAt between :from and :to
          and (:buildingId is null or a.lockDevice.room.floor.building.id = :buildingId)
        """)
    long countSuccess(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("buildingId") Long buildingId);

    @Query("""
        select count(a) from AccessLog a
        where a.success = false and a.occurredAt between :from and :to
          and (:buildingId is null or a.lockDevice.room.floor.building.id = :buildingId)
        """)
    long countDenied(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("buildingId") Long buildingId);

    /** row = [date, successCount, deniedCount] */
    @Query("""
        select function('date', a.occurredAt) as day,
               sum(case when a.success = true then 1 else 0 end),
               sum(case when a.success = false then 1 else 0 end)
        from AccessLog a
        where a.occurredAt between :from and :to
          and (:buildingId is null or a.lockDevice.room.floor.building.id = :buildingId)
        group by function('date', a.occurredAt)
        order by day
        """)
    List<Object[]> dailyActivity(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("buildingId") Long buildingId);

    /** row = [roomLabel, count] */
    @Query("""
        select concat('Phòng ', r.roomNumber), count(a)
        from AccessLog a join a.lockDevice d join d.room r
        where a.occurredAt between :from and :to
          and (:buildingId is null or r.floor.building.id = :buildingId)
        group by r.roomNumber
        order by count(a) desc
        """)
    List<Object[]> topRooms(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("buildingId") Long buildingId, Pageable pageable);

    /** row = [buildingName, activityCount] */
    @Query("""
        select b.name, count(a)
        from AccessLog a join a.lockDevice d join d.room r join r.floor f join f.building b
        where a.occurredAt between :from and :to
        group by b.name
        """)
    List<Object[]> buildingActivity(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** row = [denyReason (ruleKey), count] */
    @Query("""
        select a.denyReason, count(a)
        from AccessLog a
        where a.success = false and a.denyReason is not null and a.occurredAt between :from and :to
          and (:buildingId is null or a.lockDevice.room.floor.building.id = :buildingId)
        group by a.denyReason
        order by count(a) desc
        """)
    List<Object[]> denyReasonBreakdown(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("buildingId") Long buildingId);

    /** Endpoint chính cho trang "Lịch sử ra vào" — lọc theo tòa/phương thức/trạng thái/khoảng ngày/từ khoá */
    @Query("""
        select a from AccessLog a
        join a.lockDevice d join d.room r join r.floor f join f.building b
        left join a.user u
        where a.occurredAt between :from and :to
          and (:buildingId is null or b.id = :buildingId)
          and (:method is null or a.method = :method)
          and (:success is null or a.success = :success)
          and (:search is null
               or lower(u.fullName) like lower(concat('%', cast(:search as string), '%'))
               or str(r.roomNumber) like concat('%', cast(:search as string), '%'))
        order by a.occurredAt desc
        """)
    Page<AccessLog> findFiltered(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("buildingId") Long buildingId,
                                  @Param("method") com.example.lock.access.entity.AccessMethod method,
                                  @Param("success") Boolean success,
                                  @Param("search") String search,
                                  Pageable pageable);
}
