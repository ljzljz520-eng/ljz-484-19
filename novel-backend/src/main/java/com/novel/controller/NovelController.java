package com.novel.controller;

import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import com.novel.service.ChapterExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Novel System API")
public class NovelController {

    private final DataRepository dataRepository;
    private final ChapterExportService chapterExportService;

    public NovelController(DataRepository dataRepository, ChapterExportService chapterExportService) {
        this.dataRepository = dataRepository;
        this.chapterExportService = chapterExportService;
    }

    @GetMapping("/novels")
    @Operation(summary = "Get Novel List")
    public Map<String, Object> getNovels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        List<Novel> list = dataRepository.findAllNovels(keyword, page, size);
        long total = dataRepository.countNovels(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("data", list);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    @GetMapping("/novels/{id}")
    @Operation(summary = "Get Novel Details (with chapters)")
    public Map<String, Object> getNovelDetail(@PathVariable Long id) {
        Novel novel = dataRepository.findNovelById(id);
        if (novel == null) {
            throw new RuntimeException("Novel not found");
        }
        List<Chapter> chapters = dataRepository.findChaptersByNovelId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("novel", novel);
        response.put("chapters", chapters); // Include chapters as requested ("merge directory into detail")
        return response;
    }

    @GetMapping("/novels/{id}/chapters")
    @Operation(summary = "Get Chapters for a Novel")
    public List<Chapter> getChapters(@PathVariable Long id) {
        return dataRepository.findChaptersByNovelId(id);
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Get Chapter Content")
    public Chapter getChapter(@PathVariable Long id) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null) {
            throw new RuntimeException("Chapter not found");
        }
        return chapter;
    }

    @GetMapping("/novels/{id}/export")
    @Operation(summary = "Export merged chapter text as a .txt file")
    public ResponseEntity<FileSystemResource> exportNovel(
            @PathVariable Long id,
            @RequestParam(name = "mode", defaultValue = "published") String mode) {

        ChapterExportService.ExportMode exportMode = parseMode(mode);

        // 1. 装配稿件：作品不存在 / 章节数据损坏会在此处抛出明确异常
        ChapterExportService.ExportPayload payload = chapterExportService.buildExport(id, exportMode);

        // 2. 写入临时文件：磁盘 IO 失败会被包装为"文件写入问题"
        Path tempFile = chapterExportService.writeToTempFile(payload);

        try {
            String encodedFileName = URLEncoder.encode(payload.fileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                    .contentLength(Files.size(tempFile))
                    .body(new FileSystemResource(tempFile));
        } catch (java.io.IOException e) {
            throw new com.novel.exception.ExportFileWriteException(
                    "读取导出文件失败：" + e.getMessage(), e);
        }
        // 注：临时文件会在响应提交后由容器/系统临时目录机制回收
    }

    private ChapterExportService.ExportMode parseMode(String mode) {
        if (mode == null || mode.isBlank() || "published".equalsIgnoreCase(mode)) {
            return ChapterExportService.ExportMode.PUBLISHED;
        }
        if ("full".equalsIgnoreCase(mode)) {
            return ChapterExportService.ExportMode.FULL;
        }
        throw new IllegalArgumentException("非法的导出模式: " + mode + "（仅支持 published / full）");
    }
}
