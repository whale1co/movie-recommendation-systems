<template>
  <div class="shell">
    <AdminSidebar />
    <main class="main">
      <header><div><span>管理控制台 / </span><b>用户管理</b></div><el-button circle plain title="刷新" :loading="loading" @click="load"><el-icon><Refresh /></el-icon></el-button></header>
      <section class="heading"><div><h1>用户管理</h1><p>管理账号状态、角色与平台访问权限。</p></div><el-tag type="info" effect="plain">{{ total }} 位用户</el-tag></section>
      <el-card shadow="never" class="filter"><el-form inline @submit.prevent="load"><el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="用户名或 ID" /></el-form-item><el-form-item label="状态"><el-select v-model="filters.status" style="width:120px"><el-option label="全部状态" value="" /><el-option label="正常" value="ACTIVE" /><el-option label="已禁用" value="DISABLED" /></el-select></el-form-item><el-form-item label="角色"><el-select v-model="filters.role" style="width:120px"><el-option label="全部角色" value="" /><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></el-form-item><el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button></el-form></el-card>
      <el-card shadow="never" class="table-card"><el-table v-loading="loading" :data="users" stripe><el-table-column prop="id" label="ID" width="80" /><el-table-column prop="username" label="用户名" min-width="180" /><el-table-column label="角色" width="120"><template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></template></el-table-column><el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">{{ statusName(row.status) }}</el-tag></template></el-table-column><el-table-column prop="preferences" label="偏好" min-width="180" show-overflow-tooltip /><el-table-column prop="createTime" label="注册时间" width="170" /><el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link :type="row.status === 'ACTIVE' ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 'ACTIVE' ? '禁用' : '启用' }}</el-button></template></el-table-column><template #empty><el-empty description="暂无用户记录" /></template></el-table><div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="changeSize" /></div></el-card>
    </main>
    <el-dialog v-model="dialog" title="编辑用户" width="500px"><el-form :model="form" label-width="90px"><el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item><el-form-item label="角色"><el-select v-model="form.role"><el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" /></el-select></el-form-item><el-form-item label="状态"><el-select v-model="form.status"><el-option label="正常" value="ACTIVE" /><el-option label="已禁用" value="DISABLED" /></el-select></el-form-item><el-form-item label="重置密码"><el-input v-model="form.password" type="password" show-password placeholder="留空表示不修改" /></el-form-item><el-form-item label="偏好"><el-input v-model="form.preferences" /></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存修改</el-button></template></el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import AdminSidebar from '../components/AdminSidebar.vue'
import { getAdminUsers, updateAdminUser } from '../api/admin'

const users = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const selectedId = ref<number>()
const filters = reactive({ keyword: '', status: '', role: '' })
const form = reactive<any>({})

async function load() { loading.value = true; try { const res: any = await getAdminUsers({ current: page.value, size: size.value, ...filters }); if (res.code !== 200) throw new Error(res.message); users.value = res.data?.records || []; total.value = res.data?.total || 0 } catch (e: any) { ElMessage.error(e.message || '用户列表加载失败') } finally { loading.value = false } }
function changeSize() { page.value = 1; load() }
function statusName(status: string) { return status === 'ACTIVE' ? '正常' : status === 'DISABLED' ? '已禁用' : '已注销' }
function openEdit(row: any) { selectedId.value = row.id; Object.assign(form, row, { password: '' }); dialog.value = true }
async function save() { if (!selectedId.value) return; saving.value = true; try { const data: any = { role: form.role, status: form.status, preferences: form.preferences }; if (form.password) data.password = form.password; const res: any = await updateAdminUser(selectedId.value, data); if (res.code !== 200) throw new Error(res.message); ElMessage.success('用户资料已更新'); dialog.value = false; await load() } catch (e: any) { ElMessage.error(e.message || '保存失败') } finally { saving.value = false } }
async function toggle(row: any) { const target = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'; try { await ElMessageBox.confirm(`确定${target === 'ACTIVE' ? '启用' : '禁用'}用户“${row.username}”吗？`, '二次确认', { type: target === 'ACTIVE' ? 'info' : 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); const res: any = await updateAdminUser(row.id, { status: target }); if (res.code !== 200) throw new Error(res.message); ElMessage.success('状态已更新'); await load() } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') } }
onMounted(load)
</script>

<style scoped>
.shell{min-height:100vh;background:#f4f6f8;color:#263442}.main{margin-left:220px;padding:0 32px 40px;max-width:1500px}header{height:61px;border-bottom:1px solid #e5e9ed;display:flex;justify-content:space-between;align-items:center;font-size:12px}header span{color:#89949d}.heading{display:flex;justify-content:space-between;align-items:center;padding:26px 0 20px}.heading h1{margin:0;font-size:23px}.heading p{color:#89949d;font-size:12px;margin:7px 0 0}.filter,.table-card{border:1px solid #e5e9ec;margin-bottom:14px}.filter :deep(.el-card__body){padding:17px 18px 4px}.table-card :deep(.el-card__body){padding:0}.filter .el-form-item{margin-bottom:13px}.pager{display:flex;justify-content:flex-end;padding:15px}.el-table{font-size:12px}
@media(max-width:900px){.main{margin-left:180px;padding:0 16px}}
</style>
