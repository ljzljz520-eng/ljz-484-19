package com.novel.service;

import com.novel.exception.ChapterDataCorruptedException;
import com.novel.exception.ExportFileWriteException;
import com.novel.exception.NoExportableChaptersException;
import com.novel.exception.ResourceNotFoundException;
import com.novel.model.Chapter;
import com.novel.model.ChapterStatus;
import com.novel.model.Novel;
import com.novel.repository.DataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 稿件导出服务：将一部作品的章节按顺序合并为纯文本稿件并写入导出目录。
 *
 * 失败语义明确区分两类问题：
 * - 章节数据损坏 → {@link ChapterDataCorruptedException}
 * - 文件写入失败 → {@link ExportFileWriteException}
 */
@Service
public class ExportService {

    static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DataRepository dataRepository;
    private final Path exportDir;

    public ExportService(DataRepository dataRepository,
            @Value("${novel.export.dir:exports}") String exportDir) {
        this.dataRepository = dataRepository;
        this.exportDir = Paths.get(exportDir);
    }

    /**
     * 导出指定作品的稿件文件。
     *
     * @param novelId       作品 ID
     * @param includeDrafts true 导出含草稿的完整稿件；false 仅导出已发布章节
     * @return 导出结果（文件路径、下载文件名、章节数）
     */
    public ExportResult exportNovel(Long novelId, boolean includeDrafts) {
        Novel novel = dataRepository.findNovelById(novelId);
        if (novel == null) {
            throw new ResourceNotFoundException("作品不存在，ID: " + novelId);
        }

        List<Chapter> chapters = selectChapters(novelId, includeDrafts);
        if (chapters.isEmpty()) {
            throw new NoExportableChaptersException(includeDrafts
                    ? "《" + novel.getTitle() + "》尚无任何章节，无法导出。"
                    : "《" + novel.getTitle() + "》暂无已发布章节，可尝试导出包含草稿的完整稿件。");
        }

        validateChapters(chapters);

        String manuscript = buildManuscript(novel, chapters, includeDrafts);
        String filename = buildFilename(novel.getTitle());

        Path target = exportDir.resolve(filename);
        try {
            Files.createDirectories(exportDir);
            Files.writeString(target, manuscript, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException e) {
            throw new ExportFileWriteException(
                    "文件写入失败：无法将稿件写入导出目录（" + exportDir.toAbsolutePath()
                            + "），请检查磁盘空间与目录写权限。",
                    e);
        }

        return new ExportResult(target, filename, chapters.size());
    }

    /** 按导出范围筛选章节，并按章节序号升序排列。 */
    private List<Chapter> selectChapters(Long novelId, boolean includeDrafts) {
        return dataRepository.findChaptersByNovelId(novelId).stream()
                .filter(c -> includeDrafts || c.getStatus() == ChapterStatus.PUBLISHED)
                .sorted((a, b) -> {
                    Integer oa = a.getOrderNo();
                    Integer ob = b.getOrderNo();
                    if (oa == null && ob == null) {
                        return a.getId().compareTo(b.getId());
                    }
                    if (oa == null) {
                        return 1;
                    }
                    if (ob == null) {
                        return -1;
                    }
                    int byOrder = oa.compareTo(ob);
                    return byOrder != 0 ? byOrder : a.getId().compareTo(b.getId());
                })
                .toList();
    }

    /** 导出前校验章节数据完整性，发现问题即视为“章节数据损坏”。 */
    private void validateChapters(List<Chapter> chapters) {
        for (Chapter c : chapters) {
            String label = describe(c);
            if (c.getTitle() == null || c.getTitle().isBlank()) {
                throw new ChapterDataCorruptedException(
                        "章节数据损坏：" + label + "（ID " + c.getId() + "）缺少标题，请修复后重新导出。");
            }
            if (c.getContent() == null || c.getContent().isBlank()) {
                throw new ChapterDataCorruptedException(
                        "章节数据损坏：第 " + safeOrder(c) + " 章《" + c.getTitle()
                                + "》（ID " + c.getId() + "）正文为空，请修复后重新导出。");
            }
        }
    }

    private String describe(Chapter c) {
        if (c.getOrderNo() != null) {
            return "第 " + c.getOrderNo() + " 章";
        }
        return "存在未排序号的章节";
    }

    private String safeOrder(Chapter c) {
        return c.getOrderNo() == null ? "?" : String.valueOf(c.getOrderNo());
    }

    /** 按章节顺序合并为一份纯文本稿件。 */
    private String buildManuscript(Novel novel, List<Chapter> chapters, boolean includeDrafts) {
        StringBuilder sb = new StringBuilder();
        sb.append("《").append(novel.getTitle()).append("》\n");
        sb.append("导出日期：").append(LocalDate.now().format(FILE_DATE_FORMAT)).append("\n");
        sb.append("导出范围：").append(includeDrafts ? "完整稿件（含草稿）" : "仅已发布章节").append("\n");
        sb.append("章节总数：").append(chapters.size()).append("\n");
        sb.append("==================================================\n\n");

        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            sb.append(c.getTitle().trim());
            if (c.getStatus() == ChapterStatus.DRAFT) {
                sb.append("【草稿】");
            }
            sb.append("\n\n");
            sb.append(c.getContent().trim()).append("\n");
            if (i < chapters.size() - 1) {
                sb.append("\n--------------------------------------------------\n\n");
            }
        }
        return sb.toString();
    }

    /** 文件名规则：作品名_导出日期.txt，并剔除文件系统非法字符。 */
    private String buildFilename(String novelTitle) {
        String safeTitle = sanitizeFilename(novelTitle);
        if (safeTitle.isBlank()) {
            safeTitle = "未命名作品";
        }
        return safeTitle + "_" + LocalDate.now().format(FILE_DATE_FORMAT) + ".txt";
    }

    static String sanitizeFilename(String name) {
        if (name == null) {
            return "";
        }
        // 替换 Windows/Unix 文件系统非法字符及控制字符
        return name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
    }

    /** 导出结果记录。 */
    public record ExportResult(Path file, String filename, int chapterCount) {
    }
}
