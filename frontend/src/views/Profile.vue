<template>
  <div class="profile-page">
    <!-- 用户信息区 -->
    <div class="user-info">
      <el-avatar :size="80" class="user-avatar">
        {{ userStore.username?.charAt(0)?.toUpperCase() }}
      </el-avatar>
      <div class="user-detail">
        <div class="user-top-row">
          <span class="user-name">{{ userStore.username }}</span>
          <el-button type="primary" round size="small" @click="$router.push('/settings')">
            编辑个人信息
          </el-button>
        </div>
        <div class="user-time">📅 注册时间：{{ formattedCreateTime }}</div>
        <div class="user-prefs" v-if="prefList.length > 0">
          <span class="pref-label">偏好类型：</span>
          <el-tag
            v-for="p in prefList"
            :key="p"
            size="small"
            round
            class="pref-tag"
          >{{ p }}</el-tag>
        </div>
      </div>
    </div>

    <!-- 统计数据行 -->
    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-icon">⭐</span>
        <div class="stat-text">
          <span class="stat-num">{{ ratings.length }}</span>
          <span class="stat-label">已评分</span>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-icon">❤️</span>
        <div class="stat-text">
          <span class="stat-num">{{ favorites.length }}</span>
          <span class="stat-label">已收藏</span>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-icon">📊</span>
        <div class="stat-text">
          <span class="stat-num">{{ avgScore }}</span>
          <span class="stat-label">平均评分</span>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-icon">👍</span>
        <div class="stat-text">
          <span class="stat-num">{{ totalLikes }}</span>
          <span class="stat-label">已获赞</span>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="我的评分" name="ratings">
        <div v-loading="ratingsLoading">
          <div v-if="ratings.length > 0" class="rating-list">
            <div
              v-for="item in ratings"
              :key="item.ratingId"
              class="rating-item"
            >
              <img
                :src="item.posterUrl || defaultPoster"
                class="rating-poster"
                @error="onImgError"
              />
              <div class="rating-info">
                <router-link :to="`/movie/${item.movieId}`" class="rating-title">
                  {{ item.movieTitle }}
                </router-link>
                <el-rate :model-value="item.score" disabled size="small" class="rating-stars" />
              </div>
              <div class="rating-time">{{ formatTime(item.createTime) }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无评分记录" :image-size="80" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的收藏" name="favorites">
        <div v-loading="favoritesLoading">
          <el-row :gutter="20" v-if="favorites.length > 0">
            <el-col
              v-for="fav in favorites"
              :key="fav.movieId"
              :xs="12"
              :sm="8"
              :md="6"
              :lg="4"
            >
              <div class="fav-card-wrapper">
                <MovieCard :movie="{ ...fav, id: fav.movieId }" />
                <el-button
                  type="danger"
                  size="small"
                  round
                  class="unfav-btn"
                  @click.stop="handleUnfavorite(fav.movieId)"
                >取消收藏</el-button>
              </div>
            </el-col>
          </el-row>
          <el-empty v-else description="暂无收藏" :image-size="80" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的评论" name="comments">
        <div v-loading="commentsLoading">
          <div v-if="myComments.length > 0" class="comment-list">
            <div
              v-for="item in myComments"
              :key="item.commentId"
              class="comment-item"
            >
              <img
                :src="item.posterUrl || defaultPoster"
                class="comment-poster"
                @error="onImgError"
              />
              <div class="comment-info">
                <router-link :to="`/movie/${item.movieId}`" class="comment-movie-title">
                  {{ item.movieTitle }}
                </router-link>
                <div class="comment-content-text">{{ item.content }}</div>
                <div class="comment-meta">
                  <span class="comment-like-count">👍 {{ item.likeCount }}</span>
                  <span class="comment-time">{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评论" :image-size="80" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyRatings, getMyFavorites } from '../api/user'
import { removeFavorite } from '../api/favorite'
import { getTotalLikes, getMyComments } from '../api/comment'
import { useUserStore } from '../store/user'
import MovieCard from '../components/MovieCard.vue'

const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('ratings')
const ratings = ref<any[]>([])
const favorites = ref<any[]>([])
const myComments = ref<any[]>([])
const totalLikes = ref(0)
const ratingsLoading = ref(false)
const favoritesLoading = ref(false)
const commentsLoading = ref(false)

const defaultPoster = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDgiIGhlaWdodD0iNjgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjY4IiBmaWxsPSIjZjBmMGYwIi8+PC9zdmc+'

const prefList = computed(() => {
  if (!userStore.preferences) return []
  return userStore.preferences.split(',').map((s: string) => s.trim()).filter(Boolean)
})

const formattedCreateTime = computed(() => {
  if (!userStore.createTime) return '—'
  return formatTime(userStore.createTime)
})

const avgScore = computed(() => {
  if (ratings.value.length === 0) return '—'
  const sum = ratings.value.reduce((acc: number, r: any) => acc + (r.score || 0), 0)
  return (sum / ratings.value.length).toFixed(1)
})

onMounted(async () => {
  if (!userStore.username) {
    await userStore.fetchUserInfo()
  }
  loadRatings()
  loadFavorites()
  loadMyComments()
})

watch(() => route.path, () => {
  if (route.path === '/favorites') {
    activeTab.value = 'favorites'
  }
})

function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 10)
}

async function loadRatings() {
  ratingsLoading.value = true
  try {
    const res: any = await getMyRatings()
    if (res.code === 200) {
      ratings.value = res.data || []
    }
    // 加载获赞数
    const likesRes: any = await getTotalLikes()
    if (likesRes.code === 200) {
      totalLikes.value = likesRes.data || 0
    }
  } finally {
    ratingsLoading.value = false
  }
}

async function loadFavorites() {
  favoritesLoading.value = true
  try {
    const res: any = await getMyFavorites()
    if (res.code === 200) {
      favorites.value = res.data || []
    }
  } finally {
    favoritesLoading.value = false
  }
}

async function loadMyComments() {
  commentsLoading.value = true
  try {
    const res: any = await getMyComments()
    if (res.code === 200) {
      myComments.value = res.data || []
    }
  } finally {
    commentsLoading.value = false
  }
}

async function handleUnfavorite(movieId: number) {
  try {
    await ElMessageBox.confirm('确定取消收藏该电影吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeFavorite(movieId)
    ElMessage.success('已取消收藏')
    favorites.value = favorites.value.filter(f => f.movieId !== movieId)
  } catch {
    // cancelled
  }
}

function onImgError(e: Event) {
  ;(e.target as HTMLImageElement).src = defaultPoster
}
</script>

<style scoped>
.profile-page {
  max-width: 1000px;
  margin: 0 auto;
}

/* 用户信息区 */
.user-info {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.user-avatar {
  background: #5B8DEF;
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-top-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.user-name {
  font-size: 20px;
  font-weight: 700;
  color: #2C3E50;
}

.user-time {
  font-size: 13px;
  color: #8896A4;
  margin-bottom: 10px;
}

.user-prefs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.pref-label {
  font-size: 13px;
  color: #8896A4;
}

.pref-tag {
  background: #ecf2fe;
  border-color: #ecf2fe;
  color: #5B8DEF;
}

/* 统计数据行 */
.stats-row {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 20px 0;
  margin-bottom: 8px;
  border-top: 1px solid #eef2f7;
  border-bottom: 1px solid #eef2f7;
}

.stat-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.stat-icon {
  font-size: 20px;
}

.stat-text {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #5B8DEF;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #8896A4;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #eef2f7;
}

/* Tab 切换 */
.profile-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: #8896A4;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #2C3E50;
  font-weight: 600;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background-color: #5B8DEF;
  height: 3px;
  border-radius: 2px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #eef2f7;
}

/* 评分列表 */
.rating-list {
  display: flex;
  flex-direction: column;
}

.rating-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.rating-item:hover {
  background: #f8faff;
}

.rating-item + .rating-item {
  border-top: 1px solid #f5f7fa;
}

.rating-poster {
  width: 48px;
  height: 68px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.rating-info {
  flex: 1;
  min-width: 0;
}

.rating-title {
  font-size: 14px;
  font-weight: 500;
  color: #2C3E50;
  text-decoration: none;
  display: block;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rating-title:hover {
  color: #5B8DEF;
}

.rating-stars {
  --el-rate-icon-size: 14px;
}

.rating-time {
  font-size: 13px;
  color: #8896A4;
  flex-shrink: 0;
  white-space: nowrap;
}

/* 收藏卡片 */
.fav-card-wrapper {
  position: relative;
  margin-bottom: 20px;
}

.unfav-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.2s;
}

.fav-card-wrapper:hover .unfav-btn {
  opacity: 1;
}

/* 我的评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.comment-item:hover {
  background: #f8faff;
}

.comment-item + .comment-item {
  border-top: 1px solid #f5f7fa;
}

.comment-poster {
  width: 48px;
  height: 68px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.comment-info {
  flex: 1;
  min-width: 0;
}

.comment-movie-title {
  font-size: 14px;
  font-weight: 500;
  color: #2C3E50;
  text-decoration: none;
  display: block;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.comment-movie-title:hover {
  color: #5B8DEF;
}

.comment-content-text {
  font-size: 13px;
  color: #5a6a7e;
  line-height: 1.6;
  margin-bottom: 8px;
  word-break: break-all;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #8896A4;
}

.comment-like-count {
  color: #5B8DEF;
  font-weight: 500;
}
</style>
