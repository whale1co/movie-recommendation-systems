const localPosterFallbackMap: Record<string, string> = {
  '老友记': '/api/posters/老友记.jpg',
  '请回答1988': '/api/posters/请回答1988.jpg',
  '大明王朝1566': '/api/posters/大明王朝1566.jpg',
  '毛骗': '/api/posters/毛骗.jpg',
  '我的天才女友': '/api/posters/30395843.jpg',
  '风味人间': '/api/posters/35014718.jpg',
  '新世纪福音战士剧场版：Air/真心为你': '/api/posters/1308892.jpg',
  '齐木楠雄的灾难': '/api/posters/26801048.jpg',
  '燃情克利夫兰': '/api/posters/25947054.jpg',
  '大宋提刑官': '/api/posters/2239292.jpg',
  '巴比伦柏林': '/api/posters/30206424.jpg'
}

export const defaultPosterUrl = '/api/posters/default-movie-poster.svg'

export function resolvePosterUrl(movie?: { title?: string; posterUrl?: string | null }) {
  if (movie?.posterUrl) {
    return movie.posterUrl
  }

  const title = movie?.title?.trim()
  if (title && localPosterFallbackMap[title]) {
    return localPosterFallbackMap[title]
  }

  return defaultPosterUrl
}
