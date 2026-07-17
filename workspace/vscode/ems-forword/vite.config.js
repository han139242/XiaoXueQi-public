import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// vite.config.js 是开发服务器和生产打包的配置文件。
// mode 通常是 development 或 production，不同模式可以读取不同的 .env 文件。
export default defineConfig(({ mode }) => {
  // 读取环境变量，例如 VITE_API_PROXY_TARGET；不把后端地址写死在多个业务文件里。
  const env = loadEnv(mode, process.cwd(), '')

  return {
    // 让 Vite 能识别并编译 .vue 单文件组件。
    plugins: [vue()],
    resolve: {
      alias: {
        // 将 @ 映射到 src，例如 @/pages/login/index.vue 等价于 src/pages/login/index.vue。
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      // 监听所有本机网络地址，方便本机、局域网或小程序联调访问。
      host: '0.0.0.0',
      // npm run dev 后，PC 前端默认运行在 5173 端口。
      port: 5173,
      proxy: {
        // 浏览器请求 /api/** 时，由 Vite 开发服务器转发到 Spring Boot 后端。
        // 例如 /api/auth/login 会转发为 http://localhost:8088/api/auth/login。
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8088',
          // 修改代理请求中的 Host，避免后端因为来源主机不同而拒绝请求。
          changeOrigin: true
        }
      }
    },
    build: {
      // 单个打包文件超过 800KB 时给出警告，提醒后续按页面拆包优化。
      chunkSizeWarningLimit: 800
    }
  }
})
