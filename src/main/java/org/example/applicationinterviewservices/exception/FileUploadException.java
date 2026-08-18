package org.example.applicationinterviewservices.exception;

public class FileUploadException extends RuntimeException {

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileUploadException(String message) {
        super(message);
    }
}
