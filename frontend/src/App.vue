<template>
  <div id="app">
    <el-container class="app-container">
      <!-- 左侧侧边栏 -->
      <el-aside width="220px" class="sidebar">
        <Sidebar />
      </el-aside>

      <!-- 右侧内容区 -->
      <el-container class="main-wrapper">
        <!-- 顶栏 -->
        <el-header class="top-bar" height="56px">
          <div class="top-bar-left">
            <el-input
              v-model="searchQuery"
              placeholder="搜索电影..."
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              clearable
              class="search-input"
            />
          </div>
          <div class="top-bar-right">
            <template v-if="userStore.token">
              <el-dropdown @command="handleCommand">
                <div class="user-info">
                  <el-avatar :size="32" class="user-avatar">
                    {{ userStore.username?.charAt(0)?.toUpperCase() }}
                  </el-avatar>
                  <span class="user-name">{{ userStore.username }}</span>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                    <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                    <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" round @click="$router.push('/login')">登录</el-button>
              <el-button round @click="$router.push('/register')">注册</el-button>
            </template>
          </div>
        </el-header>

        <!-- 内容区 -->
        <el-main class="content-area">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from './store/user'
import Sidebar from './components/Sidebar.vue'

const router = useRouter()
const userStore = useUserStore()
const searchQuery = ref('')

onMounted(() => {
  if (userStore.token && !userStore.username) {
    userStore.fetchUserInfo()
  }
})

function handleSearch() {
  const q = searchQuery.value.trim()
  if (q) {
    router.push({ path: '/search', query: { q } })
  }
}

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'logout') {
    userStore.logout()
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #F7F9FC;
}

#app {
  height: 100vh;
  overflow: hidden;
}

.app-container {
  height: 100vh;
}

/* 侧边栏 */
.sidebar {
  background: linear-gradient(180deg, #f8faff 0%, #ffffff 100%);
  border-right: 1px solid #eef2f7;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
  overflow-y: auto;
  overflow-x: hidden;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
}

/* 右侧主区域 */
.main-wrapper {
  margin-left: 220px;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 顶栏 */
.top-bar {
  background: #ffffff;
  border-bottom: 1px solid #eef2f7;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.top-bar-left {
  flex: 1;
  max-width: 480px;
}

.search-input {
  width: 100%;
}

.search-input .el-input__wrapper {
  border-radius: 20px;
  background: #f5f7fa;
  box-shadow: none !important;
}

.search-input .el-input__wrapper:hover {
  background: #eef2f7;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: #5B8DEF;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: #2C3E50;
  font-weight: 500;
}

/* 内容区 */
.content-area {
  background: #F7F9FC;
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* 全局Element Plus覆盖 */
.el-button--primary {
  --el-button-bg-color: #5B8DEF;
  --el-button-border-color: #5B8DEF;
  --el-button-hover-bg-color: #4a7de0;
  --el-button-hover-border-color: #4a7de0;
}

.el-menu-item.is-active {
  background-color: #ecf2fe !important;
  color: #5B8DEF !important;
  border-radius: 8px;
  margin: 2px 8px;
}

.el-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.el-tag {
  border-radius: 16px;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #98a2b3;
}
</style>
