/**
 * 根级 Vite 配置 — 模板/后备配置
 *
 * 实际构建由各 app 自己的 vite.config.ts 接管（例如 apps/pc-admin/vite.config.ts）。
 * 此文件仅作为 pnpm workspace 根目录占位，确保 `npx vite` 在仓库根目录运行
 * 时不会直接报错；日常开发请使用每个 app 各自的 dev/build 脚本。
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

// 获取环境变量
const env = process.env.NODE_ENV || 'development'
const isDev = env === 'development'
const isTest = env === 'test'

// 路径别名配置
const alias = {
  '@': resolve(__dirname, 'src'),
  '@components': resolve(__dirname, 'src/components'),
  '@views': resolve(__dirname, 'src/views'),
  '@store': resolve(__dirname, 'src/store'),
  '@router': resolve(__dirname, 'src/router'),
  '@api': resolve(__dirname, 'src/api'),
  '@utils': resolve(__dirname, 'src/utils'),
  '@types': resolve(__dirname, 'src/types'),
  '@hooks': resolve(__dirname, 'src/hooks'),
  '@assets': resolve(__dirname, 'src/assets'),
  '@ai-ready/components': resolve(__dirname, 'packages/components/src'),
  '@ai-ready/utils': resolve(__dirname, 'packages/utils/src'),
  '@ai-ready/styles': resolve(__dirname, 'packages/styles/src'),
  '@ai-ready/themes': resolve(__dirname, 'packages/themes/src')
}

// API代理配置
const proxyConfig = {
  '/api/v1': {
    target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
    changeOrigin: process.env.VITE_PROXY_CHANGE_ORIGIN === 'true',
    rewrite: (path: string) => path.replace(/^\/api\/v1/, '')
  },
  '/api/admin': {
    target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
    changeOrigin: process.env.VITE_PROXY_CHANGE_ORIGIN === 'true',
    rewrite: (path: string) => path.replace(/^\/api\/admin/, '/admin')
  },
  '/api/mobile': {
    target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
    changeOrigin: process.env.VITE_PROXY_CHANGE_ORIGIN === 'true',
    rewrite: (path: string) => path.replace(/^\/api\/mobile/, '/mobile')
  }
}

export default defineConfig({
  // 基础路径
  base: process.env.VITE_PUBLIC_PATH || '/',
  
  // 插件配置
  plugins: [
    vue(),
    // 自动导入API
    AutoImport({
      imports: [
        'vue',
        'vue-router',
        'pinia',
        {
          'vue': ['createApp']
        }
      ],
      dts: 'src/types/auto-imports.d.ts',
    }),
    // 自动导入组件
    Components({
      dirs: ['src/components/common', 'src/components/layout'],
      extensions: ['vue'],
      dts: 'src/types/components.d.ts',
    }),
    // SVG图标插件
    createSvgIconsPlugin({
      iconDirs: [resolve(__dirname, 'src/assets/icons')],
      symbolId: 'icon-[dir]-[name]'
    })
  ],
  
  // 解析配置
  resolve: {
    alias,
    extensions: ['.js', '.ts', '.jsx', '.tsx', '.vue', '.json']
  },
  
  // 服务器配置
  server: {
    host: process.env.VITE_DEV_SERVER_HOST || 'localhost',
    port: parseInt(process.env.VITE_DEV_SERVER_PORT || '5173'),
    https: process.env.VITE_DEV_SERVER_HTTPS === 'true',
    open: true,
    cors: true,
    proxy: proxyConfig
  },
  
  // 构建配置
  build: {
    target: 'es2015',
    outDir: process.env.VITE_OUTPUT_DIR || 'dist',
    sourcemap: isDev,
    minify: !isDev ? 'terser' : false,
    terserOptions: {
      compress: {
        drop_console: !isDev && !isTest,
        drop_debugger: !isDev && !isTest
      }
    },
    rollupOptions: {
      output: {
        chunkFileNames: 'static/js/[name]-[hash].js',
        entryFileNames: 'static/js/[name]-[hash].js',
        assetFileNames: 'static/[ext]/[name]-[hash].[ext]'
      }
    },
    chunkSizeWarningLimit: 1000
  },
  
  // 环境变量
  define: {
    'import.meta.env.VITE_APP_TITLE': JSON.stringify(process.env.VITE_APP_TITLE || 'AI-Ready'),
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(process.env.VITE_APP_VERSION || '1.0.0'),
    'import.meta.env.VITE_APP_BUILD_TIME': JSON.stringify(process.env.VITE_APP_BUILD_TIME || new Date().toISOString())
  },
  
  // CSS配置
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @import "@/styles/variables.scss";
          @import "@/styles/mixins.scss";
        `
      }
    }
  },
  
  // 优化配置
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'ant-design-vue', '@vueuse/core']
  }
})