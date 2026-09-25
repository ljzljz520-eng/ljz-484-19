<template>
  <div class="container admin-page">
    <div class="admin-header">
      <h1 class="page-title">作者后台 · 稿件导出</h1>
      <p class="page-desc">选择一本作品，可导出已发布章节或包含草稿的完整稿件。</p>
    </div>

    <el-card class="glass-panel export-card" shadow="never">
      <el-form label-position="top">
        <el-form-item label="选择作品">
          <el-select
            v-model="selectedNovelId"
            placeholder="请选择一本作品"
            size="large"
            class="novel-select"
            @change="onNovelChange"
          >
            <el-option
              v-for="novel in novels"
              :key="novel.id"
              :label="novel.title"
              :value="novel.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="导出范围" v-if="selectedNovelId">
          <el-radio-group v-model="exportMode">
            <el-radio value="published" border>已发布章节</el-radio>
            <el-radio value="full" border>完整稿件（含草稿）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="selectedNovelId">
          <el-button
            type="primary"
            size="large"
            round
            :loading="exporting"
            @click="handleExport"
          >
            {{ exporting ? '正在导出…' : '导出章节文本 (.txt)' }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="errorMessage"
        :title="errorTitle"
        :type="errorType"
        :description="errorMessage"
        show-icon
        :closable="true"
        class="export-alert"
        @close="errorMessage = ''"
      />
      <el-alert
        v-if="successMessage"
        title="导出成功"
        type="success"
        :description="successMessage"
        show-icon
        :closable="true"
        class="export-alert"
        @close="successMessage = ''"
      />
    </el-card>

    <el-card v-if="selectedNovel" class="glass-panel preview-card" shadow="never">
      <template #header>
        <div class="preview-header">
          <span>{{ selectedNovel.title }} · 章节预览</span>
          <el-tag size="small" type="info">共 {{ chapters.length }} 章</el-tag>
        </div>
      </template>
      <el-table :data="chapters" v-loading="chaptersLoading" stripe>
        <el-table-column prop="orderNo" label="序号" width="80" />
        <el-table-column prop="title" label="章节标题" min-width="220" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DRAFT' ? 'warning' : 'success'" size="small">
              {{ row.status === 'DRAFT' ? '草稿' : '已发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否包含在本次导出" min-width="180">
          <template #default="{ row }">
            <el-tag
              :type="included(row) ? 'primary' : 'info'"
              size="small"
              effect="plain"
            >
              {{ included(row) ? '包含' : '不包含（草稿）' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const novels = ref([])
const chapters = ref([])
const selectedNovelId = ref(null)
const exportMode = ref('published')
const exporting = ref(false)
const chaptersLoading = ref(false)

const errorTitle = ref('')
const errorType = ref('error')
const errorMessage = ref('')
const successMessage = ref('')

const selectedNovel = computed(() =>
  novels.value.find((n) => n.id === selectedNovelId.value) || null
)

const fetchNovels = async () => {
  try {
    const res = await axios.get(`${API_URL}/novels`, {
      params: { page: 1, size: 100 }
    })
    novels.value = res.data.data
  } catch (err) {
    showError('加载失败', '无法加载作品列表，请检查后端服务是否可用。')
  }
}

const fetchChapters = async (novelId) => {
  chaptersLoading.value = true
  try {
    const res = await axios.get(`${API_URL}/novels/${novelId}/chapters`)
    chapters.value = res.data
  } catch (err) {
    chapters.value = []
    ElMessage.error('加载章节列表失败')
  } finally {
    chaptersLoading.value = false
  }
}

const onNovelChange = (novelId) => {
  errorMessage.value = ''
  successMessage.value = ''
  chapters.value = []
  if (novelId) fetchChapters(novelId)
}

const included = (row) =>
  exportMode.value === 'full' || row.status !== 'DRAFT'

const handleExport = async () => {
  if (!selectedNovelId.value) {
    ElMessage.warning('请先选择一本作品')
    return
  }
  exporting.value = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const res = await axios.get(
      `${API_URL}/novels/${selectedNovelId.value}/export`,
      {
        params: { mode: exportMode.value },
        responseType: 'blob'
      }
    )

    const fileName = extractFileName(res.headers['content-disposition']) ||
      `${selectedNovel.value.title}_导出.txt`

    triggerBrowserDownload(res.data, fileName)
    successMessage.value = `文件「${fileName}」已开始下载，章节已按顺序合并。`
  } catch (err) {
    await handleExportError(err)
  } finally {
    exporting.value = false
  }
}

const handleExportError = async (err) => {
  // responseType 为 blob 时，错误体也是 Blob，需要读取后解析 JSON
  const data = err.response && err.response.data
  let parsed = null
  if (data instanceof Blob) {
    try {
      parsed = JSON.parse(await data.text())
    } catch (e) {
      parsed = null
    }
  } else if (data && typeof data === 'object') {
    parsed = data
  }

  const code = parsed && parsed.error
  const detail = parsed && parsed.detail

  if (code === 'CHAPTER_DATA_CORRUPTED') {
    showError(
      '章节数据损坏',
      `导出已中止：${detail || '存在损坏的章节数据'}。请先修复该章节后再导出。`
    )
  } else if (code === 'EXPORT_FILE_WRITE_FAILED') {
    showError(
      '文件写入失败',
      `服务器生成导出文件时出现写入问题：${detail || 'IO 错误'}。请稍后重试，或联系管理员检查磁盘空间与权限。`
    )
  } else if (code === 'RESOURCE_NOT_FOUND') {
    showError('作品不存在', detail || '所选作品可能已被删除，请刷新列表后重试。')
  } else if (err.response) {
    showError('导出失败', detail || `服务器返回错误（HTTP ${err.response.status}）。`)
  } else {
    showError('网络错误', '无法连接导出服务，请检查网络或后端是否运行。')
  }
}

const showError = (title, message) => {
  errorTitle.value = title
  errorType.value = 'error'
  errorMessage.value = message
}

const extractFileName = (disposition) => {
  if (!disposition) return ''
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match) return decodeURIComponent(utf8Match[1])
  const plainMatch = /filename="?([^";]+)"?/i.exec(disposition)
  return plainMatch ? plainMatch[1] : ''
}

const triggerBrowserDownload = (blob, fileName) => {
  const url = window.URL.createObjectURL(
    blob instanceof Blob ? blob : new Blob([blob])
  )
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

onMounted(fetchNovels)
</script>

<style scoped>
.admin-page {
  padding-top: 40px;
  max-width: 900px;
}

.page-title {
  font-size: 2rem;
  margin-bottom: 10px;
}

.page-desc {
  color: var(--text-sub);
  margin-bottom: 30px;
}

.export-card {
  padding: 20px 30px;
  background: white;
  margin-bottom: 30px;
}

.novel-select {
  width: 100%;
}

.export-alert {
  margin-top: 10px;
}

.preview-card {
  background: white;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
</style>
