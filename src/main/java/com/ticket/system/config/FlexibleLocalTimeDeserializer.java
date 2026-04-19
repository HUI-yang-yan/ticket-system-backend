package com.ticket.system.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 支持多种格式的 LocalTime 反序列化器：
 * 1. 字符串格式: "08:00", "8:00", "08:00:00"
 * 2. 对象格式: {"hour": 8, "minute": 0}
 */
public class FlexibleLocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();

        if (token == JsonToken.VALUE_STRING) {
            String value = p.getValueAsString();
            if (value == null || value.isBlank()) {
                return null;
            }
            // 尝试多种时间格式
            return parseTimeString(value);
        }

        if (token == JsonToken.START_OBJECT) {
            int hour = 0;
            int minute = 0;
            int second = 0;

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();
                switch (fieldName) {
                    case "hour":
                        hour = p.getIntValue();
                        break;
                    case "minute":
                        minute = p.getIntValue();
                        break;
                    case "second":
                        second = p.getIntValue();
                        break;
                }
            }
            return LocalTime.of(hour, minute, second);
        }

        if (token == JsonToken.VALUE_NUMBER_INT) {
            // 毫秒时间戳
            long timestamp = p.getLongValue();
            return LocalTime.ofNanoOfDay(timestamp * 1_000_000);
        }

        return null;
    }

    private LocalTime parseTimeString(String value) {
        value = value.trim();

        // 尝试 HH:mm 格式
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {}

        // 尝试 HH:mm:ss 格式
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (Exception ignored) {}

        // 尝试 HH:mm:ss.SSS 格式
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        } catch (Exception ignored) {}

        // 尝试只有 hour:minute
        try {
            String[] parts = value.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return LocalTime.of(hour, minute, second);
        } catch (Exception ignored) {}

        return LocalTime.parse(value);
    }
}
