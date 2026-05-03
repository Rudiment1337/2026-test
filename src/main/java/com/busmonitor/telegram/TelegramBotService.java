package com.busmonitor.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Service
public class TelegramBotService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    @Async
    public void sendAlert(String message) {
        if (message == null || message.trim().isEmpty()) {
            log.error("Cannot send empty message");
            return;
        }
        try {
            String[] cmd = {
                "bash", "-c",
                String.format("curl -s -X POST 'https://api.telegram.org/bot%s/sendMessage' -H 'Content-Type: application/json' -d '{\"chat_id\":\"%s\",\"text\":\"%s\",\"parse_mode\":\"HTML\"}'",
                    botToken, chatId, message)
            };
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Curl response: {}", line);
            }
            process.waitFor();
        } catch (Exception e) {
            log.error("Failed to send alert: {}", e.getMessage());
        }
    }

    public void sendAnomalyAlert(Long busId, String sensorType, Double value, String timestamp) {
        log.info("Anomaly alert: bus={}, sensor={}, value={}", busId, sensorType, value);
        String message = String.format(
                "Аномалия по индификатору: %d | Sensor: %s | Value: %.2f | Time: %s",
                busId, sensorType, value, timestamp
        );
        sendAlert(message);
    }
}
