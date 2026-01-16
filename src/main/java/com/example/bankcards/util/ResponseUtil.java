package com.example.bankcards.util;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class ResponseUtil {

    public ResponseEntity<Map<String, Object>> successResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.OK.value());
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> createdResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.CREATED.value());
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("success", false);
        response.put("message", message);
        response.put("error", error);

        return ResponseEntity.status(status).body(response);
    }

    public ResponseEntity<Map<String, Object>> validationErrorResponse(String message, Map<String, String> errors) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("success", false);
        response.put("message", message);
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    public Map<String, Object> paginatedResponse(Object content, int page, int size, long totalElements, int totalPages) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("last", page >= totalPages - 1);

        return response;
    }
}
