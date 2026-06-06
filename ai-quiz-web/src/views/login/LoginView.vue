<template>
  <div class="login-page">
    <!-- Left: brand -->
    <div class="login-brand">
      <!-- decorative grid -->
      <div class="brand-grid"></div>
      <!-- extra accent circles -->
      <div class="brand-accent-circle circle-sm"></div>
      <div class="brand-accent-circle circle-xs"></div>
      <div class="brand-content">
        <div class="brand-badge">AI</div>
        <h1 class="brand-title">智能答题系统</h1>
        <div class="brand-line"></div>
        <p class="brand-desc">基于大语言模型的在线考试与答题生成平台，<br/>一键生成高质量试题，智能批改即时反馈。</p>
        <div class="brand-features">
          <span class="feat-item">AI 出题</span>
          <span class="feat-dot">·</span>
          <span class="feat-item">自动批改</span>
          <span class="feat-dot">·</span>
          <span class="feat-item">数据分析</span>
        </div>
      </div>
    </div>

    <!-- Right: form -->
    <div class="login-form-side">
      <div class="form-side-dots"></div>
      <div class="form-wrapper">
        <div class="form-accent-bar"></div>
        <h2 class="form-title">登录</h2>
        <p class="form-sub">欢迎回来，请登录您的账号</p>
        <el-form :model="form" :rules="rules" ref="formRef" class="login-form" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              size="large"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { LoginForm } from '@/types'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive<LoginForm>({ username: '', password: '' })
const loading = ref(false)
const formRef = ref<FormInstance>()

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(form)
    userStore.setToken(data.data.token)
    await userStore.fetchUserInfo()
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
    ElMessage.success('登录成功')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

/* ===== Left: Brand Panel ===== */
.login-brand {
  flex: 0 0 65%;
  background: #151832;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
/* Subtle geometric decoration — slow rotation */
.login-brand::before {
  content: '';
  position: absolute;
  top: -120px;
  right: -120px;
  width: 500px;
  height: 500px;
  border: 2px solid rgba(255,255,255,.06);
  border-radius: 50%;
  animation: orbit-slow 60s linear infinite;
}
.login-brand::after {
  content: '';
  position: absolute;
  bottom: -80px;
  left: -60px;
  width: 320px;
  height: 320px;
  border: 2px solid rgba(255,255,255,.04);
  border-radius: 50%;
  animation: orbit-slow 45s linear infinite reverse;
}
/* Extra accent circles */
.brand-accent-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(139,143,240,.12);
  pointer-events: none;
}
.circle-sm {
  top: 18%;
  left: 12%;
  width: 180px;
  height: 180px;
  animation: orbit-slow 35s linear infinite;
}
.circle-xs {
  bottom: 25%;
  right: 15%;
  width: 80px;
  height: 80px;
  background: rgba(139,143,240,.04);
  animation: orbit-slow 25s linear infinite reverse;
}
/* Dot grid overlay */
.brand-grid {
  position: absolute;
  inset: 0;
  opacity: .03;
  background-image: radial-gradient(circle, #fff 1px, transparent 1px);
  background-size: 28px 28px;
  pointer-events: none;
}
@keyframes orbit-slow {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 400px;
  padding: 60px 48px;
}

/* Staggered entrance */
.brand-badge {
  display: inline-block;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .12em;
  color: #8b8ff0;
  padding: 5px 14px;
  border: 1px solid rgba(139, 143, 240, .3);
  border-radius: 2px;
  margin-bottom: 32px;
  opacity: 0;
  animation: fadeUp 0.6s .1s ease forwards;
}
.brand-title {
  font-size: 38px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -.02em;
  line-height: 1.2;
  margin: 0;
  opacity: 0;
  animation: fadeUp 0.6s .25s ease forwards;
}
.brand-line {
  width: 48px;
  height: 3px;
  background: #5c61e0;
  margin: 28px 0;
  opacity: 0;
  animation: fadeUp 0.6s .35s ease forwards, lineGrow 0.8s .35s ease forwards;
}
@keyframes lineGrow {
  from { width: 0; }
  to   { width: 48px; }
}
.brand-desc {
  color: rgba(255,255,255,.55);
  font-size: 15px;
  line-height: 1.8;
  margin: 0 0 36px 0;
  opacity: 0;
  animation: fadeUp 0.6s .5s ease forwards;
}
.brand-features {
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: 0;
  animation: fadeUp 0.6s .65s ease forwards;
}
.feat-item {
  color: rgba(255,255,255,.7);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: .04em;
  transition: color .3s;
}
.feat-item:hover {
  color: #8b8ff0;
}
.feat-dot {
  color: rgba(255,255,255,.2);
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ===== Right: Form Panel ===== */
.login-form-side {
  flex: 0 0 35%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafbfc;
  position: relative;
  overflow: hidden;
  opacity: 0;
  animation: fadeIn 0.6s .4s ease forwards;
}
/* Subtle dot grid on form side */
.form-side-dots {
  position: absolute;
  inset: 0;
  opacity: .04;
  background-image: radial-gradient(circle, #5b6cc4 1.5px, transparent 1.5px);
  background-size: 32px 32px;
  pointer-events: none;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}
.form-wrapper {
  width: 380px;
  padding: 40px;
  position: relative;
  z-index: 1;
}
/* Thin accent bar above title */
.form-accent-bar {
  width: 32px;
  height: 3px;
  background: var(--accent);
  margin-bottom: 24px;
  border-radius: 1px;
}
.form-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -.02em;
  margin: 0 0 6px 0;
}
.form-sub {
  color: var(--text-tertiary);
  font-size: 14px;
  margin: 0 0 40px 0;
}
.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}
.login-form :deep(.el-input__wrapper) {
  padding: 6px 14px;
  box-shadow: none !important;
  transition: border-color .25s, box-shadow .25s;
}
.login-form :deep(.el-input__wrapper:hover) {
  border-color: var(--accent);
}
.login-form :deep(.el-input.is-focus .el-input__wrapper) {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(79,79,217,.1) !important;
}
.login-form :deep(.el-input__inner) {
  font-size: 15px;
}
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: .08em;
  margin-top: 8px;
  transition: all .25s ease;
}
.login-btn:not(.is-loading):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(79,79,217,.3);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }
  .login-brand {
    flex: 0 0 auto;
    padding: 48px 32px;
  }
  .brand-content {
    padding: 0;
    text-align: center;
  }
  .brand-line {
    margin: 24px auto;
  }
  .brand-features {
    justify-content: center;
  }
  .brand-title {
    font-size: 28px;
  }
  .form-wrapper {
    width: 100%;
    max-width: 380px;
  }
}
</style>
