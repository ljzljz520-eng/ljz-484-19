package com.novel.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常基类：携带 HTTP 状态码与机器可读的错误代码，
 * 由全局异常处理器统一转换为 JSON 错误响应。
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    protected ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
