package com.novel.controller;

import com.novel.model.Chapter;
import com.novel.model.ChapterStatus;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "novel.export.dir=target/test-exports")
class NovelControllerExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataRepository repository;

    private Long createdNovelId;
    private Long createdChapterId;

    @AfterEach
    void cleanup() {
        if (createdChapterId != null) {
            repository.deleteChapter(createdChapterId);
        }
        if (createdNovelId != null) {
            repository.deleteNovel(createdNovelId);
        }
    }

    @Test
    void exportPublished_returnsTxtWithEncodedFilename() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/novels/1/export"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8"))
                .andExpect(header().string("X-Exported-Chapters", "3"))
                .andReturn();

        String disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.startsWith("attachment;"));
        assertTrue(disposition.contains("filename*=UTF-8''"), "应提供 RFC 5987 编码文件名");
        // “星际穿越之编程大师” 的 URL 编码前缀
        assertTrue(disposition.contains("%E6%98%9F%E9%99%85%E7%A9%BF%E8%B6%8A"),
                "文件名应包含作品名（URL 编码）: " + disposition);

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("《星际穿越之编程大师》"));
        assertTrue(body.contains("第一章：Hello World"));
        assertTrue(body.contains("第三章：循环陷阱"));
        assertFalse(body.contains("第四章：异常捕获"), "默认导出不应包含草稿章节");
        assertTrue(body.indexOf("第一章") < body.indexOf("第二章")
                && body.indexOf("第二章") < body.indexOf("第三章"), "章节应按顺序合并");
    }

    @Test
    void exportFull_includesDraftChapters() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/novels/1/export").param("includeDrafts", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Exported-Chapters", "4"))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("第四章：异常捕获（草稿）【草稿】"), "完整稿件应包含带标记的草稿章节");
    }

    @Test
    void export_unknownNovel_returns404() throws Exception {
        mockMvc.perform(get("/api/novels/424242/export"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void export_novelWithoutChapters_returnsNoExportableChapters() throws Exception {
        // 种子数据中的 3 号作品没有任何章节
        mockMvc.perform(get("/api/novels/3/export"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_EXPORTABLE_CHAPTERS"));
    }

    @Test
    void export_corruptedChapter_returnsDataCorruptedError() throws Exception {
        Novel novel = repository.saveNovel(
                new Novel(null, "损坏数据测试作品", "简介", "cover", LocalDateTime.now()));
        createdNovelId = novel.getId();
        Chapter ok = repository.saveChapter(new Chapter(null, novel.getId(), "第一章", 1, "正常正文。",
                LocalDateTime.now(), ChapterStatus.PUBLISHED));
        Chapter broken = repository.saveChapter(new Chapter(null, novel.getId(), "第二章：残缺", 2, null,
                LocalDateTime.now(), ChapterStatus.PUBLISHED));
        createdChapterId = broken.getId();

        try {
            mockMvc.perform(get("/api/novels/" + novel.getId() + "/export"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("CHAPTER_DATA_CORRUPTED"))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("章节数据损坏")));
        } finally {
            repository.deleteChapter(ok.getId());
        }
    }
}
