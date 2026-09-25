package com.novel.controller;

import com.novel.exception.ResourceNotFoundException;
import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import com.novel.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to access
@Tag(name = "Novel System API")
public class NovelController {

    private final DataRepository dataRepository;
    private final ExportService exportService;

    public NovelController(DataRepository dataRepository, ExportService exportService) {
        this.dataRepository = dataRepository;
        this.exportService = exportService;
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
    @Operation(summary = "Get Novel Details (with published chapters)")
    public Map<String, Object> getNovelDetail(@PathVariable Long id) {
        Novel novel = dataRepository.findNovelById(id);
        if (novel == null) {
            throw new ResourceNotFoundException("Novel not found, id: " + id);
        }
        // 读者侧仅展示已发布章节，草稿不下发
        List<Chapter> chapters = dataRepository.findPublishedChaptersByNovelId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("novel", novel);
        response.put("chapters", chapters); // Include chapters as requested ("merge directory into detail")
        return response;
    }

    @GetMapping("/novels/{id}/chapters")
    @Operation(summary = "Get Chapters for a Novel")
    public List<Chapter> getChapters(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeDrafts) {
        if (dataRepository.findNovelById(id) == null) {
            throw new ResourceNotFoundException("Novel not found, id: " + id);
        }
        return includeDrafts
                ? dataRepository.findChaptersByNovelId(id)
                : dataRepository.findPublishedChaptersByNovelId(id);
    }

    @GetMapping("/novels/{id}/export")
    @CrossOrigin(origins = "*", exposedHeaders = "Content-Disposition")
    @Operation(summary = "Export novel manuscript as .txt (published only or including drafts)")
    public ResponseEntity<Resource> exportNovel(
            @PathVariable Long id,
            @Parameter(description = "true 导出含草稿的完整稿件；false 仅导出已发布章节")
            @RequestParam(defaultValue = "false") boolean includeDrafts) {
        ExportService.ExportResult result = exportService.exportNovel(id, includeDrafts);

        java.io.File file = result.file().toFile();
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .contentLength(file.length())
                .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(result.filename()))
                .header("X-Exported-Chapters", String.valueOf(result.chapterCount()))
                .body(resource);
    }

    /**
     * 构造兼容中文文件名的 Content-Disposition：
     * 同时提供 ASCII 回退文件名与 RFC 5987 (filename*) 编码文件名。
     */
    private String buildContentDisposition(String filename) {
        String asciiFallback = filename.replaceAll("[^\\x20-\\x7e]", "_");
        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Get Chapter Content")
    public Chapter getChapter(@PathVariable Long id) {
        Chapter chapter = dataRepository.findChapterById(id);
        if (chapter == null) {
            throw new ResourceNotFoundException("Chapter not found, id: " + id);
        }
        return chapter;
    }
}
