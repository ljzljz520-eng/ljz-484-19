package com.novel.repository;

import com.novel.model.Chapter;
import com.novel.model.ChapterStatus;
import com.novel.model.Novel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DataRepository {
        /** 章节排序：按序号升序，序号缺失的排最后，再按 ID 兜底，避免空指针。 */
        private static final Comparator<Chapter> BY_ORDER_NO = Comparator
                        .comparing(Chapter::getOrderNo, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Chapter::getId, Comparator.nullsLast(Comparator.naturalOrder()));

        private final Map<Long, Novel> novels = new ConcurrentHashMap<>();
        private final Map<Long, Chapter> chapters = new ConcurrentHashMap<>();
        private final AtomicLong novelIdGenerator = new AtomicLong(1);
        private final AtomicLong chapterIdGenerator = new AtomicLong(1);

        @PostConstruct
        public void init() {
                // Seeding Data
                Novel novel1 = new Novel(novelIdGenerator.getAndIncrement(),
                                "星际穿越之编程大师",
                                "讲述一位程序员意外穿越到未来，用代码拯救宇宙的故事。",
                                "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?q=80&w=800&auto=format&fit=crop",
                                LocalDateTime.now());
                novels.put(novel1.getId(), novel1);

                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第一章：Hello World", 1, "他醒来时，发现眼前只有绿色的代码流...", LocalDateTime.now()));
                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第二章：变量声明", 2, "“你是谁？”面前的机器人冷冷地问道。“Define me.”他回答。", LocalDateTime.now()));
                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第三章：循环陷阱", 3, "时间仿佛陷入了死循环，他必须找到 break 的条件。", LocalDateTime.now()));
                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel1.getId(),
                                "第四章：异常捕获（草稿）", 4, "try 住最后的机会，catch 住她的目光……（未完待续）",
                                LocalDateTime.now(), ChapterStatus.DRAFT));

                Novel novel2 = new Novel(novelIdGenerator.getAndIncrement(),
                                "灵气复苏时代的架构师",
                                "灵气复苏，万物进化。他发现修仙法门竟然符合微服务架构原理。",
                                "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=600&auto=format&fit=crop",
                                LocalDateTime.now());
                novels.put(novel2.getId(), novel2);

                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel2.getId(),
                                "第一章：单体应用破碎", 1, "天地巨变，世界原本的秩序（Monolith）崩塌了。", LocalDateTime.now()));
                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel2.getId(),
                                "第二章：服务发现", 2, "他感应到了周围的灵气节点，就像注册中心里的服务一样清晰。", LocalDateTime.now()));
                chapters.put(chapterIdGenerator.get(), new Chapter(chapterIdGenerator.getAndIncrement(), novel2.getId(),
                                "第三章：熔断结界（草稿）", 3, "灵气洪流即将冲垮经脉，他急布下一道熔断结界……（大纲待定）",
                                LocalDateTime.now(), ChapterStatus.DRAFT));

                Novel novel3 = new Novel(novelIdGenerator.getAndIncrement(),
                                "只有我知道剧情的测试员",
                                "作为世界系统的唯一QA，他能看到由于Bug导致的隐藏剧情。",
                                "https://images.unsplash.com/photo-1555949963-ff9fe0c870eb?q=80&w=800&auto=format&fit=crop",
                                LocalDateTime.now());
                novels.put(novel3.getId(), novel3);
        }

        public List<Novel> findAllNovels(String keyword, int page, int size) {
                return novels.values().stream()
                                .filter(n -> keyword == null || keyword.isEmpty() || n.getTitle().contains(keyword)
                                                || n.getDescription().contains(keyword))
                                .sorted(Comparator.comparing(Novel::getId).reversed())
                                .skip((long) (page - 1) * size)
                                .limit(size)
                                .collect(Collectors.toList());
        }

        public long countNovels(String keyword) {
                return novels.values().stream()
                                .filter(n -> keyword == null || keyword.isEmpty() || n.getTitle().contains(keyword)
                                                || n.getDescription().contains(keyword))
                                .count();
        }

        public Novel findNovelById(Long id) {
                return novels.get(id);
        }

        /** 返回该作品的全部章节（含草稿），按章节序号升序。 */
        public List<Chapter> findChaptersByNovelId(Long novelId) {
                return chapters.values().stream()
                                .filter(c -> c.getNovelId().equals(novelId))
                                .sorted(BY_ORDER_NO)
                                .collect(Collectors.toList());
        }

        /** 仅返回已发布章节，供读者侧页面使用。 */
        public List<Chapter> findPublishedChaptersByNovelId(Long novelId) {
                return chapters.values().stream()
                                .filter(c -> c.getNovelId().equals(novelId))
                                .filter(c -> c.getStatus() == ChapterStatus.PUBLISHED)
                                .sorted(BY_ORDER_NO)
                                .collect(Collectors.toList());
        }

        public Chapter findChapterById(Long id) {
                return chapters.get(id);
        }

        /** 新增/更新作品（若 ID 为空则自动分配），返回保存后的实体。 */
        public Novel saveNovel(Novel novel) {
                if (novel.getId() == null) {
                        novel.setId(novelIdGenerator.getAndIncrement());
                } else {
                        novelIdGenerator.accumulateAndGet(novel.getId() + 1, Math::max);
                }
                novels.put(novel.getId(), novel);
                return novel;
        }

        /** 新增/更新章节（若 ID 为空则自动分配），返回保存后的实体。 */
        public Chapter saveChapter(Chapter chapter) {
                if (chapter.getId() == null) {
                        chapter.setId(chapterIdGenerator.getAndIncrement());
                } else {
                        chapterIdGenerator.accumulateAndGet(chapter.getId() + 1, Math::max);
                }
                chapters.put(chapter.getId(), chapter);
                return chapter;
        }

        public void deleteChapter(Long id) {
                chapters.remove(id);
        }

        public void deleteNovel(Long id) {
                novels.remove(id);
        }
}
