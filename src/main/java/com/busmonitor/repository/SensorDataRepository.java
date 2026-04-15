package com.busmonitor.repository;

import com.busmonitor.model.SensorData;
import com.busmonitor.model.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    List<SensorData> findByBusIdAndTimestampBetweenOrderByTimestampDesc(
        Long busId, LocalDateTime from, LocalDateTime to);
    @Query("SELECT s FROM SensorData s WHERE s.bus.id = :busId AND s.timestamp = " +
           "(SELECT MAX(s2.timestamp) FROM SensorData s2 WHERE s2.bus.id = :busId AND s2.sensorType = s.sensorType)")
    List<SensorData> findLatestReadingsByBus(@Param("busId") Long busId);

    @Query("SELECT s FROM SensorData s WHERE s.anomaly = true ORDER BY s.timestamp DESC")
    List<SensorData> findAllAnomalies();
}
