<template>
  <div class="home-page">
    <!-- 为你推荐 -->
    <div class="section" v-if="userStore.token && isRecommendPage">
      <h2 class="section-title">
        <span class="title-icon">⭐</span>
        为你推荐
      </h2>
      <div v-loading="recLoading">
        <el-row :gutter="20" v-if="recMovies.length > 0">
          <el-col
            v-for="movie in recMovies"
            :key="movie.id"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="4"
          >
            <MovieCard :movie="movie" :show-summary="true" />
          </el-col>
        </el-row>
        <el-empty v-else description="暂无推荐数据" />
      </div>
    </div>

    <!-- 热门电影 -->
    <div class="section" v-if="!isRecommendPage">
      <h2 class="section-title">
        <span class="title-icon">📺</span>
        热门电影
      </h2>
      <div v-loading="hotLoading">
        <el-row :gutter="20" v-if="hotMovies.length > 0">
          <el-col
            v-for="movie in hotMovies"
            :key="movie.id"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="4"
          >
            <MovieCard :movie="movie" :show-summary="true" />
          </el-col>
        </el-row>
        <el-empty v-else description="暂无热门电影" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getHotMovies, getRecommendations, getColdStart } from '../api/movie'
import { useUserStore } from '../store/user'
import MovieCard from '../components/MovieCard.vue'

const route = useRoute()
const userStore = useUserStore()
const hotMovies = ref<any[]>([])
const recMovies = ref<any[]>([])
const hotLoading = ref(false)
const recLoading = ref(false)

const isRecommendPage = computed(() => route.path === '/recommend')

onMounted(async () => {
  await loadHotMovies()
  if (userStore.token) {
    await loadRecommendations()
  }
})

async function loadHotMovies() {
  hotLoading.value = true
  try {
    const res: any = await getHotMovies(12)
    if (res.code === 200) {
      hotMovies.value = res.data || []
    }
  } finally {
    hotLoading.value = false
  }
}

async function loadRecommendations() {
  recLoading.value = true
  try {
    const res: any = await getRecommendations()
    if (res.code === 200 && res.data && res.data.length > 0) {
      recMovies.value = res.data
    } else {
      const coldRes: any = await getColdStart()
      if (coldRes.code === 200) {
        recMovies.value = coldRes.data || []
      }
    }
  } catch {
    try {
      const coldRes: any = await getColdStart()
      if (coldRes.code === 200) {
        recMovies.value = coldRes.data || []
      }
    } catch {
      recMovies.value = []
    }
  } finally {
    recLoading.value = false
  }
}
</script>

<style scoped>
.home-page {
  padding: 0 0 40px;
}

/* 板块 */
.section {
  margin-bottom: 36px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #2C3E50;
  margin: 0 0 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 22px;
}
</style>
