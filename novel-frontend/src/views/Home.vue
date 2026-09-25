<template>
  <div class="container">
    <div class="hero">
      <h1 class="hero-title text-gradient">未来图书馆</h1>
      <p class="hero-subtitle">探索无限世界</p>
      
      <div class="search-wrapper glass-panel">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索你的下一场冒险..."
          class="custom-search"
          size="large"
          @keyup.enter="fetchNovels"
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </div>

    <div v-loading="loading" class="novel-grid">
      <router-link
        v-for="novel in novels"
        :key="novel.id"
        :to="'/novel/' + novel.id"
        class="novel-card glass-panel hover-lift"
      >
        <div class="card-image" :style="{ backgroundImage: 'url(' + novel.coverUrl + ')' }">
          <div class="card-overlay">
            <span class="view-btn">立即阅读</span>
          </div>
        </div>
        <div class="card-content">
          <h3 class="novel-title">{{ novel.title }}</h3>
          <p class="desc">{{ novel.description }}</p>
          <div class="meta">
            <span class="date">{{ formatDate(novel.createdAt) }}</span>
          </div>
        </div>
      </router-link>
    </div>

    <div class="pagination-wrapper" v-if="total > size">
       <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        @current-change="handlePageChange"
        class="custom-pagination"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Search } from '@element-plus/icons-vue'

const novels = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const fetchNovels = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${API_URL}/novels`, {
      params: {
        page: page.value,
        size: size.value,
        keyword: searchKeyword.value
      }
    })
    novels.value = res.data.data
    total.value = res.data.total
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (val) => {
  page.value = val
  fetchNovels()
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  fetchNovels()
})
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 80px 0 60px;
}

.hero-title {
  font-size: 4rem;
  margin-bottom: 20px;
  line-height: 1.1;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.hero-subtitle {
  font-size: 1.25rem;
  color: var(--text-sub);
  margin-bottom: 40px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.search-wrapper {
  max-width: 600px;
  margin: 0 auto;
  padding: 10px;
  background: white;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}

:deep(.custom-search .el-input__wrapper) {
  background-color: transparent;
  box-shadow: none;
  font-size: 1.1rem;
  padding: 10px;
}

:deep(.custom-search .el-input__inner) {
  color: var(--text-main);
}

.search-icon {
  font-size: 1.4rem;
  color: var(--text-sub);
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 30px;
  padding: 20px 0 60px;
}

.novel-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  border: 1px solid rgba(255, 255, 255, 0.05); /* Softer border */
}

.card-image {
  height: 220px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.card-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  opacity: 0;
  transition: opacity 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.novel-card:hover .card-overlay {
  opacity: 1;
}

.view-btn {
  padding: 10px 24px;
  background: var(--primary-color);
  color: white;
  border-radius: 30px;
  font-weight: 600;
  transform: translateY(20px);
  transition: transform 0.3s;
}

.novel-card:hover .view-btn {
  transform: translateY(0);
}

.card-content {
  padding: 24px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.novel-title {
  margin-bottom: 12px;
  font-size: 1.35rem;
  line-height: 1.3;
}

.desc {
  font-size: 0.95rem;
  color: var(--text-sub);
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

.meta {
  margin-top: auto;
  font-size: 0.85rem;
  color: var(--slate-500);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 15px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 60px;
}

/* Override Element Pagination for Dark Theme */
:deep(.el-pagination.is-background .el-pager li:not(.is-disabled)) {
  background-color: transparent;
  color: var(--text-sub);
  border: 1px solid var(--border-color);
}

:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: var(--primary-color);
  color: white;
}
</style>
