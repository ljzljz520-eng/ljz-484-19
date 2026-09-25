package com.novel.service;

import com.novel.exception.ChapterDataCorruptedException;
import com.novel.exception.ExportFileWriteException;
import com.novel.exception.NoExportableChaptersException;
import com.novel.exception.ResourceNotFoundException;
import com.novel.model.Chapter;
import com.novel.model.ChapterStatus;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    @TempDir
    Path tempDir;

    private DataRepository repository;
    private ExportService exportService;
    private Novel novel;

    @BeforeEach
    void setUp() {
        repository = new DataRepository(); // 空仓库，手工构造数据
        exportService = new ExportService(repository, tempDir.toString());
        novel = repository.saveNovel(new Novel(null, "测试作品", "简介", "cover", LocalDateTime.now()));
    }

    private Chapter chapter(String title, int orderNo, String content, ChapterStatus status) {
        return repository.saveChapter(new Chapter(null, novel.getId(), title, orderNo, content,
                LocalDateTime.now(), status));
    }

    @Test
    void exportPublishedOnly_mergesInOrderAndExcludesDrafts() throws IOException {
        // 故意乱序插入，验证按 orderNo 合并
        chapter("第二章：回归", 2, "第二章正文。", ChapterStatus.PUBLISHED);
        chapter("第一章：开端", 1, "第一章正文。", ChapterStatus.PUBLISHED);
        chapter("第三章：未公开", 3, "草稿正文。", ChapterStatus.DRAFT);

        ExportService.ExportResult result = exportService.exportNovel(novel.getId(), false);

        String expectedName = "测试作品_" + LocalDate.now().format(ExportService.FILE_DATE_FORMAT) + ".txt";
        assertEquals(expectedName, result.filename(), "文件名应包含作品名与日期");
        assertEquals(2, result.chapterCount());
        assertTrue(Files.exists(result.file()));

        String text = Files.readString(result.file());
        assertTrue(text.indexOf("第一章：开端") < text.indexOf("第二章：回归"), "应按章节顺序合并");
        assertTrue(text.contains("第一章正文。"));
        assertFalse(text.contains("第三章：未公开"), "仅导出已发布章节时不应包含草稿");
        assertFalse(text.contains("草稿正文。"));
    }

    @Test
    void exportFull_includesDraftsWithMarker() throws IOException {
        chapter("第一章：开端", 1, "第一章正文。", ChapterStatus.PUBLISHED);
        chapter("第二章：构思中", 2, "草稿正文。", ChapterStatus.DRAFT);

        ExportService.ExportResult result = exportService.exportNovel(novel.getId(), true);

        assertEquals(2, result.chapterCount());
        String text = Files.readString(result.file());
        assertTrue(text.contains("第二章：构思中【草稿】"), "完整稿件中草稿章节应有【草稿】标记");
        assertTrue(text.contains("完整稿件（含草稿）"));
    }

    @Test
    void export_withNullContent_throwsChapterDataCorrupted() {
        chapter("第一章：开端", 1, "正常正文。", ChapterStatus.PUBLISHED);
        chapter("第二章：残缺", 2, null, ChapterStatus.PUBLISHED);

        ChapterDataCorruptedException ex = assertThrows(ChapterDataCorruptedException.class,
                () -> exportService.exportNovel(novel.getId(), false));
        assertTrue(ex.getMessage().contains("章节数据损坏"));
        assertTrue(ex.getMessage().contains("第二章：残缺"));
        assertEquals("CHAPTER_DATA_CORRUPTED", ex.getCode());
    }

    @Test
    void export_withBlankTitle_throwsChapterDataCorrupted() {
        chapter("  ", 1, "正文存在但标题为空。", ChapterStatus.PUBLISHED);

        ChapterDataCorruptedException ex = assertThrows(ChapterDataCorruptedException.class,
                () -> exportService.exportNovel(novel.getId(), false));
        assertTrue(ex.getMessage().contains("章节数据损坏"));
        assertTrue(ex.getMessage().contains("缺少标题"));
    }

    @Test
    void export_draftCorruptionOnlyAffectsFullExport() throws IOException {
        chapter("第一章：开端", 1, "正常正文。", ChapterStatus.PUBLISHED);
        chapter("第二章：残缺草稿", 2, null, ChapterStatus.DRAFT);

        // 仅导出已发布章节时不受草稿数据影响
        ExportService.ExportResult result = exportService.exportNovel(novel.getId(), false);
        assertEquals(1, result.chapterCount());

        // 导出完整稿件时则报告数据损坏
        assertThrows(ChapterDataCorruptedException.class,
                () -> exportService.exportNovel(novel.getId(), true));
    }

    @Test
    void export_toUnwritableLocation_throwsExportFileWrite() throws IOException {
        chapter("第一章：开端", 1, "正文。", ChapterStatus.PUBLISHED);

        // 用一个“已存在的普通文件”作为导出目录的父级，使创建目录必然失败
        Path blocker = Files.createFile(tempDir.resolve("blocker"));
        ExportService brokenService = new ExportService(repository, blocker.resolve("sub").toString());

        ExportFileWriteException ex = assertThrows(ExportFileWriteException.class,
                () -> brokenService.exportNovel(novel.getId(), false));
        assertTrue(ex.getMessage().contains("文件写入失败"));
        assertEquals("FILE_WRITE_ERROR", ex.getCode());
    }

    @Test
    void export_unknownNovel_throwsNotFound() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> exportService.exportNovel(999999L, false));
        assertEquals("NOT_FOUND", ex.getCode());
    }

    @Test
    void export_novelWithoutPublishedChapters_throwsNoExportable() {
        chapter("第一章：草稿", 1, "草稿正文。", ChapterStatus.DRAFT);

        NoExportableChaptersException ex = assertThrows(NoExportableChaptersException.class,
                () -> exportService.exportNovel(novel.getId(), false));
        assertTrue(ex.getMessage().contains("暂无已发布章节"));
        assertEquals("NO_EXPORTABLE_CHAPTERS", ex.getCode());
    }

    @Test
    void filename_illegalCharsAreSanitized() {
        Novel weird = repository.saveNovel(
                new Novel(null, "星际/穿越:测试版*?", "简介", "cover", LocalDateTime.now()));
        repository.saveChapter(new Chapter(null, weird.getId(), "第一章", 1, "正文。",
                LocalDateTime.now(), ChapterStatus.PUBLISHED));

        ExportService.ExportResult result = exportService.exportNovel(weird.getId(), false);

        assertFalse(result.filename().contains("/"));
        assertFalse(result.filename().contains(":"));
        assertFalse(result.filename().contains("*"));
        assertTrue(result.filename().startsWith("星际_穿越_测试版"));
        assertTrue(result.filename().endsWith(".txt"));
    }
}
