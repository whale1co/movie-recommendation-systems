import request from '../utils/request'

export function addComment(movieId: number, content: string) {
  return request.post('/comments', { movieId, content })
}

export function getComments(movieId: number) {
  return request.get('/comments', { params: { movieId } })
}

export function toggleLike(commentId: number) {
  return request.post(`/comments/${commentId}/like`)
}

export function getTotalLikes() {
  return request.get('/comments/total-likes')
}

export function getMyComments() {
  return request.get('/comments/mine')
}
