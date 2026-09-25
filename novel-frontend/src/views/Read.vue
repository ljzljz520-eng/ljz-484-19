<template>
  <div class="read-page" v-loading="loading">

     <div class="reader-container">
         <div class="content-paper" v-if="chapter">
             <h2 class="chapter-heading">{{ chapter.title }}</h2>
             <div class="text-content font-serif">
                 <p v-for="(para, idx) in paragraphs" :key="idx">{{ para }}</p>
             </div>
         </div>
         
         <div class="footer-controls" v-if="chapter">
             <!-- Navigation logic could be added here if we fetched next/prev IDs -->
             <el-button class="nav-chapter-btn glass-panel" @click="goBack">返回目录</el-button>
         </div>
     </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ArrowLeft, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const chapter = ref(null)
const loading = ref(true)
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const fetchChapter = async () => {
    try {
        const res = await axios.get(`${API_URL}/chapters/${route.params.id}`)
        chapter.value = res.data
    } catch(err) {
        console.error(err)
    } finally {
        loading.value = false
    }
}

const paragraphs = computed(() => {
    if(!chapter.value || !chapter.value.content) return []
    return chapter.value.content.split('\n')
})

const goBack = () => {
    if(window.history.length > 1) {
        router.back()
    } else {
        router.push('/')
    }
}

onMounted(fetchChapter)
</script>

<style scoped>
.read-page {
    min-height: 100vh;
    background-color: #fcf6e5; /* Warm paper background */
    color: #374151;
    position: relative;
    padding-top: 60px;
}

.reader-container {
    max-width: 720px; /* Optimal reading width */
    margin: 0 auto;
    padding: 0 20px 80px;
}

.content-paper {
    padding: 0px 0 40px;
}

.chapter-heading {
    text-align: center;
    font-size: 2rem;
    margin-bottom: 3rem;
    color: #111827;
    font-family: 'Merriweather', serif;
}

.text-content {
    font-size: 1.25rem;
    line-height: 2;
    color: #374151;
}

.text-content p {
    margin-bottom: 2em;
    text-align: justify;
}

.footer-controls {
    margin-top: 60px;
    display: flex;
    justify-content: center;
}

.nav-chapter-btn {
    padding: 20px 40px;
    background: transparent;
    color: #4b5563;
    border: 1px solid rgba(0, 0, 0, 0.1);
    transition: all 0.3s;
}

.nav-chapter-btn:hover {
    background: rgba(0, 0, 0, 0.05);
    border-color: var(--primary-color);
    color: var(--primary-color);
}
</style>
