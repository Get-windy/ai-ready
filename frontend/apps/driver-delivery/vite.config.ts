import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [VantResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@ai-ready/components': resolve(__dirname, '../../packages/components/src'),
    }
  },
  server: {
    port: 3004,
    host: true,
    // 司机端所有 /api/** 转发到 core-api（`utils/request` 的 baseURL 为 /api）
    proxy: {
      '/api': {
        target: 'http://localhost:5655',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})