package com.novel.exception;

import java.io.IOException;

/**
 * 导出文件写入失败：服务器在生成/写出导出文件时发生 IO 错误。
 */
public class ExportFileWriteException extends RuntimeException {

    public ExportFileWriteException(String message, IOException cause) {
        super(message, cause);
    }
}
