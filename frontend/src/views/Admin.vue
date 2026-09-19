<template>
  <div class="admin-dashboard">
    <header class="top-header">
      <div class="brand"><span class="brand-circle">S</span><strong>Dashboard</strong><el-icon><Monitor /></el-icon><el-icon class="muted-icon"><QuestionFilled /></el-icon></div>
      <div class="header-actions"><span class="date-label"><el-icon><Calendar /></el-icon>{{ today }}</span><el-button circle text title="检查服务" :loading="loadingAction === 'health'" @click="handleHealth"><el-icon><Refresh /></el-icon></el-button><el-avatar class="admin-avatar">A</el-avatar></div>
    </header>

    <div class="title-row"><div><span class="section-kicker">OVERVIEW</span><h1>系统总览</h1><p>查看数据维护进展，管理电影推荐系统的核心资源。</p></div><div class="title-actions"><el-tag effect="plain"><i></i>管理员模式</el-tag><el-button circle plain title="刷新状态" :loading="loadingAction === 'health'" @click="handleHealth"><el-icon><Refresh /></el-icon></el-button></div></div>

    <section class="summary-grid">
      <div class="white-panel summary-list-panel">
        <div class="panel-title"><div><h2>Summary</h2><span>平台真实数据</span></div><el-icon><MoreFilled /></el-icon></div>
        <div v-for="item in summaryItems" :key="item.label" class="summary-item" :class="item.color"><span class="summary-bullet"></span><b>{{ item.label }}</b><strong>{{ item.value }}</strong></div>
      </div>

      <div class="white-panel health-panel">
        <div class="panel-title"><div><h2>服务可用性</h2><span>管理员接口状态</span></div><el-icon><MoreFilled /></el-icon></div>
        <div class="gauge-wrap"><div class="gauge" :class="{ online: health.status === 'OK' }"><div><strong>{{ health.status === 'OK' ? '100%' : '--' }}</strong><span>API 状态</span></div></div><div class="gauge-copy"><strong>{{ health.status === 'OK' ? '服务运行正常' : '等待检查' }}</strong><p>{{ health.timestamp ? `最近检查 ${formatTime(health.timestamp)}` : '点击右上角检查服务' }}</p><span><i></i> 后端连接正常</span></div></div>
        <div class="gauge-footer"><span>Spring Boot</span><span>端口 8080</span><span>JWT 已启用</span></div>
      </div>

      <div class="white-panel recent-panel">
        <div class="panel-title"><div><h2>最近任务</h2><span>数据维护执行记录</span></div><el-icon><MoreFilled /></el-icon></div>
        <div class="recent-table"><div v-for="task in recentTasks" :key="task.label" class="recent-row"><span class="task-avatar" :class="task.color"><el-icon><component :is="task.icon" /></el-icon></span><span class="recent-name">{{ task.label }}<small>{{ task.detail }}</small></span><el-tag size="small" :type="task.done ? 'success' : 'info'" effect="plain">{{ task.done ? '已完成' : '待执行' }}</el-tag></div></div>
      </div>
    </section>

    <section class="metric-strip">
      <div class="mini-metric"><span>当前采集页数</span><strong>{{ pages }}</strong><small>页</small><el-icon><Refresh /></el-icon></div>
      <div class="mini-metric"><span>最近返回指标</span><strong>{{ stats ? Object.keys(stats).length : '--' }}</strong><small>{{ stats ? '项' : '等待任务' }}</small><el-icon><DataAnalysis /></el-icon></div>
      <div class="mini-metric"><span>数据源</span><strong>豆瓣</strong><small>电影数据</small><el-icon><Connection /></el-icon></div>
      <div class="mini-metric dark-metric"><span>海报存储</span><strong>LOCAL</strong><small>服务器本地</small><el-icon><Picture /></el-icon></div>
    </section>

    <div v-if="loadingAction" class="running-bar"><el-icon class="spin"><Loading /></el-icon><span>正在执行 {{ taskLabels[loadingAction] || '后台任务' }}</span><el-progress :percentage="50" :show-text="false" status="success" /></div>
    <el-alert v-if="lastMessage" :title="lastMessage" :type="lastMessageType" show-icon closable class="result-alert" @close="lastMessage = ''" />

    <section class="charts-grid">
      <div class="white-panel chart-panel line-panel">
        <div class="panel-title"><div><h2>任务执行概览</h2><span>{{ lastTaskLabel || '执行任务后显示真实统计数据' }}</span></div><el-icon><MoreFilled /></el-icon></div>
        <div class="chart-legend"><span><i class="legend-blue"></i>任务返回值</span><span><i class="legend-pale"></i>执行基线</span></div>
        <div class="line-chart"><svg viewBox="0 0 600 170" preserveAspectRatio="none" aria-hidden="true"><line v-for="y in [20,60,100,140]" :key="y" x1="0" :y1="y" x2="600" :y2="y" class="grid-line" /><polyline :points="linePoints" class="chart-line" :class="{ empty: !hasChartData }" /><circle v-for="point in chartDots" :key="point.x" :cx="point.x" :cy="point.y" r="4" class="chart-dot" :class="{ empty: !hasChartData }" /></svg><div class="chart-labels"><span v-for="label in chartLabels" :key="label">{{ label }}</span></div></div>
      </div>

      <div class="white-panel chart-panel bar-panel">
        <div class="panel-title"><div><h2>数据对比</h2><span>{{ stats ? '最近一次任务返回' : '当前数据库概览' }}</span></div><el-icon><MoreFilled /></el-icon></div>
        <div v-if="barRows.length" class="bar-chart"><div v-for="item in barRows" :key="item.key" class="bar-column"><strong>{{ item.value }}</strong><div class="bar-track"><i :style="{ height: `${item.percent}%` }"></i></div><span>{{ item.key }}</span></div></div>
        <div v-else class="empty-bar-chart"><div class="empty-bars"><i></i><i></i><i></i><i></i><i></i></div><span>等待后端数据</span></div>
      </div>

      <div class="white-panel action-panel">
        <div class="panel-title"><div><h2>快速操作</h2><span>常用维护任务</span></div><el-icon><MoreFilled /></el-icon></div>
        <button class="action-button blue-action" :disabled="!!loadingAction" @click="handleTop250"><span><el-icon><VideoPlay /></el-icon></span><b>采集 Top 250<small>默认任务</small></b><el-icon><ArrowRight /></el-icon></button>
        <button class="action-button purple-action" :disabled="!!loadingAction" @click="handlePosters"><span><el-icon><Download /></el-icon></span><b>下载远程海报<small>保存到本地</small></b><el-icon><ArrowRight /></el-icon></button>
        <button class="action-button green-action" :disabled="!!loadingAction" @click="handleCsv"><span><el-icon><Upload /></el-icon></span><b>导入 CSV 数据<small>电影与评分</small></b><el-icon><ArrowRight /></el-icon></button>
      </div>
    </section>

    <section class="white-panel task-panel">
      <div class="panel-title task-title"><div><h2>数据任务中心</h2><span>自定义电影采集范围，任务结果会同步到上方图表。</span></div><div class="task-control"><span>采集页数</span><el-input-number v-model="pages" :min="1" :max="500" controls-position="right" /><el-button type="primary" :loading="loadingAction === 'crawl'" @click="handleCrawl"><el-icon><Refresh /></el-icon>开始采集</el-button></div></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, CircleCheckFilled, Connection, DataAnalysis, Download, Loading, Monitor, MoreFilled, Picture, QuestionFilled, Refresh, TrendCharts, Upload, VideoPlay } from '@element-plus/icons-vue'
import { crawlMovies, crawlTop250, fetchPosters, getAdminHealth, importCsv } from '../api/admin'

const pages = ref(13); const loadingAction = ref(''); const lastMessage = ref(''); const lastMessageType = ref<'success' | 'error'>('success'); const lastTaskLabel = ref(''); const stats = ref<Record<string, number> | null>(null); const health = ref<{ status?: string; timestamp?: number; movieCount?: number; userCount?: number; ratingCount?: number }>({})
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
const taskLabels: Record<string, string> = { crawl: '豆瓣电影采集', top250: '豆瓣 Top 250 采集', posters: '远程海报下载', csv: 'CSV 数据导入', health: '服务健康检查' }
const summaryItems = computed(() => [{ label: '电影总数', value: health.value.movieCount ?? '--', color: 'blue' }, { label: '用户总数', value: health.value.userCount ?? '--', color: 'lilac' }, { label: '评分记录', value: health.value.ratingCount ?? '--', color: 'pink' }, { label: 'API 服务', value: health.value.status === 'OK' ? 'ONLINE' : 'OFFLINE', color: 'yellow' }])
const recentTasks = computed(() => [{ label: '豆瓣电影采集', detail: lastTaskLabel.value === '豆瓣电影采集' ? '最近刚刚完成' : '按页获取电影数据', color: 'blue', icon: Refresh, done: lastTaskLabel.value === '豆瓣电影采集' }, { label: '海报本地化', detail: lastTaskLabel.value === '远程海报下载' ? '远程资源已处理' : '等待执行下载', color: 'purple', icon: Picture, done: lastTaskLabel.value === '远程海报下载' }, { label: 'CSV 数据导入', detail: '电影与评分数据', color: 'green', icon: Upload, done: lastTaskLabel.value === 'CSV 数据导入' }])
const barRows = computed(() => { const entries = stats.value ? Object.entries(stats.value).slice(0, 5) : [['电影', health.value.movieCount ?? 0], ['用户', health.value.userCount ?? 0], ['评分', health.value.ratingCount ?? 0]] as [string, number][]; const max = Math.max(...entries.map(([, value]) => value), 1); return entries.filter(([, value]) => value > 0).map(([key, value]) => ({ key, value, percent: Math.max(8, Math.round((value / max) * 100)) })) })
const chartValues = computed(() => stats.value ? Object.values(stats.value).slice(0, 6) : [health.value.movieCount ?? 0, health.value.userCount ?? 0, health.value.ratingCount ?? 0])
const hasChartData = computed(() => chartValues.value.some(value => value > 0))
const chartDots = computed(() => { const max = Math.max(...chartValues.value, 1); const step = chartValues.value.length > 1 ? 540 / (chartValues.value.length - 1) : 0; return chartValues.value.map((value, index) => ({ x: chartValues.value.length > 1 ? 30 + index * step : 300, y: 145 - (value / max) * 110 })) })
const linePoints = computed(() => chartDots.value.map(point => `${point.x},${point.y}`).join(' '))
const chartLabels = computed(() => stats.value ? Object.keys(stats.value).slice(0, 6) : ['电影', '用户', '评分'])

onMounted(handleHealth)
async function runTask(action: string, task: () => Promise<any>, successText: string) { loadingAction.value = action; lastMessage.value = ''; try { const res: any = await task(); if (res.code !== 200) throw new Error(res.message || `${successText}失败`); stats.value = res.data || null; lastTaskLabel.value = taskLabels[action]; lastMessageType.value = 'success'; lastMessage.value = res.message || successText; ElMessage.success(lastMessage.value) } catch (error: any) { lastMessageType.value = 'error'; lastMessage.value = error.message || `${successText}失败`; ElMessage.error(lastMessage.value) } finally { loadingAction.value = '' } }
function handleCrawl() { return runTask('crawl', () => crawlMovies(pages.value), '电影采集完成') }
function handleTop250() { return runTask('top250', crawlTop250, '豆瓣 Top 250 采集完成') }
function handlePosters() { return runTask('posters', fetchPosters, '海报下载完成') }
function handleCsv() { return runTask('csv', importCsv, 'CSV 导入完成') }
async function handleHealth() { loadingAction.value = 'health'; try { const res: any = await getAdminHealth(); if (res.code === 200) health.value = res.data || {} } finally { loadingAction.value = '' } }
function formatTime(timestamp: number) { return new Date(timestamp).toLocaleString('zh-CN', { hour12: false }) }
</script>

<style scoped>
.admin-dashboard { max-width: 1240px; margin: 0 auto; color: #29364a; } .top-header, .brand, .header-actions, .header-date, .dashboard-nav, .nav-item, .title-row, .title-actions, .panel-title, .metric-top, .metric-foot, .gauge-wrap, .gauge-footer, .chart-legend, .action-button, .task-control { display: flex; align-items: center; }
.top-header { height: 53px; justify-content: space-between; } .brand { gap: 11px; min-width: 270px; } .brand-circle { display: grid; place-items: center; width: 34px; height: 34px; border: 1px solid #cad1da; border-radius: 50%; color: #536174; font-size: 18px; } .brand strong { font-size: 19px; margin-right: 8px; } .brand .el-icon { color: #abb5c1; } .muted-icon { font-size: 13px; }
.header-actions { gap: 8px; } .header-date { gap: 5px; color: #8c97a4; font-size: 11px; margin-right: 7px; } .admin-avatar { background: #283548; color: #fff; font-size: 12px; font-weight: 700; }
.dashboard-nav { height: 43px; gap: 26px; border-bottom: 1px solid #e9edf2; } .nav-item { position: relative; height: 43px; gap: 5px; color: #a0a9b3; font-size: 11px; } .nav-item.active { color: #35445a; font-weight: 600; } .nav-item.active::after { content: ''; position: absolute; left: 0; right: 0; bottom: -1px; height: 2px; background: #637be0; } .nav-caption { margin-left: auto; color: #b0b8c1; font-size: 9px; letter-spacing: 1.4px; }
.title-row { justify-content: space-between; align-items: flex-end; margin: 25px 0 16px; } .section-kicker { color: #687ee1; font-size: 10px; font-weight: 700; letter-spacing: 1.6px; } h1 { font-size: 25px; margin: 5px 0; } .title-row p { color: #8995a3; font-size: 12px; } .title-actions { gap: 10px; } .title-actions .el-tag { border-color: #dfe5f2; color: #6c7889; } .title-actions .el-tag i { display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: #45ae75; margin-right: 6px; }
.summary-grid { display: grid; grid-template-columns: 1fr 1.22fr 1fr; gap: 13px; } .white-panel { background: #fff; border: 1px solid #e8edf2; border-radius: 10px; box-shadow: 0 5px 16px rgba(48,65,90,.035); } .summary-list-panel, .health-panel, .recent-panel { min-height: 217px; padding: 17px 19px; } .panel-title { justify-content: space-between; align-items: flex-start; } .panel-title h2 { font-size: 15px; margin-bottom: 4px; } .panel-title span { color: #a0a9b3; font-size: 10px; } .panel-title > .el-icon { color: #b0b8c2; }
.summary-item { display: flex; align-items: center; height: 30px; gap: 8px; padding: 0 11px; margin: 8px 0 0; border-radius: 15px; font-size: 11px; } .summary-item b { flex: 1; font-weight: 500; } .summary-item strong { color: #6e7b8e; font-size: 11px; } .summary-item.blue { background: #e4edff; color: #4f70c7; } .summary-item.lilac { background: #ebe4ff; color: #765cc0; } .summary-item.pink { background: #f7dff2; color: #a55899; } .summary-item.yellow { background: #fff3bf; color: #a7903e; } .summary-bullet { width: 5px; height: 5px; border: 1px solid currentColor; border-radius: 50%; }
.gauge-wrap { justify-content: center; gap: 21px; height: 133px; } .gauge { display: grid; place-items: center; width: 111px; height: 111px; border-radius: 50%; background: conic-gradient(#e8ebf0 0 100%); position: relative; } .gauge.online { background: conic-gradient(#667fe0 0 100%); } .gauge::after { content: ''; position: absolute; inset: 9px; border-radius: 50%; background: #fff; } .gauge > div { position: relative; z-index: 1; text-align: center; } .gauge strong { display: block; font-size: 24px; } .gauge span { color: #a0a9b3; font-size: 9px; } .gauge-copy strong { display: block; font-size: 12px; } .gauge-copy p { max-width: 125px; color: #9aa4af; font-size: 10px; line-height: 1.5; margin: 7px 0; } .gauge-copy > span { color: #42a46f; font-size: 10px; } .gauge-copy > span i, .service-status i { display: inline-block; width: 5px; height: 5px; border-radius: 50%; background: #40b678; margin-right: 5px; } .gauge-footer { justify-content: space-between; border-top: 1px solid #eef1f4; padding-top: 10px; color: #a0a9b3; font-size: 9px; }
.recent-table { margin-top: 10px; } .recent-row { display: flex; align-items: center; gap: 8px; min-height: 42px; border-top: 1px solid #f0f2f5; } .task-avatar { display: grid; place-items: center; width: 25px; height: 25px; border-radius: 7px; } .task-avatar.blue { color: #5976d5; background: #eaf0ff; } .task-avatar.purple { color: #876dd0; background: #f0ebff; } .task-avatar.green { color: #3d9e6c; background: #e5f6ed; } .recent-name { flex: 1; color: #526176; font-size: 11px; } .recent-name small { display: block; color: #a1aab4; font-size: 9px; margin-top: 3px; } .recent-row .el-tag { border: 0; font-size: 9px; }
.metric-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 13px; margin-top: 13px; } .mini-metric { position: relative; min-height: 80px; padding: 14px 16px; background: #fff; border: 1px solid #e8edf2; border-radius: 10px; box-shadow: 0 5px 16px rgba(48,65,90,.03); } .mini-metric span, .mini-metric small { display: block; color: #9ba5af; font-size: 10px; } .mini-metric strong { display: inline-block; margin: 9px 5px 0 0; color: #3a485b; font-size: 20px; } .mini-metric small { display: inline; } .mini-metric > .el-icon { position: absolute; right: 15px; top: 17px; color: #a0aee1; font-size: 19px; } .dark-metric { background: #2d3543; border-color: #2d3543; } .dark-metric span, .dark-metric small { color: #aeb7c3; } .dark-metric strong { color: #fff; font-size: 16px; }
.running-bar { display: flex; align-items: center; gap: 9px; padding: 9px 12px; margin-top: 13px; background: #fff; border: 1px solid #e7ecf2; border-radius: 8px; color: #68778b; font-size: 11px; } .running-bar .el-progress { flex: 1; max-width: 240px; margin-left: auto; } .spin { color: #667fe0; animation: spin 1s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } } .result-alert { margin-top: 13px; }
.charts-grid { display: grid; grid-template-columns: 1.35fr 1fr .85fr; gap: 13px; margin-top: 13px; } .chart-panel, .action-panel { min-height: 242px; padding: 17px 19px; } .chart-legend { justify-content: flex-end; gap: 12px; color: #9ba5af; font-size: 9px; margin-top: -13px; } .chart-legend i { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 4px; } .legend-blue { background: #637be0; } .legend-pale { background: #dfe5f5; } .line-chart { height: 168px; margin-top: 10px; } .line-chart svg { width: 100%; height: 145px; overflow: visible; } .grid-line { stroke: #eff2f5; stroke-width: 1; } .chart-line { fill: none; stroke: #687fe0; stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; } .chart-line.empty { stroke: #dfe5f5; stroke-dasharray: 5 6; } .chart-dot { fill: #fff; stroke: #687fe0; stroke-width: 2; } .chart-dot.empty { stroke: #dfe5f5; } .chart-labels { display: flex; justify-content: space-between; color: #a2abb5; font-size: 9px; }
.bar-chart { height: 173px; display: flex; align-items: flex-end; justify-content: space-around; gap: 9px; padding: 20px 4px 0; border-bottom: 1px solid #eef1f4; } .bar-column { display: flex; align-items: center; flex-direction: column; height: 100%; justify-content: flex-end; min-width: 31px; } .bar-column strong { color: #657de0; font-size: 10px; margin-bottom: 4px; } .bar-track { display: flex; align-items: flex-end; width: 25px; height: 112px; border-radius: 6px 6px 1px 1px; background: #f0f2fa; overflow: hidden; } .bar-track i { display: block; width: 100%; background: #8999e7; border-radius: 6px 6px 1px 1px; } .bar-column span { max-width: 43px; overflow: hidden; color: #9ba5af; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; margin-top: 7px; } .empty-bar-chart { display: flex; align-items: center; flex-direction: column; justify-content: center; height: 178px; gap: 12px; color: #a1abb6; font-size: 10px; } .empty-bars { display: flex; align-items: flex-end; gap: 7px; height: 62px; } .empty-bars i { width: 20px; background: #eef1f6; border-radius: 5px 5px 0 0; } .empty-bars i:nth-child(1) { height: 27px; } .empty-bars i:nth-child(2) { height: 43px; } .empty-bars i:nth-child(3) { height: 34px; } .empty-bars i:nth-child(4) { height: 58px; } .empty-bars i:nth-child(5) { height: 39px; }
.action-button { width: 100%; gap: 9px; border: 0; border-radius: 8px; padding: 11px 9px; margin-top: 10px; text-align: left; cursor: pointer; color: #46546a; } .action-button:disabled { opacity: .55; cursor: wait; } .action-button > span { display: grid; place-items: center; width: 27px; height: 27px; border-radius: 7px; } .blue-action { background: #edf3ff; } .blue-action > span { color: #5675d7; background: #dce7ff; } .purple-action { background: #f2eeff; } .purple-action > span { color: #896fd2; background: #e4dbff; } .green-action { background: #ebf8f1; } .green-action > span { color: #399d6b; background: #d7f0e2; } .action-button b { flex: 1; font-size: 10px; } .action-button small { display: block; color: #9ca6b0; font-size: 9px; font-weight: 400; margin-top: 3px; } .action-button > .el-icon { color: #a3adb8; }
.task-panel { margin-top: 13px; padding: 17px 19px; } .task-title { align-items: center; } .task-control { gap: 8px; color: #8d98a5; font-size: 10px; }
@media (max-width: 1000px) { .summary-grid, .charts-grid { grid-template-columns: repeat(2, 1fr); } .recent-panel, .action-panel { grid-column: span 1; } .charts-grid .action-panel { grid-column: 1 / -1; } .action-button { display: inline-flex; width: calc(33.333% - 9px); margin-right: 8px; } }
@media (max-width: 680px) { .date-label, .dashboard-nav .nav-item:not(.active), .nav-caption { display: none; } .brand { min-width: auto; } .title-row, .task-title { align-items: flex-start; flex-direction: column; gap: 12px; } .metric-strip, .summary-grid, .charts-grid { grid-template-columns: 1fr; } .charts-grid .action-panel { grid-column: auto; } .action-button { width: 100%; margin-right: 0; } .gauge-wrap { justify-content: flex-start; } .task-control { flex-wrap: wrap; } }
</style>
