<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <div class="brand-badge">系统 登录</div>
        <h1>IoT 设备管理平台</h1>
        <p class="subtitle">请输入账号密码登录控制台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="tech-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="输入用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="输入密码" show-password size="large" prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="submit-btn">
          登 录
        </el-button>
      </el-form>

      <div class="auth-footer">
        <span>还没有账号？</span>
        <router-link to="/register">注册新账号 →</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 24px;
  box-shadow: var(--shadow-md);
  padding: 44px 40px;
  width: 100%;
  max-width: 420px;
  position: relative;
}

.brand-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  background: var(--brand-soft);
  color: var(--brand-dark);
  padding: 4px 12px;
  border-radius: 999px;
  letter-spacing: 0.5px;
  margin-bottom: 18px;
}

.auth-header h1 {
  font-size: 24px;
  font-weight: 800;
  color: var(--ink);
  letter-spacing: -0.3px;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: var(--muted);
  margin-bottom: 28px;
}

.tech-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  padding-bottom: 6px;
}

.tech-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: none;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 8px 14px;
  transition: all 0.18s;
}

.tech-form :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
}

.tech-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--brand) !important;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12) !important;
}

.submit-btn {
  width: 100%;
  border-radius: 12px;
  padding: 22px 0;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border: none;
  color: #ffffff;
  margin-top: 20px;
  box-shadow: var(--shadow-lg);
  transition: transform 0.18s, box-shadow 0.18s, opacity 0.18s;
}

.submit-btn:hover {
  transform: translateY(-1px);
  opacity: 0.95;
  box-shadow: 0 14px 34px rgba(99, 102, 241, 0.28);
}

.auth-footer {
  margin-top: 28px;
  border-top: 1px solid var(--line);
  padding-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.auth-footer span {
  color: #94a3b8;
}

.auth-footer a {
  color: var(--brand);
  font-weight: 600;
  text-decoration: none;
}

.auth-footer a:hover {
  color: var(--brand-dark);
}
</style>