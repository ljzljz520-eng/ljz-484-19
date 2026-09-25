package com.novel.exception;

import org.springframework.http.HttpStatus;

/** 所选导出范围内没有任何章节（例如作品尚无已发布章节）。 */
public class NoExportableChaptersException extends ApiException {

    public NoExportableChaptersException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "NO_EXPORTABLE_CHAPTERS", message);
    }
}
