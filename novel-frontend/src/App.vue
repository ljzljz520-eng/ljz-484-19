<template>
  <el-config-provider :locale="locale">
    <div class="app-layout">
      <header class="app-header">
        <div class="container header-content">
          <router-link to="/" class="logo">
            <span class="logo-icon">📚</span>
            Novels
          </router-link>

           <nav class="app-nav">
             <router-link to="/" class="nav-link">书库</router-link>
             <router-link to="/admin/export" class="nav-link">作者后台</router-link>
           </nav>
        </div>
      </header>
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
const locale = zhCn
</script>

<style scoped>
.app-header {
  height: 64px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-content {
  display: flex;
  align-items: center;
  height: 100%;
}
.logo {
  font-size: 24px;
  font-weight: bold;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo-icon {
  -webkit-text-fill-color: initial;
}
.app-nav {
  margin-left: auto;
  display: flex;
  gap: 24px;
}
.nav-link {
  color: var(--text-sub, #94a3b8);
  font-size: 0.95rem;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s;
}
.nav-link:hover,
.nav-link.router-link-active {
  color: #fff;
}
.app-main {
  min-height: calc(100vh - 64px);
  padding-bottom: 40px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
