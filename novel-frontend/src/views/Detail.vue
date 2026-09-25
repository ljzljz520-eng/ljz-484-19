<template>
  <div class="page-container" v-loading="loading">
    
    <!-- Dynamic Backdrop -->
    <div class="backdrop" v-if="novel" :style="{ backgroundImage: 'url(' + novel.coverUrl + ')' }"></div>
    <div class="backdrop-overlay"></div>

    <div class="container content-wrapper">
      <div v-if="novel" class="novel-header">
        <el-button @click="goBack" circle plain icon="ArrowLeft" class="back-btn"></el-button>
        
        <div class="header-inner glass-panel">
          <div class="cover-wrapper">
             <img :src="novel.coverUrl" class="cover-img" />
          </div>
          <div class="info-content">
            <h1 class="novel-title text-gradient">{{ novel.title }}</h1>
            <div class="meta-tags">
                 <span class="meta-item">{{ formatDate(novel.createdAt) }}</span>
                 <span class="meta-item">{{ chapters.length }} 章</span>
            </div>
            <p class="description">{{ novel.description }}</p>
            <el-button type="primary" size="large" round class="start-read-btn" @click="startReading">
                开始阅读
            </el-button>
          </div>
        </div>
      </div>

      <div class="chapters-section glass-panel">
        <h2 class="section-title">章节目录</h2>
        <div class="chapter-grid">
          <router-link
              v-for="chapter in chapters"
              :key="chapter.id"
              :to="'/chapter/' + chapter.id"
              class="chapter-card"
          >
              <span class="chapter-no">{{ formatNumber(chapter.orderNo) }}</span>
              <span class="chapter-title">{{ chapter.title }}</span>
              <span class="status-dot"></span>
          </router-link>
        </div>
         <el-empty v-if="chapters.length === 0" description="暂无章节" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const novel = ref(null)
const chapters = ref([])
const loading = ref(true)
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const fetchDetail = async () => {
  try {
    const res = await axios.get(`${API_URL}/novels/${route.params.id}`)
    novel.value = res.data.novel
    chapters.value = res.data.chapters
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
    router.back()
}

const startReading = () => {
    if (chapters.value.length > 0) {
        router.push('/chapter/' + chapters.value[0].id)
    }
}

const formatDate = (val) => {
    if(!val) return ''
    return new Date(val).toLocaleDateString('zh-CN')
}

const formatNumber = (num) => {
    return num.toString().padStart(2, '0')
}

onMounted(fetchDetail)
</script>

<style scoped>
.page-container {
    min-height: 100vh;
    position: relative;
    padding-bottom: 60px;
}

.backdrop {
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 60vh;
    background-size: cover;
    background-position: center;
    z-index: 0;
    filter: blur(20px);
    opacity: 0.3;
}

.backdrop-overlay {
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 70vh;
    background: linear-gradient(to bottom, rgba(255,255,255,0.2), var(--bg-color));
    z-index: 1;
}

.content-wrapper {
    position: relative;
    z-index: 2;
    padding-top: 40px;
}

.back-btn {
    margin-bottom: 20px;
    background: white;
    border: 1px solid rgba(0,0,0,0.1);
    color: var(--text-main);
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.novel-header {
    margin-bottom: 40px;
}

.header-inner {
    display: flex;
    gap: 40px;
    padding: 40px;
    align-items: flex-start;
    background: rgba(255,255,255,0.8);
    backdrop-filter: blur(20px);
}

.cover-img {
    width: 220px;
    border-radius: 8px;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
}

.info-content {
    flex: 1;
}

.novel-title {
    font-size: 2.5rem;
    margin-bottom: 15px;
}

.meta-tags {
    display: flex;
    gap: 20px;
    color: var(--text-sub);
    font-size: 0.9rem;
    margin-bottom: 25px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}

.description {
    line-height: 1.8;
    color: var(--slate-600);
    font-size: 1.1rem;
    margin-bottom: 30px;
    max-width: 800px;
}

.start-read-btn {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    border: none;
    padding: 24px 40px;
    font-weight: 600;
    font-size: 1.1rem;
    box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);
}

.start-read-btn:hover {
    filter: brightness(1.1);
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(99, 102, 241, 0.4);
}

.section-title {
    font-size: 1.5rem;
    margin-bottom: 25px;
    padding-left: 10px;
    border-left: 4px solid var(--primary-color);
    color: var(--slate-800);
}

.chapters-section {
    padding: 40px;
    background: white;
}

.chapter-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 15px;
}

.chapter-card {
    display: flex;
    align-items: center;
    padding: 20px;
    background: var(--slate-50);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    transition: all 0.2s;
}

.chapter-card:hover {
    background: white;
    border-color: var(--primary-color);
    transform: translateX(5px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.chapter-no {
    font-family: 'Space Mono', monospace;
    color: var(--slate-400);
    margin-right: 15px;
    font-size: 0.9rem;
}

.chapter-title {
    flex: 1;
    font-weight: 500;
}

.status-dot {
    width: 6px;
    height: 6px;
    background-color: var(--primary-color);
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.2s;
}

.chapter-card:hover .status-dot {
    opacity: 1;
}

@media (max-width: 768px) {
    .header-inner {
        flex-direction: column;
        align-items: center;
        text-align: center;
    }
    
    .meta-tags {
        justify-content: center;
    }
    
    .start-read-btn {
        width: 100%;
    }
}
</style>
