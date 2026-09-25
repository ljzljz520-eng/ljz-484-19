package com.novel.service;

import com.novel.exception.ChapterDataCorruptedException;
import com.novel.exception.ResourceNotFoundException;
import com.novel.model.Chapter;
import com.novel.model.ChapterStatus;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChapterExportServiceTest {

    @Mock
    private DataRepository dataRepository;

    @InjectMocks
    private ChapterExportService service;

    private final Novel novel = new Novel(1L, "测试/作品:名", "描述", null, LocalDateTime.now());

    private Chapter chapter(long id, int order, String title, String content, ChapterStatus status) {
        return new Chapter(id, 1L, title, order, content, status, LocalDateTime.now());
    }

    @BeforeEach
    void setUp() {
        when(dataRepository.findNovelById(1L)).thenReturn(novel);
    }

    @Test
    void publishedMode_excludesDrafts_andMergesInOrder() {
        // 故意以乱序存储，验证导出按 orderNo 排序
        when(dataRepository.findChaptersByNovelId(1L)).thenReturn(List.of(
                chapter(2L, 2, "第二章", "正文二", ChapterStatus.PUBLISHED),
                chapter(3L, 3, "第三章（草稿）", "正文三", ChapterStatus.DRAFT),
                chapter(1L, 1, "第一章", "正文一", ChapterStatus.PUBLISHED)));

        ChapterExportService.ExportPayload payload =
                service.buildExport(1L, ChapterExportService.ExportMode.PUBLISHED);

        assertThat(payload.fileName()).startsWith("测试_作品_名_").endsWith("_已发布章节.txt");
        int idx1 = payload.content().indexOf("第一章");
        int idx2 = payload.content().indexOf("第二章");
        assertThat(idx1).isGreaterThanOrEqualTo(0);
        assertThat(idx2).isGreaterThan(idx1);
        assertThat(payload.content()).doesNotContain("正文三");
    }

    @Test
    void fullMode_includesDraftsMarked() {
        when(dataRepository.findChaptersByNovelId(1L)).thenReturn(List.of(
                chapter(1L, 1, "第一章", "正文一", ChapterStatus.PUBLISHED),
                chapter(2L, 2, "第二章", "正文二", ChapterStatus.DRAFT)));

        ChapterExportService.ExportPayload payload =
                service.buildExport(1L, ChapterExportService.ExportMode.FULL);

        assertThat(payload.fileName()).endsWith("_完整稿件.txt");
        assertThat(payload.content()).contains("正文二").contains("第二章【草稿】");
    }

    @Test
    void blankContent_isReportedAsCorruptedChapter() {
        when(dataRepository.findChaptersByNovelId(1L)).thenReturn(List.of(
                chapter(1L, 1, "第一章", "  ", ChapterStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.buildExport(1L, ChapterExportService.ExportMode.PUBLISHED))
                .isInstanceOf(ChapterDataCorruptedException.class)
                .hasMessageContaining("正文为空")
                .extracting(ex -> ((ChapterDataCorruptedException) ex).getChapterId())
                .isEqualTo(1L);
    }

    @Test
    void duplicateOrder_isReportedAsCorruptedChapter() {
        when(dataRepository.findChaptersByNovelId(1L)).thenReturn(List.of(
                chapter(1L, 1, "第一章", "正文一", ChapterStatus.PUBLISHED),
                chapter(2L, 1, "重号章节", "正文二", ChapterStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.buildExport(1L, ChapterExportService.ExportMode.FULL))
                .isInstanceOf(ChapterDataCorruptedException.class)
                .hasMessageContaining("序号 1 重复");
    }

    @Test
    void missingNovel_throwsNotFound() {
        when(dataRepository.findNovelById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.buildExport(99L, ChapterExportService.ExportMode.PUBLISHED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void writeToTempFile_persistsContent() throws Exception {
        when(dataRepository.findChaptersByNovelId(1L)).thenReturn(List.of(
                chapter(1L, 1, "第一章", "正文一", ChapterStatus.PUBLISHED)));
        ChapterExportService.ExportPayload payload =
                service.buildExport(1L, ChapterExportService.ExportMode.PUBLISHED);

        var path = service.writeToTempFile(payload);
        try {
            assertThat(Files.exists(path)).isTrue();
            assertThat(Files.readString(path, StandardCharsets.UTF_8)).contains("正文一");
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
