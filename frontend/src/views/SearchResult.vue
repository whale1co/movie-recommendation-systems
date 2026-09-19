<template>
  <div class="search-result-page">
    <div class="search-header">
      <h2 v-if="keyword">🔍 搜索 "{{ keyword }}" 的结果</h2>
      <p v-if="total > 0" class="result-count">共找到 {{ total }} 部电影</p>
    </div>

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

      <el-empty v-else-if="!loading && searched" description="未找到相关电影" />

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
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { searchMovies } from '../api/movie'
import MovieCard from '../components/MovieCard.vue'

const route = useRoute()

const keyword = ref('')
const movies = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const searched = ref(false)

onMounted(() => {
  doSearch()
})

watch(() => route.query.q, () => {
  currentPage.value = 1
  doSearch()
})

async function doSearch() {
  const q = (route.query.q as string) || ''
  if (!q.trim()) {
    movies.value = []
    total.value = 0
    searched.value = true
    return
  }

  keyword.value = q
  loading.value = true
  searched.value = false

  try {
    const res: any = await searchMovies(q, currentPage.value, pageSize.value)
    if (res.code === 200) {
      movies.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
    searched.value = true
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  doSearch()
}
</script>

<style scoped>
.search-result-page {
  max-width: 1200px;
  margin: 0 auto;
}

.search-header {
  margin-bottom: 24px;
}

.search-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #2C3E50;
  margin: 0 0 8px;
}

.result-count {
  font-size: 14px;
  color: #8896A4;
  margin: 0;
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
