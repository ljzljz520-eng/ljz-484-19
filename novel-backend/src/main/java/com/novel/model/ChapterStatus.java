package com.novel.model;

/**
 * 章节发布状态。
 * PUBLISHED: 已发布，对读者可见，可单独导出；
 * DRAFT: 草稿，仅在导出“完整稿件”时包含。
 */
public enum ChapterStatus {
    PUBLISHED,
    DRAFT
}
