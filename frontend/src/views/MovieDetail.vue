<template>
  <div class="movie-detail" v-loading="loading">
    <template v-if="movie">
      <div class="detail-top">
        <div class="poster-section">
          <img
            :src="posterSrc"
            :alt="movie.title"
            class="poster-img"
            @error="onImgError"
          />
        </div>
        <div class="info-section">
          <h1 class="movie-title">{{ movie.title }}</h1>

          <div class="info-row">
            <span class="info-label">导演</span>
            <span class="info-value">{{ movie.director || '暂无' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">主演</span>
            <span class="info-value">{{ movie.actors || '暂无' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">类型</span>
            <span class="info-value">
              <el-tag
                v-for="g in genres"
                :key="g"
                size="small"
                class="genre-tag"
                type="primary"
                effect="plain"
              >{{ g }}</el-tag>
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">上映日期</span>
            <span class="info-value">{{ movie.releaseDate || '暂无' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">片长</span>
            <span class="info-value">{{ movie.runtime ? movie.runtime + ' 分钟' : '暂无' }}</span>
          </div>

          <div class="rating-section">
            <div class="rating-item">
              <span class="rating-label">豆瓣评分</span>
              <span class="rating-score">{{ movie.doubanRating || '-' }}</span>
            </div>
            <div class="rating-item">
              <span class="rating-label">站内均分</span>
              <span class="rating-score">{{ movie.avgRating || '-' }}</span>
              <span class="rating-count">({{ movie.ratingCount || 0 }}人评分)</span>
            </div>
          </div>

          <div class="action-section" v-if="userStore.token">
            <div class="user-rating">
              <span class="rating-label">我的评分</span>
              <el-rate
                v-model="myScore"
                :max="5"
                allow-half
                show-score
                score-template="{value}星"
                @change="handleRate"
              />
              <el-button
                v-if="myRatingId"
                type="danger"
                size="small"
                link
                @click="handleDeleteRating"
              >删除评分</el-button>
            </div>
            <div class="favorite-btn">
              <el-button
                :type="isFavorited ? 'warning' : 'primary'"
                round
                @click="handleFavorite"
              >
                {{ isFavorited ? '取消收藏' : '加入收藏' }}
              </el-button>
            </div>
          </div>
          <div class="action-section login-hint" v-else>
            <el-button type="primary" round @click="$router.push('/login')">登录后可评分和收藏</el-button>
          </div>
        </div>
      </div>

      <div class="summary-section">
        <h2>剧情简介</h2>
        <p class="summary-text">{{ movie.summary || '暂无简介' }}</p>
      </div>

      <!-- 评论区 -->
      <div class="comment-section">
        <div class="comment-header">
          <h2>评论区</h2>
          <span class="comment-count">共 {{ comments.length }} 条评论</span>
        </div>

        <!-- 发表评论 -->
        <div class="comment-input" v-if="userStore.token">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <div class="comment-input-action">
            <el-button type="primary" round @click="handleAddComment" :loading="commentSubmitting">
              发表评论
            </el-button>
          </div>
        </div>
        <div class="comment-login-hint" v-else>
          <el-button type="primary" round @click="$router.push('/login')">登录后可评论</el-button>
        </div>

        <!-- 评论列表 -->
        <div class="comment-list" v-loading="commentsLoading">
          <div v-if="comments.length > 0">
            <div
              v-for="item in comments"
              :key="item.commentId"
              class="comment-item"
            >
              <div class="comment-user">
                <el-avatar :size="32" class="comment-avatar">
                  {{ item.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <span class="comment-username">{{ item.username }}</span>
                <span class="comment-time">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="comment-content">{{ item.content }}</div>
              <div class="comment-actions">
                <span
                  class="like-btn"
                  :class="{ liked: item.liked }"
                  @click="handleLike(item)"
                >
                  {{ item.liked ? '👍' : '👍' }} {{ item.likeCount || 0 }}
                </span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评论，快来抢沙发" :image-size="80" />
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="电影不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMovieDetail } from '../api/movie'
import { addRating, updateRating, deleteRating, getUserRatings } from '../api/rating'
import { addFavorite, removeFavorite, getUserFavorites } from '../api/favorite'
import { addComment, getComments, toggleLike } from '../api/comment'
import { useUserStore } from '../store/user'
import { resolvePosterUrl } from '../utils/poster'

const route = useRoute()
const userStore = useUserStore()

const movie = ref<any>(null)
const loading = ref(false)
const myScore = ref(0)
const myRatingId = ref<number | null>(null)
const isFavorited = ref(false)

const comments = ref<any[]>([])
const commentsLoading = ref(false)
const commentContent = ref('')
const commentSubmitting = ref(false)

const defaultPoster = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjQ1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMzAwIiBoZWlnaHQ9IjQ1MCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjY2NjIiBmb250LXNpemU9IjE2Ij7ml6DnvKnnlaSjmWltZzwvdGV4dD48L3N2Zz4='

const genres = computed(() => {
  if (!movie.value?.genre) return []
  return movie.value.genre.split(',').map((g: string) => g.trim()).filter(Boolean)
})

const posterSrc = computed(() => resolvePosterUrl(movie.value) || defaultPoster)

onMounted(() => {
  loadMovie()
})

watch(() => route.params.id, () => {
  if (route.params.id) {
    loadMovie()
  }
})

async function loadMovie() {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res: any = await getMovieDetail(id)
    if (res.code === 200) {
      movie.value = res.data
      if (userStore.token) {
        await loadUserStatus()
      }
      loadComments()
    } else {
      movie.value = null
    }
  } catch {
    movie.value = null
  } finally {
    loading.value = false
  }
}

async function loadUserStatus() {
  const movieId = Number(route.params.id)
  try {
    const [ratingsRes, favoritesRes]: any[] = await Promise.all([
      getUserRatings(),
      getUserFavorites()
    ])

    if (ratingsRes.code === 200) {
      const found = (ratingsRes.data || []).find((r: any) => r.movieId === movieId)
      if (found) {
        myScore.value = found.score
        myRatingId.value = found.ratingId
      }
    }

    if (favoritesRes.code === 200) {
      isFavorited.value = (favoritesRes.data || []).some((f: any) => f.movieId === movieId)
    }
  } catch {
    // ignore
  }
}

async function loadComments() {
  const movieId = Number(route.params.id)
  if (!movieId) return
  commentsLoading.value = true
  try {
    const res: any = await getComments(movieId)
    if (res.code === 200) {
      comments.value = res.data || []
    }
  } finally {
    commentsLoading.value = false
  }
}

async function handleAddComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    const res: any = await addComment(movie.value.id, commentContent.value.trim())
    if (res.code === 200) {
      ElMessage.success('评论成功')
      commentContent.value = ''
      await loadComments()
    } else {
      ElMessage.error(res.message || '评论失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function handleLike(item: any) {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res: any = await toggleLike(item.commentId)
    if (res.code === 200) {
      item.liked = res.data.liked
      item.likeCount = res.data.likeCount
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleRate(val: number) {
  if (!val) return
  try {
    if (myRatingId.value) {
      await updateRating(myRatingId.value, val)
      ElMessage.success('评分已更新')
    } else {
      const res: any = await addRating(movie.value.id, val)
      if (res.code === 200) {
        myRatingId.value = res.data?.ratingId || null
        ElMessage.success('评分成功')
      } else {
        ElMessage.error(res.message || '评分失败')
        myScore.value = 0
      }
    }
    await loadMovie()
  } catch (e: any) {
    ElMessage.error(e.message || '评分失败')
    myScore.value = 0
  }
}

async function handleDeleteRating() {
  if (!myRatingId.value) return
  try {
    await ElMessageBox.confirm('确定删除该评分吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteRating(myRatingId.value)
    myScore.value = 0
    myRatingId.value = null
    ElMessage.success('评分已删除')
    await loadMovie()
  } catch {
    // cancelled
  }
}

async function handleFavorite() {
  try {
    if (isFavorited.value) {
      await removeFavorite(movie.value.id)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      const res: any = await addFavorite(movie.value.id)
      if (res.code === 200) {
        isFavorited.value = true
        ElMessage.success('收藏成功')
      } else {
        ElMessage.error(res.message || '收藏失败')
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function onImgError(e: Event) {
  ;(e.target as HTMLImageElement).src = defaultPoster
}
</script>

<style scoped>
.movie-detail {
  max-width: 960px;
  margin: 0 auto;
}

.detail-top {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
  background: transparent;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
}

.poster-section {
  flex-shrink: 0;
}

.poster-img {
  width: 260px;
  height: 370px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.info-section {
  flex: 1;
}

.movie-title {
  font-size: 26px;
  font-weight: 700;
  color: #2C3E50;
  margin: 0 0 20px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  font-size: 14px;
}

.info-label {
  color: #8896A4;
  width: 70px;
  flex-shrink: 0;
}

.info-value {
  color: #2C3E50;
}

.genre-tag {
  margin-right: 6px;
  border-radius: 16px;
}

.rating-section {
  display: flex;
  gap: 30px;
  margin: 20px 0;
  padding: 16px;
  background: #f8faff;
  border-radius: 12px;
}

.rating-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.rating-label {
  color: #8896A4;
  font-size: 14px;
}

.rating-score {
  font-size: 24px;
  font-weight: 700;
  color: #e6a23c;
}

.rating-count {
  font-size: 12px;
  color: #c0c4cc;
}

.action-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eef2f7;
}

.user-rating {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.login-hint {
  text-align: center;
}

.summary-section {
  background: transparent;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  margin-bottom: 24px;
}

.summary-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: #2C3E50;
  margin: 0 0 12px;
  padding-left: 0;
  border-left: none;
}

.summary-text {
  font-size: 15px;
  line-height: 1.8;
  color: #5a6a7e;
  text-indent: 2em;
}

/* 评论区 */
.comment-section {
  background: transparent;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.comment-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #2C3E50;
  margin: 0;
  padding-left: 0;
  border-left: none;
}

.comment-count {
  font-size: 13px;
  color: #8896A4;
}

.comment-input {
  margin-bottom: 24px;
}

.comment-input-action {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-login-hint {
  text-align: center;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8faff;
  border-radius: 12px;
}

.comment-list {
  min-height: 60px;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.comment-avatar {
  background: #5B8DEF;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-username {
  font-size: 14px;
  font-weight: 500;
  color: #2C3E50;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: auto;
}

.comment-content {
  font-size: 14px;
  line-height: 1.7;
  color: #5a6a7e;
  padding-left: 42px;
}

.comment-actions {
  padding-left: 42px;
  margin-top: 6px;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #8896A4;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 12px;
  transition: all 0.2s;
  user-select: none;
}

.like-btn:hover {
  background: #f0f5ff;
  color: #5B8DEF;
}

.like-btn.liked {
  color: #5B8DEF;
  font-weight: 600;
}

@media (max-width: 768px) {
  .detail-top {
    flex-direction: column;
    align-items: center;
  }

  .poster-img {
    width: 200px;
    height: 280px;
  }

  .info-section {
    width: 100%;
  }
}
</style>
