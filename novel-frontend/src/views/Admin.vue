<template>
  <div class="container admin-page">
    <div class="admin-hero">
      <h1 class="page-title text-gradient">作者后台</h1>
      <p class="page-subtitle">选择一部作品，导出已发布章节或包含草稿的完整稿件，系统将按章节顺序合并为 .txt 文件</p>
    </div>

    <div class="admin-layout">
      <!-- 作品列表 -->
      <div class="works-panel glass-panel" v-loading="loadingWorks">
        <h2 class="panel-title">我的作品</h2>
        <div
          v-for="novel in novels"
          :key="novel.id"
          class="work-item"
          :class="{ active: selected && selected.id === novel.id }"
          @click="selectNovel(novel)"
        >
          <img :src="novel.coverUrl" class="work-cover" :alt="novel.title" />
          <div class="work-info">
            <div class="work-title">{{ novel.title }}</div>
            <div class="work-desc">{{ novel.description }}</div>
          </div>
          <el-icon class="work-arrow"><ArrowRight /></el-icon>
        </div>
        <el-empty v-if="!loadingWorks && novels.length === 0" description="暂无作品" />
      </div>

      <!-- 导出面板 -->
      <div class="export-panel glass-panel" v-if="selected" v-loading="loadingChapters">
        <h2 class="panel-title">导出稿件</h2>
        <div class="selected-work">
          <span class="selected-name">《{{ selected.title }}》</span>
          <div class="chapter-stats" v-if="chapters">
            <el-tag type="success" effect="light" round>已发布 {{ publishedCount }} 章</el-tag>
            <el-tag type="warning" effect="light" round>草稿 {{ draftCount }} 章</el-tag>
          </div>
        </div>

        <div class="export-options">
          <div class="export-option">
            <div class="option-info">
              <h3>已发布章节</h3>
              <p>仅合并读者可见的已发布章节</p>
            </div>
            <el-button
              type="primary"
              round
              :loading="exporting === 'published'"
              :disabled="exporting !== ''"
              @click="doExport(false)"
            >
              导出已发布
            </el-button>
          </div>
          <div class="export-option">
            <div class="option-info">
              <h3>完整稿件（含草稿）</h3>
              <p>已发布章节与草稿一并按序合并，草稿带【草稿】标记</p>
            </div>
            <el-button
              type="warning"
              round
              :loading="exporting === 'full'"
              :disabled="exporting !== ''"
              @click="doExport(true)"
            >
              导出完整稿件
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="exportError"
          :title="exportError.title"
          :description="exportError.message"
          :type="exportError.type"
          show-icon
          class="export-alert"
          @close="exportError = null"
        />
        <el-alert
          v-if="exportSuccess"
          :title="exportSuccess"
          type="success"
          show-icon
          class="export-alert"
          @close="exportSuccess = ''"
        />

        <p class="filename-hint">文件名格式：作品名_导出日期.txt（如：{{ selected.title }}_{{ today }}.txt）</p>
      </div>

      <div class="export-panel glass-panel placeholder" v-else>
        <el-empty description="请先在左侧选择一部作品" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'

const novels = ref([])
const selected = ref(null)
const chapters = ref(null)
const loadingWorks = ref(false)
const loadingChapters = ref(false)
const exporting = ref('') // '' | 'published' | 'full'
const exportError = ref(null)
const exportSuccess = ref('')

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const today = new Date().toLocaleDateString('sv-SE') // yyyy-MM-dd

const publishedCount = computed(() => (chapters.value || []).filter(c => c.status === 'PUBLISHED').length)
const draftCount = computed(() => (chapters.value || []).filter(c => c.status === 'DRAFT').length)

const fetchNovels = async () => {
  loadingWorks.value = true
  try {
    const res = await axios.get(`${API_URL}/novels`, { params: { page: 1, size: 100 } })
    novels.value = res.data.data
  } catch (err) {
    console.error(err)
    ElMessage.error('作品列表加载失败')
  } finally {
    loadingWorks.value = false
  }
}

const selectNovel = async (novel) => {
  selected.value = novel
  exportError.value = null
  exportSuccess.value = ''
  loadingChapters.value = true
  try {
    const res = await axios.get(`${API_URL}/novels/${novel.id}/chapters`, {
      params: { includeDrafts: true }
    })
    chapters.value = res.data
  } catch (err) {
    console.error(err)
    chapters.value = []
    ElMessage.error('章节信息加载失败')
  } finally {
    loadingChapters.value = false
  }
}

// 后端错误码 → 用户可读的失败分类（文件写入问题 / 章节数据损坏等）
const ERROR_META = {
  FILE_WRITE_ERROR: { title: '文件写入失败', type: 'error' },
  CHAPTER_DATA_CORRUPTED: { title: '章节数据损坏', type: 'error' },
  NO_EXPORTABLE_CHAPTERS: { title: '没有可导出的章节', type: 'warning' },
  NOT_FOUND: { title: '作品不存在', type: 'error' }
}

const doExport = async (includeDrafts) => {
  if (!selected.value || exporting.value) return
  exporting.value = includeDrafts ? 'full' : 'published'
  exportError.value = null
  exportSuccess.value = ''
  try {
    const res = await axios.get(`${API_URL}/novels/${selected.value.id}/export`, {
      params: { includeDrafts },
      responseType: 'blob'
    })
    const filename = parseFilename(res.headers['content-disposition'])
      || `${selected.value.title}_${today}.txt`
    downloadBlob(res.data, filename)
    exportSuccess.value = `已导出：${filename}`
    ElMessage.success('导出成功')
  } catch (err) {
    exportError.value = await normalizeError(err)
  } finally {
    exporting.value = ''
  }
}

const downloadBlob = (data, filename) => {
  const blob = new Blob([data], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

const parseFilename = (disposition) => {
  if (!disposition) return null
  const star = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/.exec(disposition)
  if (star) {
    try {
      return decodeURIComponent(star[1].trim())
    } catch (e) {
      // fall through to ASCII filename
    }
  }
  const quoted = /filename\s*=\s*"([^"]+)"/.exec(disposition)
  if (quoted) return quoted[1]
  const plain = /filename\s*=\s*([^;]+)/.exec(disposition)
  return plain ? plain[1].trim() : null
}

const normalizeError = async (err) => {
  const fallback = { title: '导出失败', message: '网络或服务异常，请稍后重试。', type: 'error' }
  const res = err && err.response
  if (!res) return fallback
  let data = res.data
  if (data instanceof Blob) {
    try {
      data = JSON.parse(await data.text())
    } catch (e) {
      data = null
    }
  }
  if (data && data.code) {
    const meta = ERROR_META[data.code] || { title: '导出失败', type: 'error' }
    return { title: meta.title, message: data.message || '', type: meta.type }
  }
  return { ...fallback, message: `服务返回异常（HTTP ${res.status}）` }
}

onMounted(fetchNovels)
</script>

<style scoped>
.admin-page {
  padding-bottom: 60px;
}

.admin-hero {
  text-align: center;
  padding: 60px 0 40px;
}

.page-title {
  font-size: 2.8rem;
  margin-bottom: 12px;
}

.page-subtitle {
  color: var(--text-sub);
  font-size: 1.05rem;
  max-width: 640px;
  margin: 0 auto;
}

.admin-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  align-items: start;
}

.works-panel,
.export-panel {
  padding: 30px;
  background: white;
}

.panel-title {
  font-size: 1.3rem;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid var(--primary-color);
  color: var(--slate-800);
}

.work-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--slate-50);
}

.work-item:hover {
  transform: translateX(4px);
  border-color: var(--primary-color);
}

.work-item.active {
  border-color: var(--primary-color);
  background: #eef2ff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

.work-cover {
  width: 48px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.work-info {
  flex: 1;
  min-width: 0;
}

.work-title {
  font-weight: 600;
  color: var(--slate-800);
  margin-bottom: 4px;
}

.work-desc {
  font-size: 0.85rem;
  color: var(--text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.work-arrow {
  color: var(--slate-400);
}

.work-item.active .work-arrow {
  color: var(--primary-color);
}

.selected-work {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
}

.selected-name {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--slate-800);
}

.chapter-stats {
  display: flex;
  gap: 10px;
}

.export-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.export-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--slate-50);
}

.option-info h3 {
  font-size: 1rem;
  margin-bottom: 4px;
  color: var(--slate-800);
}

.option-info p {
  font-size: 0.85rem;
  color: var(--text-sub);
  margin: 0;
}

.export-alert {
  margin-top: 20px;
}

.filename-hint {
  margin-top: 20px;
  font-size: 0.8rem;
  color: var(--slate-400);
}

.export-panel.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
