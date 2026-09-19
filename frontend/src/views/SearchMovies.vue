<template>
  <div class="search-movies-page">
    <!-- 标题 -->
    <h1 class="page-title">🔍 搜索电影</h1>

    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="输入电影名称进行搜索..."
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch" :icon="Search">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 类型筛选标签 -->
    <div class="genre-filter">
      <span
        v-for="g in genreList"
        :key="g"
        class="genre-check-tag"
        :class="{ active: selectedGenre === g }"
        @click="selectGenre(g)"
      >{{ g }}</span>
    </div>

    <!-- 结果统计 -->
    <div class="result-info" v-if="!loading">
      <span v-if="isSearchMode">
        搜索 "{{ searchKeyword }}" 找到 <strong>{{ total }}</strong> 部电影
      </span>
      <span v-else>
        共 <strong>{{ total }}</strong> 部电影
      </span>
    </div>

    <!-- 电影网格 -->
    <div v-loading="loading">
      <el-row :gutter="20" v-if="movies.length > 0">
        <el-col
          v-for="movie in movies"
          :key="movie.id"
          :xs="12"
          :sm="8"
          :md="6"
          :lg="4"
        >
          <MovieCard :movie="movie" />
        </el-col>
      </el-row>

      <el-empty v-else-if="!loading" description="未找到相关电影" />

      <!-- 分页器 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getMovieList, searchMovies } from '../api/movie'
import MovieCard from '../components/MovieCard.vue'

const genreList = ['全部', '剧情', '喜剧', '动画', '爱情', '悬疑', '动作', '科幻', '历史', '战争', '恐怖']

const keyword = ref('')
const searchKeyword = ref('')
const selectedGenre = ref('全部')
const movies = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const isSearchMode = ref(false)

onMounted(() => {
  loadMovies()
})

function selectGenre(g: string) {
  selectedGenre.value = g
  currentPage.value = 1
  isSearchMode.value = false
  keyword.value = ''
  searchKeyword.value = ''
  loadMovies()
}

function handleSearch() {
  if (!keyword.value.trim()) {
    isSearchMode.value = false
    loadMovies()
    return
  }
  searchKeyword.value = keyword.value.trim()
  currentPage.value = 1
  isSearchMode.value = true
  doSearch()
}

async function loadMovies() {
  loading.value = true
  try {
    const genre = selectedGenre.value === '全部' ? undefined : selectedGenre.value
    const res: any = await getMovieList(currentPage.value, pageSize.value, genre)
    if (res.code === 200) {
      movies.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function doSearch() {
  loading.value = true
  try {
    const res: any = await searchMovies(searchKeyword.value, currentPage.value, pageSize.value)
    if (res.code === 200) {
      movies.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  if (isSearchMode.value) {
    doSearch()
  } else {
    loadMovies()
  }
}
</script>

<style scoped>
.search-movies-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #2C3E50;
  margin: 0 0 20px;
}

.search-bar {
  margin-bottom: 20px;
}

.search-bar :deep(.el-input-group__append) {
  background: #5B8DEF;
  color: #fff;
  border-color: #5B8DEF;
  cursor: pointer;
}

.search-bar :deep(.el-input-group__append:hover) {
  background: #4a7de0;
}

.genre-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.genre-check-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  background: #f0f2f5;
  color: #5a6a7e;
  transition: all 0.2s;
  user-select: none;
}

.genre-check-tag:hover {
  background: #ecf2fe;
  color: #5B8DEF;
}

.genre-check-tag.active {
  background: #5B8DEF;
  color: #fff;
}

.result-info {
  font-size: 14px;
  color: #8896A4;
  margin-bottom: 20px;
}

.result-info strong {
  color: #5B8DEF;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 20px;
}

:deep(.el-pagination .el-pager li.is-active) {
  background-color: #5B8DEF;
  border-radius: 6px;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next) {
  border-radius: 6px;
}
</style>
