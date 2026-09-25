package com.novel.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理：将不同失败原因映射为不同的错误类型与 HTTP 状态码，
 * 前端据此区分"文件写入问题"与"章节数据损坏"。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChapterDataCorruptedException.class)
    public ResponseEntity<Map<String, Object>> handleCorrupted(ChapterDataCorruptedException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "CHAPTER_DATA_CORRUPTED",
                "章节数据损坏，导出中止", ex.getMessage(), ex.getChapterId());
    }

    @ExceptionHandler(ExportFileWriteException.class)
    public ResponseEntity<Map<String, Object>> handleFileWrite(ExportFileWriteException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "EXPORT_FILE_WRITE_FAILED",
                "导出文件写入失败", ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "资源不存在", ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "请求参数错误", ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error,
                                                      String message, String detail, Long chapterId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("detail", detail);
        if (chapterId != null) {
            body.put("chapterId", chapterId);
        }
        return ResponseEntity.status(status).body(body);
    }
}
