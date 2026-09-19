<template>
  <div class="register-page">
    <el-card class="register-card">
      <div class="register-header">
        <span class="register-logo">🎬</span>
        <h2 class="register-title">智能电影推荐</h2>
        <p class="register-subtitle">用户注册</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="2-20位字符"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="不少于6位"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        <el-form-item prop="preferences">
          <div class="pref-label">选择你喜欢的电影类型</div>
          <div class="pref-tags">
            <el-check-tag
              v-for="tag in prefOptions"
              :key="tag"
              :checked="form.preferences.includes(tag)"
              @change="togglePref(tag)"
              class="pref-tag"
            >{{ tag }}</el-check-tag>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            size="large"
            style="width: 100%; border-radius: 10px;"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">去登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const prefOptions = ['动作', '喜剧', '科幻', '爱情', '恐怖', '悬疑', '动画', '剧情', '历史', '战争']

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  preferences: [] as string[]
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePreferences = (_rule: any, value: string[], callback: any) => {
  if (!value || value.length === 0) {
    callback(new Error('请至少选择一个偏好类型'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名2-20位字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  preferences: [
    { validator: validatePreferences, trigger: 'change' }
  ]
}

function togglePref(tag: string) {
  const idx = form.preferences.indexOf(tag)
  if (idx >= 0) {
    form.preferences.splice(idx, 1)
  } else {
    form.preferences.push(tag)
  }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const prefs = form.preferences.join(',')
    await userStore.register(form.username, form.password, prefs)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 120px);
}

.register-card {
  width: 450px;
  border-radius: 16px;
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.register-logo {
  font-size: 40px;
}

.register-title {
  font-size: 22px;
  font-weight: 700;
  color: #2C3E50;
  margin: 8px 0 4px;
}

.register-subtitle {
  font-size: 14px;
  color: #8896A4;
  margin: 0;
}

.pref-label {
  font-size: 14px;
  color: #5a6a7e;
  margin-bottom: 10px;
}

.pref-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pref-tag {
  cursor: pointer;
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 20px;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #8896A4;
}

.register-footer a {
  color: #5B8DEF;
  text-decoration: none;
  font-weight: 500;
}

.register-footer a:hover {
  text-decoration: underline;
}
</style>
