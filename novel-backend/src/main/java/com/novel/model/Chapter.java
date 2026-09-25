package com.novel.model;

import java.time.LocalDateTime;

public class Chapter {
    private Long id;
    private Long novelId;
    private String title;
    private Integer orderNo;
    private String content;
    private LocalDateTime createdAt;
    /** 发布状态：PUBLISHED 已发布 / DRAFT 草稿。默认已发布，兼容旧数据。 */
    private ChapterStatus status = ChapterStatus.PUBLISHED;

    public Chapter() {
    }

    public Chapter(Long id, Long novelId, String title, Integer orderNo, String content, LocalDateTime createdAt) {
        this(id, novelId, title, orderNo, content, createdAt, ChapterStatus.PUBLISHED);
    }

    public Chapter(Long id, Long novelId, String title, Integer orderNo, String content, LocalDateTime createdAt,
            ChapterStatus status) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.orderNo = orderNo;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status == null ? ChapterStatus.PUBLISHED : status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ChapterStatus getStatus() {
        return status;
    }

    public void setStatus(ChapterStatus status) {
        this.status = status == null ? ChapterStatus.PUBLISHED : status;
    }
}
