import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/movie/:id',
    alias: '/movies/:id',
    name: 'MovieDetail',
    component: () => import('../views/MovieDetail.vue')
  },
  {
    path: '/search',
    name: 'SearchResult',
    component: () => import('../views/SearchResult.vue')
  },
  {
    path: '/search-movies',
    name: 'SearchMovies',
    component: () => import('../views/SearchMovies.vue')
  },
  {
    path: '/recommend',
    name: 'Recommend',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-advisor',
    name: 'AiAdvisor',
    component: () => import('../views/AiAdvisor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  await userStore.initialize()

  if (to.meta.requiresAuth && !userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') {
    next({ name: 'Home' })
  } else if ((to.name === 'Login' || to.name === 'Register') && userStore.token) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
