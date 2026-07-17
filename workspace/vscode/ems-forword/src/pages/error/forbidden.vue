<script setup>
import { useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { getNavigationItems } from '@/config/navigation'
import { clearAuth } from '@/utils/auth'

// 403表示用户已登录但没有访问当前页面的权限。
const router = useRouter()

// 返回当前角色第一个可访问菜单；若没有任何菜单则清理登录状态重新登录。
async function returnHome() {
  // 优先回到当前角色第一个可访问菜单；完全没有菜单通常说明登录信息异常，需要重新登录。
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
  <!-- 已登录但无权限时显示403说明，并提供返回可访问首页的按钮。 -->
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
/* 403页面使用全屏居中布局，视觉结构与404页保持一致。 */
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
