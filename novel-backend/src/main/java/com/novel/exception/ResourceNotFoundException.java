package com.novel.exception;

import org.springframework.http.HttpStatus;

/** 请求的资源（作品 / 章节）不存在。 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
