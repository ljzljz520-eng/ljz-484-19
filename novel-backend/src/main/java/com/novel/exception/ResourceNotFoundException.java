package com.novel.exception;

/**
 * 请求的资源（如作品）不存在。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
