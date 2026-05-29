package com.testround.seven_eleven.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s không tìm thấy với ID: %s", resourceName, identifier));
    }
}
