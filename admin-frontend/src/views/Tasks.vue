<template>
  <div class="task-shell">
    <AdminSidebar />
    <main class="main">
      <header><div><span>管理控制台 / </span><b>数据任务</b></div><el-button circle plain title="刷新" :loading="loading" @click="load"><el-icon><Refresh /></el-icon></el-button></header>
      <section class="heading"><div><h1>数据任务</h1><p>异步执行 CSV 导入和海报下载，实时查看进度与失败原因。</p></div></section>

      <el-card shadow="never" class="create-card">
        <div class="create-title"><div><h2>CSV 与海报</h2><span>CSV 使用 UTF-8 编码，单个文件不超过 50 MB；海报仅下载远程图片。</span></div><el-button text title="刷新海报状态" :loading="posterLoading" @click="loadPosterStatus"><el-icon><Refresh /></el-icon></el-button></div>
        <div class="maintenance-row">
          <div class="maintenance-item"><b>导入电影 CSV</b><small>{{ csvFile?.name || '未选择文件' }}</small><input ref="csvInput" type="file" accept=".csv,text/csv" @change="chooseCsv" /><div><el-button type="primary" :loading="submitting === 'csv'" :disabled="Boolean(submitting) || !csvFile" @click="submitCsv"><el-icon><Upload /></el-icon>提交导入</el-button><el-button :disabled="Boolean(submitting)" @click="csvInput?.click()">选择文件</el-button></div></div>
          <div class="maintenance-item"><b>海报本地化</b><small>{{ posterSummary.localizedMovies || 0 }} 部已本地化 · {{ posterSummary.remoteMovies || 0 }} 部待下载 · {{ posterSummary.fileCount || 0 }} 个文件</small><div><el-button type="primary" :loading="submitting === 'posters'" :disabled="Boolean(submitting)" @click="submitPosters"><el-icon><Picture /></el-icon>下载远程海报</el-button></div></div>
        </div>
      </el-card>

      <el-card shadow="never" class="table-card">
        <div class="table-head"><div><h2>执行记录</h2><span>任务完成后可删除记录，运行中的任务可取消。</span></div><el-select v-model="status" clearable placeholder="全部状态" style="width: 130px" @change="load"><el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></div>
        <el-table v-loading="loading" :data="tasks" stripe>
          <el-table-column prop="id" label="编号" width="82" />
          <el-table-column label="类型" width="150"><template #default="{ row }">{{ taskType(row.taskType) }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
          <el-table-column label="进度" min-width="190"><template #default="{ row }"><div class="progress"><el-progress :percentage="progress(row)" :show-text="false" /><small>{{ row.successCount || 0 }} 成功 / {{ row.failedCount || 0 }} 失败</small></div></template></el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button><el-button v-if="row.status === 'PENDING' || row.status === 'RUNNING'" link type="warning" @click="cancel(row)">取消</el-button><el-button v-else link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
          <template #empty><el-empty description="暂无数据任务" /></template>
        </el-table>
        <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="changeSize" /></div>
      </el-card>
    </main>
    <el-dialog v-model="detailVisible" title="任务详情" width="560px"><template v-if="detail"><div class="detail-grid"><span>任务编号</span><b>{{ detail.id }}</b><span>参数</span><code>{{ detail.paramsJson || '-' }}</code><span>失败原因</span><p>{{ detail.errorMessage || '暂无失败原因' }}</p></div><el-divider /><h3>错误记录</h3><el-empty v-if="!errors.length" description="暂无错误记录" :image-size="50" /><div v-for="error in errors" :key="error.id" class="error-row"><b>{{ error.errorType || 'ERROR' }}</b><span>{{ error.errorMessage }}</span></div></template></el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Refresh, Upload } from '@element-plus/icons-vue'
import AdminSidebar from '../components/AdminSidebar.vue'
import { cancelAdminTask, deleteAdminTask, getAdminTask, getAdminTaskErrors, getAdminTasks, getPosterStatus, submitCsvImport, submitPosterDownload, type AdminTask } from '../api/admin'

interface TaskError { id: number; errorType?: string; errorMessage: string }
const loading = ref(false)
const submitting = ref('')
const tasks = ref<AdminTask[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const status = ref('')
const detailVisible = ref(false)
const detail = ref<AdminTask | null>(null)
const errors = ref<TaskError[]>([])
const csvInput = ref<HTMLInputElement | null>(null)
const csvFile = ref<File | null>(null)
const posterLoading = ref(false)
const posterSummary = ref<{ localizedMovies?: number; remoteMovies?: number; fileCount?: number; totalBytes?: number }>({})
const timer = ref<ReturnType<typeof setInterval> | null>(null)
const statuses = [{ value: 'PENDING', label: '等待执行' }, { value: 'RUNNING', label: '执行中' }, { value: 'SUCCESS', label: '已完成' }, { value: 'PARTIAL', label: '部分成功' }, { value: 'FAILED', label: '失败' }, { value: 'CANCELLED', label: '已取消' }]
const hasRunning = computed(() => tasks.value.some((task) => task.status === 'PENDING' || task.status === 'RUNNING'))

onMounted(() => { load(); loadPosterStatus(); timer.value = setInterval(() => { if (hasRunning.value) load(true); loadPosterStatus(true) }, 3000) })
onBeforeUnmount(() => { if (timer.value) clearInterval(timer.value) })

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const response: any = await getAdminTasks({ current: page.value, size: size.value, status: status.value || undefined })
    if (response.code !== 200) throw new Error(response.message || '任务加载失败')
    tasks.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error: any) { if (!silent) ElMessage.error(error.message || '任务加载失败') }
  finally { if (!silent) loading.value = false }
}
function chooseCsv(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0] || null
  if (file && !file.name.toLowerCase().endsWith('.csv')) { ElMessage.warning('请选择 .csv 文件'); csvFile.value = null; return }
  if (file && file.size > 50 * 1024 * 1024) { ElMessage.warning('CSV 文件不能超过 50 MB'); csvFile.value = null; return }
  csvFile.value = file
}
async function submitCsv() { if (csvFile.value) await submit('csv', () => submitCsvImport(csvFile.value!)); csvFile.value = null; if (csvInput.value) csvInput.value.value = '' }
async function submitPosters() { await submit('posters', submitPosterDownload); await loadPosterStatus() }
async function loadPosterStatus(silent = false) {
  if (!silent) posterLoading.value = true
  try { const response: any = await getPosterStatus(); if (response.code === 200) posterSummary.value = response.data || {} }
  catch (error: any) { if (!silent) ElMessage.error(error.message || '海报状态加载失败') }
  finally { if (!silent) posterLoading.value = false }
}
async function submit(kind: string, action: () => Promise<any>) {
  submitting.value = kind
  try { const response: any = await action(); if (![200, 202].includes(response.code)) throw new Error(response.message || '任务提交失败'); ElMessage.success('任务已提交，可在列表查看进度'); await load() }
  catch (error: any) { ElMessage.error(error.message || '任务提交失败') }
  finally { submitting.value = '' }
}
async function cancel(task: AdminTask) { try { await ElMessageBox.confirm(`确定取消任务 #${task.id} 吗？`, '取消任务', { type: 'warning' }); await cancelAdminTask(task.id); ElMessage.success('任务已取消'); await load() } catch { /* cancelled by user */ } }
async function remove(task: AdminTask) { try { await ElMessageBox.confirm(`确定删除任务 #${task.id} 吗？`, '删除记录', { type: 'warning' }); await deleteAdminTask(task.id); ElMessage.success('任务记录已删除'); await load() } catch { /* cancelled by user */ } }
async function showDetail(task: AdminTask) { detail.value = task; detailVisible.value = true; try { const response: any = await Promise.all([getAdminTask(task.id), getAdminTaskErrors(task.id)]); detail.value = response[0].data; errors.value = response[1].data || [] } catch { errors.value = [] } }
function changeSize() { page.value = 1; load() }
function progress(task: AdminTask) { const totalCount = task.totalCount || 0; if (!totalCount) return task.status === 'SUCCESS' ? 100 : 0; return Math.min(100, Math.round(((task.successCount || 0) + (task.failedCount || 0)) / totalCount * 100)) }
function taskType(type: string) { return type === 'CRAWL_TOP250' || type === 'CRAWL_PAGE' ? '历史任务' : type === 'CSV_IMPORT' ? 'CSV 导入' : type === 'POSTER_DOWNLOAD' ? '海报下载' : type }
function statusLabel(value: string) { return statuses.find((item) => item.value === value)?.label || value }
function statusType(value: string) { return value === 'SUCCESS' ? 'success' : value === 'FAILED' ? 'danger' : value === 'PARTIAL' ? 'warning' : value === 'CANCELLED' ? 'info' : 'primary' }
</script>

<style scoped>
.task-shell{min-height:100vh;background:#f4f6f8;color:#263442}.side{position:fixed;inset:0 auto 0 0;width:218px;background:#fff;border-right:1px solid #e5e9ed;padding:25px 15px 16px;display:flex;flex-direction:column}.brand{display:flex;align-items:center;gap:10px;padding:0 8px 30px}.brand>b{display:grid;place-items:center;width:36px;height:36px;border-radius:7px;background:#176b63;color:#fff}.brand small{display:block;color:#929da5;font-size:10px;margin-top:3px}.side nav{display:grid;gap:4px}.side nav a{padding:11px 12px;border-radius:5px;color:#67747e;text-decoration:none;font-size:13px}.side nav a.active{background:#eaf4f2;color:#176b63;font-weight:600}.profile{display:flex;align-items:center;justify-content:space-between;margin-top:auto;padding:14px 5px 0;border-top:1px solid #edf0f2;color:#56636d;font-size:12px}.main{margin-left:218px;padding:0 34px 30px;max-width:1450px}.main header{height:61px;border-bottom:1px solid #e6eaed;display:flex;align-items:center;justify-content:space-between;color:#87929b;font-size:12px}.main header b{color:#36434d;font-weight:500}.heading{display:flex;justify-content:space-between;align-items:center;padding:26px 0 19px}.heading h1{margin:0;font-size:23px}.heading p,.create-title span,.table-head span{color:#89949d;font-size:12px;margin:7px 0 0}.heading-actions{display:flex;gap:8px}.create-card,.table-card{border:1px solid #e5e9ec;border-radius:6px;margin-bottom:14px}.create-title,.table-head{display:flex;align-items:flex-start;justify-content:space-between}.create-title h2,.table-head h2{margin:0;font-size:14px}.form-row{display:flex;align-items:flex-end;gap:14px;flex-wrap:wrap;border-top:1px solid #edf0f2;margin-top:15px;padding-top:15px}.form-row label{display:grid;gap:6px;color:#77838c;font-size:11px}.form-row .el-input-number{width:120px}.form-row .el-checkbox{margin:0 5px 8px}.maintenance-row{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px;border-top:1px solid #edf0f2;margin-top:15px;padding-top:15px}.maintenance-item{display:grid;gap:8px;align-content:start}.maintenance-item b{font-size:12px}.maintenance-item small{color:#89949d;font-size:11px;min-height:16px}.maintenance-item input{display:none}.table-head{margin-bottom:16px}.progress{min-width:140px}.progress small{display:block;margin-top:4px;color:#9aa3aa;font-size:10px}.pager{display:flex;justify-content:flex-end;margin-top:16px}.detail-grid{display:grid;grid-template-columns:80px 1fr;gap:12px;color:#7f8b94;font-size:12px}.detail-grid b,.detail-grid code,.detail-grid p{color:#35434d;word-break:break-all;margin:0}.detail-grid code{font-family:monospace}.detail-grid p{line-height:1.6}.error-row{display:flex;gap:12px;padding:9px 0;border-bottom:1px solid #edf0f2;font-size:12px}.error-row b{color:#b65b53}.error-row span{color:#5f6a72}
@media(max-width:900px){.side{width:170px}.main{margin-left:170px;padding:0 18px}.heading{align-items:flex-start;gap:14px;flex-direction:column}.form-row{align-items:flex-start;flex-direction:column}.form-row label{width:100%}.form-row .el-input-number{width:100%}}
</style>
