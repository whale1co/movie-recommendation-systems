<template>
  <div class="advisor-page">
    <header class="page-header">
      <div class="header-icon" aria-hidden="true"><el-icon><MagicStick /></el-icon></div>
      <div>
        <h1>AI 选片顾问</h1>
        <p>说说你和谁看、想看什么类型，或对片长和评分的要求。</p>
      </div>
    </header>

    <section class="advisor-composer" aria-label="选片需求">
      <el-input
        v-model="question"
        type="textarea"
        :rows="4"
        resize="none"
        maxlength="1000"
        show-word-limit
        placeholder="例如：周末想和父母看一部两小时以内的温馨喜剧，不要恐怖片，评分高一些"
        :disabled="loading"
        @keydown.ctrl.enter.prevent="submitQuestion"
        @keydown.meta.enter.prevent="submitQuestion"
      />
      <div class="composer-footer">
        <div class="suggestions" aria-label="需求示例">
          <button
            v-for="item in suggestions"
            :key="item"
            type="button"
            :disabled="loading"
            @click="question = item"
          >{{ item }}</button>
        </div>
        <div class="composer-actions">
          <el-button v-if="loading" :icon="Close" @click="cancelRequest">取消</el-button>
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="loading"
            :disabled="!question.trim()"
            @click="submitQuestion"
          >获取推荐</el-button>
        </div>
      </div>
    </section>

    <div v-if="loading" class="loading-state" role="status" aria-live="polite">
      <el-icon class="is-loading"><Loading /></el-icon>
      <div>
        <strong>正在检索片库并整理推荐</strong>
        <span>这通常需要几秒，真实模型响应可能稍慢。</span>
      </div>
    </div>

    <section v-else-if="errorState" class="error-state" :class="`error-${errorState.kind}`" role="alert">
      <el-icon><WarningFilled /></el-icon>
      <div class="error-copy">
        <strong>{{ errorState.title }}</strong>
        <span>{{ errorState.message }}</span>
        <small v-if="errorState.requestId">请求 ID：{{ errorState.requestId }}</small>
      </div>
      <el-button :icon="Refresh" @click="retry">重试</el-button>
    </section>

    <template v-else-if="result">
      <section class="answer-section" aria-labelledby="advisor-answer-title">
        <div class="section-heading">
          <h2 id="advisor-answer-title">顾问建议</h2>
          <div class="answer-status">
            <el-tag v-if="result.degraded" type="warning" effect="plain">本地推荐</el-tag>
            <el-tag v-else-if="result.aiGenerated" type="success" effect="plain">AI 已生成</el-tag>
          </div>
        </div>
        <p class="answer-text">{{ result.answer }}</p>
        <p v-if="result.degraded" class="degraded-note">
          AI 服务本次未能完成回答，以下结果由本地片库检索生成，仍可正常查看和收藏。
        </p>
      </section>

      <section v-if="intentItems.length" class="intent-section" aria-labelledby="intent-title">
        <h2 id="intent-title">已理解的需求</h2>
        <div class="intent-list">
          <span v-for="item in intentItems" :key="item.label" class="intent-item">
            <small>{{ item.label }}</small>{{ item.value }}
          </span>
        </div>
      </section>

      <section class="recommendation-section" aria-labelledby="recommendation-title">
        <div class="section-heading recommendation-heading">
          <h2 id="recommendation-title">推荐电影</h2>
          <span>{{ result.recommendations.length }} 部</span>
        </div>

        <div v-if="result.recommendations.length" class="recommendation-list">
          <article v-for="movie in result.recommendations" :key="movie.movieId" class="recommendation-card">
            <button class="poster-button" type="button" @click="goDetail(movie)">
              <img :src="posterFor(movie)" :alt="`${movie.title}海报`" @error="onImgError" />
            </button>
            <div class="movie-content">
              <div class="movie-title-row">
                <div>
                  <h3><router-link :to="detailTarget(movie)">{{ movie.title }}</router-link></h3>
                  <div class="movie-meta">
                    <span v-if="movie.releaseDate"><el-icon><Calendar /></el-icon>{{ movie.releaseDate.slice(0, 4) }}</span>
                    <span v-if="movie.runtime"><el-icon><Clock /></el-icon>{{ movie.runtime }} 分钟</span>
                    <span v-if="movie.rating != null" class="movie-rating"><el-icon><StarFilled /></el-icon>{{ formatRating(movie.rating) }}</span>
                  </div>
                </div>
                <div class="movie-actions">
                  <el-tooltip :content="favoriteIds.has(movie.movieId) ? '取消收藏' : '收藏'">
                    <el-button
                      circle
                      :type="favoriteIds.has(movie.movieId) ? 'warning' : 'default'"
                      :loading="favoritePending.has(movie.movieId)"
                      :icon="favoriteIds.has(movie.movieId) ? StarFilled : Star"
                      :aria-label="favoriteIds.has(movie.movieId) ? '取消收藏' : '收藏'"
                      @click="toggleFavorite(movie.movieId)"
                    />
                  </el-tooltip>
                  <el-button :icon="EditPen" @click="goDetail(movie)">评分</el-button>
                </div>
              </div>

              <div v-if="genreList(movie).length" class="genre-list">
                <el-tag v-for="genre in genreList(movie)" :key="genre" size="small" effect="plain">{{ genre }}</el-tag>
              </div>
              <p class="movie-reason">{{ movie.reason }}</p>
              <div v-if="movie.matchedCriteria?.length" class="criteria-list">
                <span v-for="criterion in movie.matchedCriteria" :key="criterion">
                  <el-icon><CircleCheck /></el-icon>{{ criterion }}
                </span>
              </div>
              <el-button class="detail-link" link type="primary" :icon="View" @click="goDetail(movie)">查看电影详情</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前条件下没有找到合适影片，请放宽条件后再试" :image-size="96" />
      </section>
    </template>

    <section v-else class="empty-state">
      <div class="empty-mark"><el-icon><ChatDotRound /></el-icon></div>
      <h2>从一句话开始选片</h2>
      <p>顾问会从真实片库中筛选，推荐结果均可打开详情验证。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import {
  Calendar, ChatDotRound, CircleCheck, Clock, Close, EditPen, Loading,
  MagicStick, Promotion, Refresh, Star, StarFilled, View, WarningFilled
} from '@element-plus/icons-vue'
import { askAiAdvisor, type AiAdvisorResult, type AiMovieRecommendation } from '../api/ai'
import { addFavorite, getUserFavorites, removeFavorite } from '../api/favorite'
import { resolvePosterUrl } from '../utils/poster'

type ErrorKind = 'limit' | 'unavailable' | 'network' | 'request'

interface AdvisorError {
  kind: ErrorKind
  title: string
  message: string
  requestId?: string
}

const router = useRouter()
const question = ref('')
const lastQuestion = ref('')
const loading = ref(false)
const result = ref<AiAdvisorResult | null>(null)
const errorState = ref<AdvisorError | null>(null)
const favoriteIds = ref(new Set<number>())
const favoritePending = ref(new Set<number>())
let controller: AbortController | null = null

const suggestions = [
  '适合全家一起看的温馨喜剧',
  '两小时内的高分悬疑片',
  '节奏轻松的科幻电影'
]

const defaultPoster = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cub3JnLzIwMDAvc3ZnPjxyZWN0IHdpZHRoPSIyMDAiIGhlaWdodD0iMzAwIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGRvbWluYW50LWJhc2VsaW5lPSJtaWRkbGUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiM5NmEwYWEiIGZvbnQtc2l6ZT0iMTQiPuS4reaXoOa1t+WKoTwvdGV4dD48L3N2Zz4='

const intentItems = computed(() => {
  const intent = result.value?.intent
  if (!intent) return []
  const items: Array<{ label: string; value: string }> = []
  if (intent.genres?.length) items.push({ label: '类型', value: intent.genres.join('、') })
  if (intent.excludedGenres?.length) items.push({ label: '排除', value: intent.excludedGenres.join('、') })
  if (intent.maxRuntime) items.push({ label: '片长', value: `${intent.maxRuntime} 分钟以内` })
  if (intent.minRating != null) items.push({ label: '评分', value: `${intent.minRating} 分以上` })
  if (intent.minYear || intent.maxYear) {
    const year = intent.minYear && intent.maxYear
      ? `${intent.minYear} - ${intent.maxYear}`
      : intent.minYear ? `${intent.minYear} 年以后` : `${intent.maxYear} 年以前`
    items.push({ label: '年份', value: year })
  }
  if (intent.keywords?.length) items.push({ label: '主题', value: intent.keywords.join('、') })
  return items
})

async function submitQuestion() {
  const value = question.value.trim()
  if (!value || loading.value) return
  lastQuestion.value = value
  loading.value = true
  result.value = null
  errorState.value = null
  controller = new AbortController()
  try {
    const response = await askAiAdvisor(value, controller.signal)
    result.value = response.data
    await loadFavorites()
  } catch (error: any) {
    if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') return
    errorState.value = toAdvisorError(error)
  } finally {
    loading.value = false
    controller = null
  }
}

function cancelRequest() {
  controller?.abort()
}

function retry() {
  question.value = lastQuestion.value
  submitQuestion()
}

function toAdvisorError(error: any): AdvisorError {
  const status = error?.response?.status
  const requestId = error?.response?.data?.requestId || error?.response?.headers?.['x-request-id']
  if (status === 429) {
    return { kind: 'limit', title: '本次额度已用完', message: 'AI 请求较多，请稍后再试。你当前的输入已保留。', requestId }
  }
  if (status === 503) {
    return { kind: 'unavailable', title: 'AI 服务暂时不可用', message: '模型服务可能正在忙碌，请稍后重试。', requestId }
  }
  if (!error?.response) {
    return { kind: 'network', title: '无法连接服务', message: '请检查网络或后端服务状态后重试。' }
  }
  return {
    kind: 'request',
    title: '暂时无法完成推荐',
    message: error.response.data?.message || '请调整需求后再试。',
    requestId
  }
}

async function loadFavorites() {
  try {
    const response: any = await getUserFavorites()
    favoriteIds.value = new Set((response.data || []).map((item: any) => Number(item.movieId)))
  } catch {
    favoriteIds.value = new Set()
  }
}

async function toggleFavorite(movieId: number) {
  if (favoritePending.value.has(movieId)) return
  favoritePending.value = new Set(favoritePending.value).add(movieId)
  try {
    const next = new Set(favoriteIds.value)
    if (next.has(movieId)) {
      await removeFavorite(movieId)
      next.delete(movieId)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(movieId)
      next.add(movieId)
      ElMessage.success('收藏成功')
    }
    favoriteIds.value = next
  } finally {
    const pending = new Set(favoritePending.value)
    pending.delete(movieId)
    favoritePending.value = pending
  }
}

function detailTarget(movie: AiMovieRecommendation) {
  return movie.detailPath || `/movie/${movie.movieId}`
}

function goDetail(movie: AiMovieRecommendation) {
  router.push(detailTarget(movie))
}

function genreList(movie: AiMovieRecommendation) {
  return (movie.genre || '').split(',').map(item => item.trim()).filter(Boolean).slice(0, 4)
}

function posterFor(movie: AiMovieRecommendation) {
  return resolvePosterUrl({ posterUrl: movie.posterUrl }) || defaultPoster
}

function formatRating(rating: number) {
  return Number(rating).toFixed(1)
}

function onImgError(event: Event) {
  ;(event.target as HTMLImageElement).src = defaultPoster
}

onBeforeUnmount(() => controller?.abort())
</script>

<style scoped>
.advisor-page { width: min(1040px, 100%); margin: 0 auto; padding-bottom: 48px; color: #243447; }
.page-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.header-icon { display: grid; place-items: center; width: 46px; height: 46px; border-radius: 8px; background: #25364a; color: #fff; font-size: 23px; flex: 0 0 auto; }
.page-header h1 { margin: 0 0 4px; font-size: 24px; line-height: 1.25; letter-spacing: 0; }
.page-header p { margin: 0; color: #708093; font-size: 14px; }
.advisor-composer { padding: 18px; background: #fff; border: 1px solid #e5eaf0; border-radius: 8px; box-shadow: 0 8px 24px rgba(31, 45, 61, 0.06); }
.advisor-composer :deep(.el-textarea__inner) { padding: 14px 15px 28px; border-radius: 6px; box-shadow: 0 0 0 1px #dce3eb inset; line-height: 1.7; }
.composer-footer { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-top: 12px; }
.suggestions { display: flex; flex-wrap: wrap; gap: 8px; }
.suggestions button { min-height: 30px; padding: 5px 10px; border: 1px solid #dce3eb; border-radius: 6px; background: #f8fafc; color: #5f7083; font: inherit; font-size: 12px; cursor: pointer; }
.suggestions button:hover:not(:disabled) { border-color: #5b8def; color: #3f72d2; background: #f3f7ff; }
.suggestions button:disabled { cursor: not-allowed; opacity: .6; }
.composer-actions { display: flex; gap: 8px; flex: 0 0 auto; }
.loading-state, .error-state { display: flex; align-items: center; gap: 14px; min-height: 82px; margin-top: 20px; padding: 16px 18px; border-radius: 8px; border: 1px solid #dce3eb; background: #fff; }
.loading-state > .el-icon, .error-state > .el-icon { font-size: 24px; color: #5b8def; flex: 0 0 auto; }
.loading-state div, .error-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 4px; }
.loading-state span, .error-copy span { color: #708093; font-size: 13px; }
.error-copy small { color: #99a4b1; overflow-wrap: anywhere; }
.error-limit { border-color: #f3d59b; background: #fffbf2; }
.error-limit > .el-icon { color: #d99519; }
.error-unavailable, .error-network { border-color: #f0b8b8; background: #fff8f8; }
.error-unavailable > .el-icon, .error-network > .el-icon { color: #d94e4e; }
.answer-section, .intent-section, .recommendation-section { margin-top: 30px; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.answer-section h2, .intent-section h2, .recommendation-section h2 { margin: 0 0 12px; font-size: 18px; line-height: 1.4; letter-spacing: 0; }
.answer-text { margin: 0; padding-left: 14px; border-left: 3px solid #5b8def; color: #3f4f60; font-size: 15px; line-height: 1.85; white-space: pre-wrap; overflow-wrap: anywhere; }
.degraded-note { margin: 12px 0 0; padding: 10px 12px; border-radius: 6px; background: #fff7e8; color: #8a6012; font-size: 13px; line-height: 1.6; }
.intent-list { display: flex; flex-wrap: wrap; gap: 8px; }
.intent-item { display: inline-flex; align-items: center; min-height: 34px; padding: 6px 10px; border: 1px solid #dce3eb; border-radius: 6px; background: #fff; color: #43566a; font-size: 13px; }
.intent-item small { margin-right: 7px; color: #8592a2; font-size: 11px; }
.recommendation-heading > span { color: #8592a2; font-size: 13px; }
.recommendation-list { display: grid; gap: 14px; }
.recommendation-card { display: grid; grid-template-columns: 136px minmax(0, 1fr); min-height: 204px; overflow: hidden; border: 1px solid #e3e8ee; border-radius: 8px; background: #fff; transition: border-color .2s, box-shadow .2s; }
.recommendation-card:hover { border-color: #c7d6ed; box-shadow: 0 6px 20px rgba(31, 45, 61, .07); }
.poster-button { padding: 0; border: 0; background: #eef1f4; cursor: pointer; }
.poster-button img { display: block; width: 136px; height: 100%; min-height: 204px; object-fit: cover; }
.movie-content { min-width: 0; padding: 16px 18px 14px; }
.movie-title-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 14px; }
.movie-title-row h3 { margin: 0 0 7px; font-size: 17px; line-height: 1.35; letter-spacing: 0; }
.movie-title-row h3 a { color: #243447; text-decoration: none; }
.movie-title-row h3 a:hover { color: #3f72d2; }
.movie-meta { display: flex; flex-wrap: wrap; gap: 13px; color: #7b8998; font-size: 12px; }
.movie-meta span { display: inline-flex; align-items: center; gap: 4px; }
.movie-meta .movie-rating { color: #b9790c; font-weight: 600; }
.movie-actions { display: flex; gap: 8px; flex: 0 0 auto; }
.genre-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.movie-reason { margin: 12px 0 8px; color: #526375; line-height: 1.65; font-size: 14px; overflow-wrap: anywhere; }
.criteria-list { display: flex; flex-wrap: wrap; gap: 7px 14px; color: #4c7a5b; font-size: 12px; }
.criteria-list span { display: inline-flex; align-items: center; gap: 4px; }
.detail-link { margin-top: 8px; }
.empty-state { padding: 66px 20px 30px; text-align: center; color: #6f7e8e; }
.empty-mark { display: grid; place-items: center; width: 56px; height: 56px; margin: 0 auto 14px; border-radius: 8px; background: #eaf0f8; color: #4f6d91; font-size: 27px; }
.empty-state h2 { margin: 0 0 7px; color: #405164; font-size: 17px; letter-spacing: 0; }
.empty-state p { margin: 0; font-size: 13px; }
@media (max-width: 700px) {
  .page-header { align-items: flex-start; }
  .page-header h1 { font-size: 21px; }
  .composer-footer { align-items: stretch; flex-direction: column; }
  .composer-actions { justify-content: flex-end; }
  .error-state { align-items: flex-start; flex-wrap: wrap; }
  .error-state .el-button { margin-left: 38px; }
  .recommendation-card { grid-template-columns: 96px minmax(0, 1fr); }
  .poster-button img { width: 96px; min-height: 150px; }
  .movie-content { padding: 13px; }
  .movie-title-row { flex-direction: column; }
  .movie-actions { width: 100%; justify-content: flex-end; }
}
@media (max-width: 430px) {
  .advisor-composer { padding: 12px; }
  .suggestions button { width: 100%; text-align: left; }
  .recommendation-card { grid-template-columns: 80px minmax(0, 1fr); }
  .poster-button img { width: 80px; min-height: 124px; }
  .movie-meta { gap: 7px; }
}
</style>
