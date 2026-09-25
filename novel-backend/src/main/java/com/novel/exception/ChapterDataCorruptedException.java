package com.novel.exception;

/**
 * 章节数据损坏：章节缺失必要字段（标题/正文/序号/状态）或状态非法，无法导出。
 */
public class ChapterDataCorruptedException extends RuntimeException {

    private final Long chapterId;

    public ChapterDataCorruptedException(Long chapterId, String message) {
        super(message);
        this.chapterId = chapterId;
    }

    public ChapterDataCorruptedException(String message) {
        this(null, message);
    }

    public Long getChapterId() {
        return chapterId;
    }
}
