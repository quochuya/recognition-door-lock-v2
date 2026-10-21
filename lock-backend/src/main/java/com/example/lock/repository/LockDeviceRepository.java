package com.example.lock.repository;

import com.example.lock.room.entity.LockDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LockDeviceRepository extends JpaRepository<LockDevice, Long> {
    Optional<LockDevice> findByDeviceCode(String deviceCode);
    Optional<LockDevice> findByRoomId(Long roomId);

    long countByBatteryPercentGreaterThanEqual(int min);
    long countByBatteryPercentBetween(int min, int max);
    long countByBatteryPercentLessThan(int max);
    long countByConnectionStatus(LockDevice.ConnectionStatus status);

    /** row = [buildingName, offlineDeviceCount] */
    @Query("""
        select b.name, count(d)
        from LockDevice d join d.room r join r.floor f join f.building b
        where d.connectionStatus = com.example.lock.room.entity.LockDevice.ConnectionStatus.OFFLINE
        group by b.name
        """)
    List<Object[]> offlineCountByBuilding();

    /** Endpoint chính cho trang "Thiết bị" — lọc theo tòa + tìm theo mã thiết bị/số phòng. Lọc status/pin xử lý ở service. */
    @Query("""
        select d from LockDevice d join d.room r join r.floor f join f.building b
        where (:buildingId is null or b.id = :buildingId)
          and (:search is null
               or lower(d.deviceCode) like lower(concat('%', cast(:search as string), '%'))
               or str(r.roomNumber) like concat('%', cast(:search as string), '%'))
        order by r.roomNumber
        """)
    List<LockDevice> findFiltered(@Param("buildingId") Long buildingId, @Param("search") String search);
}
