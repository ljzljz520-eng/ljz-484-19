package com.novel.exception;

import org.springframework.http.HttpStatus;

/**
 * 稿件文件写入失败：合并内容已生成，但写入导出目录时发生 IO 错误
 * （如磁盘满、目录无写权限），属于“文件写入问题”。
 */
public class ExportFileWriteException extends ApiException {

    public ExportFileWriteException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_ERROR", message, cause);
    }
}
