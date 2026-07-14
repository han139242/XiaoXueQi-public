<script setup>
import { useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { getNavigationItems } from '@/config/navigation'
import { clearAuth } from '@/utils/auth'

const router = useRouter()

async function returnHome() {
  const target = getNavigationItems().find((item) => item.path)?.path
  if (!target) {
    clearAuth()
    await router.replace('/login')
    return
  }
  await router.replace(target)
}
</script>

<template>
  <main class="forbidden-page">
    <div class="forbidden-code">403</div>
    <h1>当前账号无访问权限</h1>
    <p>该功能未分配给你的岗位角色。</p>
    <el-button type="primary" @click="returnHome">
      <AppIcon name="dashboard" :size="16" />返回首页
    </el-button>
  </main>
</template>

<style scoped>
.forbidden-page {
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  color: var(--mes-text);
  background: var(--mes-bg);
}

.forbidden-code {
  color: var(--mes-primary);
  font-size: 52px;
  font-weight: 700;
}

h1 {
  font-size: 22px;
}

p {
  color: var(--mes-text-muted);
  font-size: 13px;
}
</style>
