package com.busmonitor.service;

import com.busmonitor.model.SensorData;
import com.busmonitor.repository.SensorDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExportService {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    public ByteArrayInputStream exportSensorDataToExcel(Long busId, LocalDateTime from, LocalDateTime to) {
        List<SensorData> data = sensorDataRepository.findByBusIdAndTimestampBetweenOrderByTimestampDesc(busId, from, to);
        log.info("Export {} for bus {}", data.size(), busId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sensor Data");

            // Стиль
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Заголовки
            String[] columns = {"ID", "Bus ID", "Sensor Type", "Value", "Timestamp", "Anomaly"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            int rowNum = 1;
            for (SensorData sd : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sd.getId());
                row.createCell(1).setCellValue(sd.getBus().getId());
                row.createCell(2).setCellValue(sd.getSensorType().getType());
                row.createCell(3).setCellValue(sd.getValue());
                row.createCell(4).setCellValue(sd.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                row.createCell(5).setCellValue(sd.isAnomaly() ? "YES" : "NO");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to export to Excel: {}", e.getMessage());
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }
}
