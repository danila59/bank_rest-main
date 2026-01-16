package com.example.bankcards.exception;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CardOperationException extends RuntimeException {

    private final HttpStatus status;
    private final String details;

    public CardOperationException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.details = message;
    }

    public CardOperationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.details = message;
    }
}