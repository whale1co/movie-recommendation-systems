<template>
  <el-card
    class="movie-card"
    :body-style="{ padding: '0' }"
    shadow="hover"
    @click="goDetail"
  >
    <div class="poster-wrapper">
      <img
        :src="posterSrc"
        :alt="movie.title"
        class="poster-img"
        @error="onImgError"
      />
      <div class="poster-rating" v-if="movie.doubanRating || movie.avgRating">
        {{ (movie.doubanRating || movie.avgRating || 0).toFixed(1) }}
      </div>
    </div>
    <div class="card-info">
      <div class="card-genres" v-if="genreList.length > 0">
        <span class="genre-tag" v-for="g in genreList.slice(0, 3)" :key="g">{{ g }}</span>
      </div>
      <h3 class="card-title" :title="movie.title">{{ movie.title }}</h3>
      <p class="card-summary" v-if="showSummary && movie.summary">
        {{ movie.summary.slice(0, 40) }}{{ movie.summary.length > 40 ? '...' : '' }}
      </p>
      <div class="card-bottom">
        <span class="card-year">
          {{ movie.releaseDate ? new Date(movie.releaseDate).getFullYear() : (movie.releaseYear || '') }}
        </span>
        <el-rate
          :model-value="(movie.doubanRating || movie.avgRating || 0) / 2"
          disabled
          size="small"
        />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { resolvePosterUrl } from '../utils/poster'

export interface MovieItem {
  id: number
  title: string
  posterUrl: string
  avgRating: number
  doubanRating: number
  releaseDate: string
  genre?: string
  releaseYear?: string
  summary?: string
}

const props = withDefaults(defineProps<{
  movie: MovieItem
  showSummary?: boolean
}>(), {
  showSummary: false
})

const router = useRouter()

const genreList = computed(() => {
  if (!props.movie.genre) return []
  return props.movie.genre.split(',').map((s: string) => s.trim()).filter(Boolean)
})

const posterSrc = computed(() => resolvePosterUrl(props.movie) || defaultPoster)

const defaultPoster = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjMwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjY2NjIiBmb250LXNpemU9IjE0Ij7ml6DnvKnnlaSjmWltZzwvdGV4dD48L3N2Zz4='

function onImgError(e: Event) {
  ;(e.target as HTMLImageElement).src = defaultPoster
}

function goDetail() {
  router.push(`/movie/${props.movie.id}`)
}
</script>

<style scoped>
.movie-card {
  cursor: pointer;
  transition: all 0.3s ease;
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
}

.movie-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 24px rgba(91, 141, 239, 0.15);
}

.poster-wrapper {
  width: 100%;
  height: 300px;
  overflow: hidden;
  background: #f0f2f5;
  position: relative;
}

.poster-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.movie-card:hover .poster-img {
  transform: scale(1.05);
}

.poster-rating {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(91, 141, 239, 0.9);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}

.card-info {
  padding: 12px 14px 14px;
}

.card-genres {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.genre-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  background: #ecf2fe;
  color: #5B8DEF;
  white-space: nowrap;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #2C3E50;
}

.card-summary {
  font-size: 12px;
  color: #8896A4;
  margin: 0 0 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-year {
  font-size: 12px;
  color: #8896A4;
}
</style>
