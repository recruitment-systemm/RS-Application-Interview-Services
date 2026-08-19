package org.example.applicationinterviewservices.dto.response;

public record ErrorResponse(boolean success, String message, ErrorDetails error) { }