package com.novel.model;

/**
 * 章节发布状态。
 */
public enum ChapterStatus {
    /** 草稿：仅在导出完整稿件时包含。 */
    DRAFT,
    /** 已发布：导出已发布章节和完整稿件时均包含。 */
    PUBLISHED
}
