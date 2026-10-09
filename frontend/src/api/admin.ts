import request from '../utils/request'

export function getAdminHealth() {
  return request.get('/admin/health')
}

export function fetchPosters() {
  return request.post('/admin/fetch-posters')
}

export function importCsv() {
  return request.post('/admin/import-csv')
}
