<template>
  <aside class="admin-sidebar">
    <div class="brand"><b>M</b><span>MovieRec<small>管理控制台</small></span></div>
    <nav aria-label="管理员导航">
      <RouterLink v-for="item in items" :key="item.to" :to="item.to" exact-active-class="active">{{ item.label }}</RouterLink>
    </nav>
    <div class="account">
      <el-avatar :size="32">{{ initial }}</el-avatar>
      <span>{{ user.username || '管理员' }}<small>管理员</small></span>
      <el-button text title="退出登录" @click="logout"><el-icon><SwitchButton /></el-icon></el-button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const user = useUserStore()
const router = useRouter()
const initial = computed(() => (user.username || 'A').slice(0, 1).toUpperCase())
const items = [
  { to: '/dashboard', label: '系统总览' },
  { to: '/movies', label: '电影管理' },
  { to: '/users', label: '用户管理' },
  { to: '/audit', label: '操作审计' },
  { to: '/tasks', label: '数据任务' }
]

async function logout() {
  await user.logout()
  router.replace('/login')
}
</script>

<style scoped>
.admin-sidebar{position:fixed;inset:0 auto 0 0;width:220px;background:#fff;border-right:1px solid #e5e9ed;padding:24px 14px 16px;display:flex;flex-direction:column;z-index:2}.brand{display:flex;gap:10px;align-items:center;padding:0 10px 30px;font-weight:700}.brand b{display:grid;place-items:center;width:36px;height:36px;border-radius:7px;background:#176b63;color:#fff;font-size:18px}.brand span,.brand small,.account span,.account small{display:block}.brand small,.account small{color:#97a1a9;font-size:11px;font-weight:400;margin-top:3px}.admin-sidebar nav{display:grid;gap:5px}.admin-sidebar nav a{padding:11px 12px;color:#68757e;text-decoration:none;border-radius:5px;font-size:13px}.admin-sidebar nav a.active,.admin-sidebar nav a:hover{background:#eaf4f2;color:#176b63;font-weight:600}.account{margin-top:auto;border-top:1px solid #edf0f2;padding-top:14px;display:flex;align-items:center;gap:8px;font-size:12px}.account span{flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
@media(max-width:900px){.admin-sidebar{width:180px}}
</style>
