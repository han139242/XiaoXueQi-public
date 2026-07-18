<script setup>
import { useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { firstAccessiblePath } from '@/config/navigation'

// 404页面用于匹配不存在的地址，replace返回首页后不会把错误地址留在返回历史中。
const router = useRouter()

function returnHome() {
  router.replace(firstAccessiblePath('/forbidden'))
}
</script>

<template>
  <!-- 未匹配到任何路由时显示404说明和返回首页按钮。 -->
  <main class="not-found">
    <div class="error-code">404</div>
    <h1>页面不存在</h1>
    <p>访问地址无效或页面已经调整。</p>
    <el-button type="primary" @click="returnHome">
      <AppIcon name="home" :size="16" />
      返回首页
    </el-button>
  </main>
</template>

<style scoped>
/* 404页面使用全屏居中布局突出错误码和恢复入口。 */
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
