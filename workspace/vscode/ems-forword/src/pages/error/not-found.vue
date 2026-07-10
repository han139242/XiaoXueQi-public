<script setup>
import { useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'

const router = useRouter()
</script>

<template>
  <main class="not-found">
    <div class="error-code">404</div>
    <h1>页面不存在</h1>
    <p>访问地址无效或页面已经调整。</p>
    <el-button type="primary" @click="router.replace('/dashboard')">
      <AppIcon name="home" :size="16" />
      返回工作台
    </el-button>
  </main>
</template>

<style scoped>
.not-found {
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 24px;
  color: var(--mes-text);
  background: var(--mes-bg);
  text-align: center;
}

.error-code {
  color: var(--mes-primary);
  font-size: 76px;
  font-weight: 720;
  line-height: 1;
}

.not-found h1 {
  margin-top: 18px;
  font-size: 22px;
}

.not-found p {
  margin: 8px 0 24px;
  color: var(--mes-text-secondary);
}

.not-found :deep(.el-button) {
  gap: 7px;
}
</style>
