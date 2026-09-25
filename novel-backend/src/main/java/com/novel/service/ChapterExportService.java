package com.novel.service;

import com.novel.exception.ChapterDataCorruptedException;
import com.novel.exception.ExportFileWriteException;
import com.novel.model.Chapter;
import com.novel.model.ChapterStatus;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 章节稿件导出服务：
 * 1. 按章节顺序合并章节文本；
 * 2. 支持仅导出已发布章节或包含草稿的完整稿件；
 * 3. 文件名带作品名与导出日期。
 */
@Service
public class ChapterExportService {

    /** 导出范围：已发布章节 / 完整稿件（含草稿）。 */
    public enum ExportMode {
        PUBLISHED, FULL
    }

    /** 一次导出的结果：合并后的稿件文本 + 下载文件名。 */
    public record ExportPayload(String fileName, String content) {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final DataRepository dataRepository;

    public ChapterExportService(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    /**
     * 构建导出稿件（内存中拼装，不涉及磁盘写入）。
     *
     * @param novelId 作品 ID
     * @param mode    导出范围
     * @return 文件名与稿件内容
     * @throws com.novel.exception.ResourceNotFoundException 作品不存在
     * @throws ChapterDataCorruptedException                 章节数据损坏
     */
    public ExportPayload buildExport(Long novelId, ExportMode mode) {
        Novel novel = dataRepository.findNovelById(novelId);
        if (novel == null) {
            throw new com.novel.exception.ResourceNotFoundException("Novel not found: " + novelId);
        }

        List<Chapter> all = dataRepository.findChaptersByNovelId(novelId);
        List<Chapter> selected = all.stream()
                .filter(c -> mode == ExportMode.FULL || c.getStatus() == ChapterStatus.PUBLISHED)
                .collect(Collectors.toList());

        String fileName = sanitizeFileName(novel.getTitle())
                + "_" + LocalDate.now().format(DATE_FMT)
                + (mode == ExportMode.FULL ? "_完整稿件" : "_已发布章节")
                + ".txt";

        String content = assembleManuscript(novel, selected, mode);
        return new ExportPayload(fileName, content);
    }

    /**
     * 将导出内容写入临时文件。磁盘 IO 失败时包装为 {@link ExportFileWriteException}，
     * 以便调用方向用户明确说明是"文件写入问题"。
     */
    public Path writeToTempFile(ExportPayload payload) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile("novel-export-", ".txt");
        } catch (IOException e) {
            throw new ExportFileWriteException("创建导出临时文件失败：" + e.getMessage(), e);
        }
        try {
            Files.writeString(tempFile, payload.content(), StandardCharsets.UTF_8);
            return tempFile;
        } catch (IOException | UncheckedIOException e) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // 清理失败不影响原始异常的抛出
            }
            throw new ExportFileWriteException("写入导出文件失败：" + e.getMessage(),
                    e instanceof IOException ioe ? ioe : new IOException(e.getMessage(), e));
        }
    }

    /**
     * 按章节顺序合并为一份稿件。合并前逐章校验，发现损坏数据立即抛出
     * {@link ChapterDataCorruptedException}，避免导出残缺稿件。
     */
    private String assembleManuscript(Novel novel, List<Chapter> chapters, ExportMode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append(novel.getTitle()).append(LINE_SEPARATOR);
        sb.append("=".repeat(20)).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        sb.append(mode == ExportMode.FULL ? "完整稿件（含草稿）" : "已发布章节")
          .append("    导出日期：").append(LocalDate.now().format(DATE_FMT))
          .append(LINE_SEPARATOR).append(LINE_SEPARATOR);

        Set<Integer> seenOrder = new HashSet<>();
        for (Chapter chapter : chapters) {
            validateChapter(chapter, seenOrder);
            sb.append(chapter.getTitle().trim());
            if (chapter.getStatus() == ChapterStatus.DRAFT) {
                sb.append("【草稿】");
            }
            sb.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            sb.append(chapter.getContent().trim());
            sb.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    private void validateChapter(Chapter chapter, Set<Integer> seenOrder) {
        Long id = chapter.getId();
        if (chapter.getOrderNo() == null) {
            throw new ChapterDataCorruptedException(id,
                    "章节数据损坏：章节 #" + id + " 缺少章节序号，无法确定排序。");
        }
        if (!seenOrder.add(chapter.getOrderNo())) {
            throw new ChapterDataCorruptedException(id,
                    "章节数据损坏：章节序号 " + chapter.getOrderNo() + " 重复，章节顺序冲突。");
        }
        if (chapter.getTitle() == null || chapter.getTitle().isBlank()) {
            throw new ChapterDataCorruptedException(id,
                    "章节数据损坏：第 " + chapter.getOrderNo() + " 章缺少标题。");
        }
        if (chapter.getContent() == null || chapter.getContent().isBlank()) {
            throw new ChapterDataCorruptedException(id,
                    "章节数据损坏：第 " + chapter.getOrderNo() + " 章《"
                            + chapter.getTitle().trim() + "》正文为空或已损坏。");
        }
        if (chapter.getStatus() == null) {
            throw new ChapterDataCorruptedException(id,
                    "章节数据损坏：第 " + chapter.getOrderNo() + " 章缺少发布状态。");
        }
    }

    /**
     * 去掉文件名中操作系统不允许的字符，避免因作品名含特殊符号导致文件写入失败。
     */
    private String sanitizeFileName(String title) {
        if (title == null || title.isBlank()) {
            return "untitled";
        }
        String cleaned = title.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        return cleaned.isBlank() ? "untitled" : cleaned;
    }
}
