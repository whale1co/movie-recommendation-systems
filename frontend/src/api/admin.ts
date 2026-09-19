import request from '../utils/request'

export function getAdminHealth() {
  return request.get('/admin/health')
}

export function crawlTop250() {
  return request.post('/admin/crawl')
}

export function crawlMovies(pages: number) {
  return request.post('/admin/crawl-movies', null, { params: { pages } })
}

export function fetchPosters() {
  return request.post('/admin/fetch-posters')
}

export function importCsv() {
  return request.post('/admin/import-csv')
}
