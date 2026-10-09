<template>
  <div class="admin-shell">
    <AdminSidebar />

    <main class="main-content">
      <header class="topbar"><div><span class="crumb">管理控制台</span><span class="crumb-separator">/</span><strong>系统总览</strong></div><div class="top-actions"><span class="date-label"><el-icon><Calendar /></el-icon>{{ today }}</span><el-button circle plain title="刷新数据" :loading="loading" @click="loadDashboard"><el-icon><Refresh /></el-icon></el-button></div></header>

      <div class="page-heading"><div><h1>系统总览</h1><p>查看平台数据规模、运行状态与近期维护记录。</p></div><el-tag effect="plain" type="success"><span class="online-dot"></span>管理员模式</el-tag></div>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon closable class="error-alert" @close="errorMessage = ''" />

      <section class="stat-grid" aria-label="核心数据统计">
        <article class="stat-card"><div class="stat-top"><span>电影总数</span><span class="stat-icon blue"><el-icon><Film /></el-icon></span></div><strong>{{ number(summary.movieCount) }}</strong><small>今日新增 {{ number(summary.newMoviesToday) }} 部</small></article>
        <article class="stat-card"><div class="stat-top"><span>用户总数</span><span class="stat-icon green"><el-icon><User /></el-icon></span></div><strong>{{ number(summary.userCount) }}</strong><small>今日新增 {{ number(summary.newUsersToday) }} 位</small></article>
        <article class="stat-card"><div class="stat-top"><span>评分记录</span><span class="stat-icon amber"><el-icon><Star /></el-icon></span></div><strong>{{ number(summary.ratingCount) }}</strong><small>用户评分总量</small></article>
        <article class="stat-card"><div class="stat-top"><span>平均评分</span><span class="stat-icon rose"><el-icon><TrendCharts /></el-icon></span></div><strong>{{ summary.averageRating == null ? '--' : Number(summary.averageRating).toFixed(2) }}</strong><small>基于全部评分记录</small></article>
      </section>

      <section class="content-grid">
        <article class="panel health-panel">
          <div class="panel-heading"><div><h2>系统健康</h2><p>服务组件实时状态</p></div><el-button text :loading="healthLoading" title="重新检查" @click="loadHealth"><el-icon><Refresh /></el-icon></el-button></div>
          <div class="service-row"><span><i class="service-dot" :class="stateClass(system.application)" />应用服务</span><el-tag size="small" :type="tagType(system.application)">{{ stateLabel(system.application) }}</el-tag></div>
          <div class="service-row"><span><i class="service-dot" :class="stateClass(system.database)" />数据库连接</span><el-tag size="small" :type="tagType(system.database)">{{ stateLabel(system.database) }}</el-tag></div>
          <div class="service-row"><span><i class="service-dot" :class="stateClass(system.storage)" />海报存储目录</span><el-tag size="small" :type="tagType(system.storage)">{{ stateLabel(system.storage) }}</el-tag></div>
          <div class="health-meta"><span>运行中任务</span><strong>{{ number(system.runningTasks) }}</strong><span>服务运行时长</span><strong>{{ uptime }}</strong></div>
          <div class="checked-at">最近检查：{{ system.serverTime ? formatTime(system.serverTime) : '尚未检查' }}</div>
        </article>

        <article class="panel recent-panel">
          <div class="panel-heading"><div><h2>最近任务</h2><p>最近 5 条数据维护记录</p></div><el-tag size="small" effect="plain">失败 {{ number(summary.failedTaskCount) }}</el-tag></div>
          <div v-if="summary.recentTasks?.length" class="task-list">
            <div v-for="task in summary.recentTasks" :key="task.id" class="task-row"><span class="task-symbol"><el-icon><Download /></el-icon></span><span class="task-info"><strong>{{ taskName(task.taskType) }}</strong><small>{{ task.message || formatTime(task.createTime) }}</small></span><el-tag size="small" :type="taskTagType(task.status)">{{ taskStatus(task.status) }}</el-tag></div>
          </div>
          <el-empty v-else description="暂无任务记录" :image-size="54" />
        </article>
      </section>

      <section id="tasks" class="panel task-panel">
        <div class="panel-heading task-heading"><div><h2>数据维护</h2><p>执行海报下载与 CSV 导入。</p></div></div>
        <div class="action-list"><el-button :loading="loadingAction === 'posters'" :disabled="Boolean(loadingAction)" @click="handlePosters"><el-icon><Picture /></el-icon>下载远程海报</el-button><el-button :loading="loadingAction === 'csv'" :disabled="Boolean(loadingAction)" @click="handleCsv"><el-icon><Upload /></el-icon>导入 CSV 数据</el-button></div>
        <el-alert v-if="taskMessage" :title="taskMessage" :type="taskMessageType" show-icon class="task-alert" />
      </section>
      <footer>MovieRec 管理控制台 <span>服务运行 {{ uptime }}</span></footer>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Download, Film, Picture, Refresh, Star, TrendCharts, Upload, User } from '@element-plus/icons-vue'
import AdminSidebar from '../components/AdminSidebar.vue'
import { getAdminHealth, getSystemHealth } from '../api/admin'

interface TaskRecord { id: number; taskType: string; status: string; message?: string; createTime: string }
const router = useRouter()
const loading = ref(false)
const healthLoading = ref(false)
const loadingAction = ref('')
const errorMessage = ref('')
const taskMessage = ref('')
const taskMessageType = ref<'success' | 'error'>('success')
const summary = ref<{ movieCount?: number; userCount?: number; ratingCount?: number; averageRating?: number; newMoviesToday?: number; newUsersToday?: number; runningTaskCount?: number; failedTaskCount?: number; recentTasks?: TaskRecord[] }>({})
const system = ref<{ application?: string; database?: string; storage?: string; runningTasks?: number; serverTime?: string; uptimeSeconds?: number }>({})
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
const uptime = computed(() => { const seconds = system.value.uptimeSeconds; if (seconds == null) return '--'; const days = Math.floor(seconds / 86400); const hours = Math.floor(seconds % 86400 / 3600); const minutes = Math.floor(seconds % 3600 / 60); return days ? `${days}天 ${hours}小时` : `${hours}小时 ${minutes}分` })

onMounted(loadDashboard)
async function loadDashboard() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [summaryResponse, healthResponse] = await Promise.all([getAdminHealth(), getSystemHealth()]) as any[]
    if (summaryResponse.code !== 200) throw new Error(summaryResponse.message || '读取统计数据失败')
    summary.value = summaryResponse.data || {}
    if (healthResponse.code === 200) system.value = healthResponse.data || {}
  } catch (error: any) {
    errorMessage.value = error.message || '仪表盘数据加载失败，请稍后重试'
  } finally { loading.value = false }
}
async function loadHealth() {
  healthLoading.value = true
  try {
    const response: any = await getSystemHealth()
    if (response.code !== 200) throw new Error(response.message || '健康检查失败')
    system.value = response.data || {}
  } catch (error: any) { errorMessage.value = error.message || '健康检查失败' }
  finally { healthLoading.value = false }
}
async function runTask(action: string, task: () => Promise<any>, successText: string) {
  loadingAction.value = action
  taskMessage.value = ''
  try {
    const response: any = await task()
    if (![200, 202].includes(response.code)) throw new Error(response.message || `${successText}失败`)
    taskMessageType.value = 'success'
    taskMessage.value = response.message || successText
    ElMessage.success(taskMessage.value)
    await loadDashboard()
  } catch (error: any) {
    taskMessageType.value = 'error'
    taskMessage.value = error.message || `${successText}失败`
  } finally { loadingAction.value = '' }
}
function handlePosters() { router.push('/tasks') }
function handleCsv() { router.push('/tasks') }
function number(value?: number) { return value == null ? '--' : new Intl.NumberFormat('zh-CN').format(value) }
function formatTime(value: string) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '--' }
function stateClass(state?: string) { return state === 'UP' ? 'up' : state === 'DOWN' ? 'down' : 'unknown' }
function tagType(state?: string) { return state === 'UP' ? 'success' : state === 'DOWN' ? 'danger' : 'info' }
function stateLabel(state?: string) { return state === 'UP' ? '正常' : state === 'DOWN' ? '异常' : '未知' }
function taskName(type: string) { return ({ AJAX_API: '历史任务', TOP250: '历史任务' } as Record<string, string>)[type] || type || '后台任务' }
function taskStatus(status: string) { return ({ RUNNING: '执行中', SUCCESS: '已完成', FAILED: '失败' } as Record<string, string>)[status] || status }
function taskTagType(status: string) { return status === 'SUCCESS' ? 'success' : status === 'FAILED' ? 'danger' : 'warning' }
</script>

<style scoped>
.admin-shell{min-height:100vh;background:#f4f6f8;color:#263442}.main-content{max-width:1500px;min-height:100vh;margin-left:220px;padding:0 34px 20px}.topbar{height:61px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e6eaed;font-size:12px}.crumb{color:#87929b}.crumb-separator{margin:0 9px;color:#b3bbc1}.topbar strong{font-weight:500}.top-actions{display:flex;align-items:center;gap:16px}.date-label{display:flex;align-items:center;gap:7px;color:#77838e}.page-heading{display:flex;align-items:center;justify-content:space-between;padding:27px 0 21px}.page-heading h1{margin:0;font-size:23px;font-weight:650}.page-heading p,.panel-heading p{margin:6px 0 0;color:#88939c;font-size:12px}.online-dot{display:inline-block;width:6px;height:6px;margin-right:6px;border-radius:50%;background:#2e9a72;vertical-align:1px}.error-alert{margin:0 0 14px}.stat-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.stat-card,.panel{background:#fff;border:1px solid #e5e9ec;border-radius:6px}.stat-card{min-height:133px;padding:16px 18px}.stat-top{display:flex;align-items:center;justify-content:space-between;color:#74808a;font-size:12px}.stat-icon{display:grid;place-items:center;width:30px;height:30px;border-radius:6px;font-size:15px}.stat-icon.blue{background:#eaf2fb;color:#4778a9}.stat-icon.green{background:#e8f4ef;color:#398265}.stat-icon.amber{background:#fbf2df;color:#a78237}.stat-icon.rose{background:#f7eceb;color:#a65c56}.stat-card>strong{display:block;margin-top:10px;font-size:26px;font-weight:650;line-height:1.15;font-variant-numeric:tabular-nums}.stat-card>small{display:block;margin-top:7px;color:#929ca4;font-size:11px}.content-grid{display:grid;grid-template-columns:minmax(310px,.85fr) minmax(0,1.45fr);gap:14px;margin-top:14px}.panel{padding:17px 19px}.health-panel,.recent-panel{min-height:260px}.panel-heading{display:flex;align-items:flex-start;justify-content:space-between}.panel-heading h2{margin:0;font-size:14px;font-weight:600}.service-row{height:39px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #f0f2f3;font-size:12px}.service-row:first-of-type{margin-top:12px}.service-row>span{display:flex;align-items:center;gap:8px}.service-dot{width:7px;height:7px;border-radius:50%;background:#aab3b9}.service-dot.up{background:#36a277}.service-dot.down{background:#d35d55}.health-meta{display:grid;grid-template-columns:1fr auto;gap:9px;margin-top:12px;color:#83909a;font-size:11px}.health-meta strong{color:#36444f;font-size:11px;font-weight:600}.checked-at{margin-top:11px;color:#a0a9b0;font-size:10px}.task-list{margin-top:10px}.task-row{display:flex;align-items:center;gap:10px;min-height:48px;border-top:1px solid #f0f2f3}.task-symbol{display:grid;place-items:center;width:27px;height:27px;border-radius:5px;background:#eef4f3;color:#327d71}.task-info{flex:1;min-width:0}.task-info strong,.task-info small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.task-info strong{font-size:11px;font-weight:550}.task-info small{margin-top:4px;color:#9aa4ac;font-size:10px}.task-panel{margin-top:14px}.task-heading{align-items:center}.task-controls{display:flex;align-items:center;gap:9px;color:#7f8b94;font-size:11px}.task-controls .el-input-number{width:126px}.action-list{display:flex;gap:9px;margin-top:17px;padding-top:15px;border-top:1px solid #edf0f2}.action-list .el-button{margin:0}.task-alert{margin-top:14px}footer{display:flex;justify-content:space-between;padding:20px 2px 3px;color:#9aa4ab;font-size:10px}
@media(max-width:1100px){.main-content{padding-right:22px;padding-left:22px}.stat-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.task-heading{align-items:flex-start;gap:14px;flex-direction:column}.task-controls{flex-wrap:wrap}.action-list{flex-wrap:wrap}}
</style>
