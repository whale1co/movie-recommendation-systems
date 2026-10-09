import request from '../utils/request'

export function getAdminHealth() {
  return request.get('/admin/health')
}

export function getSystemHealth() {
  return request.get('/admin/system/health')
}

export function submitCsvImport(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/tasks/import/csv', form)
}

export function submitPosterDownload() {
  return request.post('/admin/tasks/posters/download')
}

export function getPosterStatus() {
  return request.get('/admin/tasks/posters/status')
}

export interface AdminMovie {
  id: number
  doubanId?: string
  title: string
  director?: string
  actors?: string
  genre?: string
  releaseDate?: string
  runtime?: number
  summary?: string
  posterUrl?: string
  doubanRating?: number
  avgRating?: number
  ratingCount?: number
  status?: string
  createTime?: string
}

export function getAdminMovies(params: { current: number; size: number; keyword?: string; genre?: string; year?: number; status?: string }) {
  return request.get('/admin/movies', { params })
}

export function getAdminMovie(id: number) {
  return request.get(`/admin/movies/${id}`)
}

export function updateAdminMovie(id: number, data: Partial<AdminMovie>) {
  return request.patch(`/admin/movies/${id}`, data)
}

export function deleteAdminMovie(id: number) {
  return request.delete(`/admin/movies/${id}`)
}

export function getAdminUsers(params: { current: number; size: number; keyword?: string; status?: string; role?: string }) {
  return request.get('/admin/users', { params })
}

export function getAdminUser(id: number) {
  return request.get(`/admin/users/${id}`)
}

export function updateAdminUser(id: number, data: { password?: string; preferences?: string; role?: string; status?: string }) {
  return request.patch(`/admin/users/${id}`, data)
}

export function deleteAdminUser(id: number) {
  return request.delete(`/admin/users/${id}`)
}

export function getAuditEvents(params: { current: number; size: number }) {
  return request.get('/admin/audit-events', { params })
}

export interface AdminTask {
  id: number
  taskType: string
  status: string
  paramsJson?: string
  totalCount: number
  successCount: number
  failedCount: number
  errorMessage?: string
  createdBy: number
  startedAt?: string
  finishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export function getAdminTasks(params: { current: number; size: number; status?: string; taskType?: string }) {
  return request.get('/admin/tasks', { params })
}

export function getAdminTask(id: number) {
  return request.get(`/admin/tasks/${id}`)
}

export function getAdminTaskErrors(id: number) {
  return request.get(`/admin/tasks/${id}/errors`)
}

export function cancelAdminTask(id: number) {
  return request.post(`/admin/tasks/${id}/cancel`)
}

export function deleteAdminTask(id: number) {
  return request.delete(`/admin/tasks/${id}`)
}
