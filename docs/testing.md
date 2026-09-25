# 测试报告 (Testing)

## 1. 测试概览
对小说系统进行的整体功能验证和视觉回归测试。

## 2. 功能测试用例
| 模块 | 测试场景 | 预期结果 | 状态 |
| :--- | :--- | :--- | :--- |
| 首页 | 小说列表展示 | 能够正确显示多本书籍封面及简介 | ✅ 通过 |
| 首页 | 关键词搜索 | 输入标题关键词能实时过滤列表 | ✅ 通过 |
| 详情页 | 目录加载 | 进入书籍后能显示其所有章节列表 | ✅ 通过 |
| 阅读页 | 正文渲染 | 点击章节后能流畅进入阅读页，排版正常 | ✅ 通过 |

## 3. 接口测试 (API Testing)
使用 Swagger 进行接口验证：
- `GET /api/novels`: 返回 200，JSON 包含分页数据。
- `GET /api/novels/1`: 返回书籍详情及章节数据。
- `GET /api/chapters/1`: 正确返回正文字符串。
- `GET /api/novels/1/export?mode=published`: 返回 200 与 `Content-Disposition: attachment; filename*=UTF-8''..._已发布章节.txt`，正文不含草稿章节。
- `GET /api/novels/1/export?mode=full`: 文件名带 `_完整稿件`，草稿章节包含在内并带【草稿】标记。
- `GET /api/novels/999/export`: 返回 404，错误码 `RESOURCE_NOT_FOUND`。
- 章节数据损坏（标题/正文/序号缺失或序号重复）：返回 422，错误码 `CHAPTER_DATA_CORRUPTED`，响应体包含损坏章节 ID 与原因。
- 服务器磁盘写入失败：返回 500，错误码 `EXPORT_FILE_WRITE_FAILED`，与数据损坏错误明确区分。

### 3.1 导出服务单元测试
`ChapterExportServiceTest`（Mockito + JUnit 5）覆盖：
- 已发布模式过滤草稿、按 orderNo 顺序合并；
- 完整稿件模式包含草稿并标注【草稿】；
- 文件名包含作品名（特殊字符已清洗）与当日日期；
- 正文为空、序号重复时抛出 `ChapterDataCorruptedException`；
- 作品不存在时抛出 `ResourceNotFoundException`；
- 临时文件写入后可正确读回内容。

## 4. 兼容性测试
- **设备**: 适配 PC 端 (1440px+) 和 移动端 (iPhone/Android)。
- **浏览器**: 通过 Chrome, Safari 最新版验证。

## 5. 视觉回归
- ✅ **浅色主题**: 全局色彩方案一致，无文字由于对比度不足导致的不可见问题。
- ✅ **阅读体验**: 暖色底色配合 serif 字体，长文阅读无视觉疲劳。
