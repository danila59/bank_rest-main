package com.example.bankcards.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@JsonComponent
public class YearMonthDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String date = p.getText();
        try {
            YearMonth yearMonth = YearMonth.parse(date,
                    DateTimeFormatter.ofPattern("MM/yy"));
            return yearMonth.atDay(1);
        } catch (Exception e) {
            throw new IOException("Invalid date format. Use MM/yy", e);
        }
    }
}