<template>
  <div class="sidebar-inner">
    <!-- Logo区域 -->
    <div class="sidebar-logo" @click="$router.push('/')">
      <span class="logo-icon">🎬</span>
      <span class="logo-text">智能电影推荐</span>
    </div>

    <!-- 用户信息卡（登录后显示） -->
    <div class="user-card" v-if="userStore.token">
      <el-avatar :size="44" class="user-card-avatar">
        {{ userStore.username?.charAt(0)?.toUpperCase() }}
      </el-avatar>
      <div class="user-card-info">
        <div class="user-card-name">{{ userStore.username }}</div>
        <div class="user-card-desc">欢迎回来</div>
      </div>
    </div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activeMenu"
      class="sidebar-menu"
      @select="handleSelect"
    >
      <el-menu-item index="/">
        <el-icon><Film /></el-icon>
        <span>热门电影</span>
      </el-menu-item>

      <el-menu-item index="/recommend" v-if="userStore.token">
        <el-icon><Star /></el-icon>
        <span>为你推荐</span>
      </el-menu-item>

      <el-menu-item index='/ai-advisor' v-if='userStore.token'>
        <el-icon><MagicStick /></el-icon>
        <span>AI 选片顾问</span>
      </el-menu-item>

      <el-menu-item index="/search-movies">
        <el-icon><Search /></el-icon>
        <span>搜索电影</span>
      </el-menu-item>

      <el-divider v-if="userStore.token" />

      <el-menu-item index="/profile" v-if="userStore.token">
        <el-icon><User /></el-icon>
        <span>个人资料</span>
      </el-menu-item>

      <el-menu-item index="/settings" v-if="userStore.token">
        <el-icon><Setting /></el-icon>
        <span>修改密码</span>
      </el-menu-item>

      <el-menu-item index="logout" v-if="userStore.token">
        <el-icon><SwitchButton /></el-icon>
        <span>退出系统</span>
      </el-menu-item>
    </el-menu>

    <!-- 底部版本号 -->
    <div class="sidebar-footer">
      <span>v1.0.0</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Film, MagicStick, Star, User, Setting, SwitchButton, Search } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  return route.path
})

async function handleSelect(index: string) {
  if (index === 'logout') {
    await userStore.logout()
    router.push('/login')
    return
  }
  router.push(index)
}
</script>

<style scoped>
.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

/* Logo */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 16px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.sidebar-logo:hover {
  opacity: 0.8;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  color: #2C3E50;
  letter-spacing: 0.5px;
}

/* 用户信息卡 */
.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 16px 16px;
  padding: 14px;
  background: linear-gradient(135deg, #ecf2fe 0%, #f0f5ff 100%);
  border-radius: 12px;
}

.user-card-avatar {
  background: #5B8DEF;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-card-info {
  overflow: hidden;
}

.user-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #2C3E50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-card-desc {
  font-size: 12px;
  color: #8896A4;
  margin-top: 2px;
}

/* 导航菜单 */
.sidebar-menu {
  border-right: none;
  padding: 0 8px;
  flex: 1;
}

.sidebar-menu .el-menu-item {
  height: 44px;
  line-height: 44px;
  border-radius: 10px;
  margin-bottom: 4px;
  font-size: 14px;
  color: #5a6a7e;
  transition: all 0.2s;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #f0f5ff;
  color: #5B8DEF;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #ecf2fe !important;
  color: #5B8DEF !important;
  font-weight: 600;
}

.sidebar-menu .el-menu-item .el-icon {
  font-size: 18px;
  margin-right: 10px;
}

.sidebar-menu .el-divider {
  margin: 8px 12px;
  border-color: #eef2f7;
}

/* 底部 */
.sidebar-footer {
  padding: 16px 20px;
  text-align: center;
  font-size: 11px;
  color: #c0c4cc;
  border-top: 1px solid #eef2f7;
}

@media (max-width: 768px) {
  .sidebar-logo {
    justify-content: center;
    padding: 18px 8px 14px;
  }

  .logo-text,
  .user-card-info,
  .sidebar-menu .el-menu-item span,
  .sidebar-footer {
    display: none;
  }

  .user-card {
    justify-content: center;
    margin: 0 8px 14px;
    padding: 8px;
  }

  .sidebar-menu {
    padding: 0 6px;
  }

  .sidebar-menu .el-menu-item {
    justify-content: center;
    padding: 0 !important;
  }

  .sidebar-menu .el-menu-item .el-icon {
    margin: 0;
  }
}
</style>
