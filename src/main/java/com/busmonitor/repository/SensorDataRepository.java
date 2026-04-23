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
	@Query(value = "SELECT DISTINCT ON (sensor_type) * FROM sens_data WHERE bus_id = :busId ORDER BY sensor_type, timestamp DESC", nativeQuery = true)
    List<SensorData> findLatestReadingsByBus(@Param("busId") Long busId);

    @Query("SELECT s FROM SensorData s WHERE s.anomaly = true ORDER BY s.timestamp DESC")
    List<SensorData> findAllAnomalies();
}
