import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../store/user'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'AdminLogin', component: () => import('../views/Login.vue') },
  { path: '/dashboard', name: 'AdminDashboard', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/tasks', name: 'AdminTasks', component: () => import('../views/Tasks.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/movies', name: 'AdminMovies', component: () => import('../views/Movies.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/users', name: 'AdminUsers', component: () => import('../views/Users.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/audit', name: 'AdminAudit', component: () => import('../views/Audit.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
  { path: '/forbidden', name: 'Forbidden', component: () => import('../views/Forbidden.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  await userStore.initialize()
  if (to.meta.requiresAuth && !userStore.token) next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
  else if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') next({ name: 'Forbidden' })
  else if (to.name === 'AdminLogin' && userStore.token && userStore.role === 'ADMIN') next({ name: 'AdminDashboard' })
  else next()
})

export default router
