package com.novel.exception;

import org.springframework.http.HttpStatus;

/**
 * 章节数据损坏：导出前校验发现章节缺少标题、正文或序号等关键字段，
 * 属于“数据问题”，与文件写入问题明确区分。
 */
public class ChapterDataCorruptedException extends ApiException {

    public ChapterDataCorruptedException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "CHAPTER_DATA_CORRUPTED", message);
    }
}
