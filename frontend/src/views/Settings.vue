<template>
  <div class="settings-page">
    <h2 class="page-title">账号设置</h2>

    <el-card class="settings-card">
      <h3 class="section-title">基本信息</h3>
      <el-form label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="userStore.username" disabled />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings-card">
      <h3 class="section-title">修改密码</h3>
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="至少8位，包含字母和数字"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            placeholder="再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" round :loading="pwdLoading" @click="handleChangePassword">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings-card">
      <h3 class="section-title">修改偏好</h3>
      <el-form label-width="100px">
        <el-form-item label="电影偏好">
          <div class="pref-tags">
            <el-check-tag
              v-for="tag in prefOptions"
              :key="tag"
              :checked="prefForm.preferences.includes(tag)"
              @change="togglePref(tag)"
              class="pref-tag"
            >{{ tag }}</el-check-tag>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" round :loading="prefLoading" @click="handleSavePreferences">
            保存偏好
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

const pwdFormRef = ref<FormInstance>()
const pwdLoading = ref(false)
const prefLoading = ref(false)

const prefOptions = ['动作', '喜剧', '科幻', '爱情', '恐怖', '悬疑', '动画', '剧情', '历史', '战争']

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const prefForm = reactive({
  preferences: [] as string[]
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度为8到72位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码必须同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

onMounted(() => {
  if (userStore.preferences) {
    prefForm.preferences = userStore.preferences.split(',').filter(Boolean)
  }
})

function togglePref(tag: string) {
  const idx = prefForm.preferences.indexOf(tag)
  if (idx >= 0) {
    prefForm.preferences.splice(idx, 1)
  } else {
    prefForm.preferences.push(tag)
  }
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    await userStore.updateUserProfile({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

async function handleSavePreferences() {
  if (prefForm.preferences.length === 0) {
    ElMessage.warning('请至少选择一个偏好类型')
    return
  }

  prefLoading.value = true
  try {
    const prefs = prefForm.preferences.join(',')
    await userStore.updateUserProfile({ preferences: prefs })
    ElMessage.success('偏好修改成功')
  } catch (e: any) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    prefLoading.value = false
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #2C3E50;
  margin: 0 0 24px;
}

.settings-card {
  margin-bottom: 24px;
  border-radius: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #2C3E50;
  margin: 0 0 20px;
  padding-left: 10px;
  border-left: 3px solid #5B8DEF;
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
</style>
