package org.example.applicationinterviewservices.exception;

public class InvalidResumeException extends RuntimeException {
    public InvalidResumeException(String message) {
        super(message);
    }
}
