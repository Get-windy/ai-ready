import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag.includes('-')
        }
      }
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'PurchaseManagement',
      fileName: (format) => `purchase-management.${format}.js`
    },
    rollupOptions: {
      // 确保外部化处理那些你不想打包进库的依赖
      external: ['vue', 'element-plus', 'pinia', 'vue-router', 'axios'],
      output: {
        // 在 UMD 构建模式下为这些外部化的依赖提供一个全局变量
        globals: {
          vue: 'Vue',
          'element-plus': 'ElementPlus',
          pinia: 'Pinia',
          'vue-router': 'VueRouter',
          axios: 'axios'
        },
        assetFileNames: (assetInfo) => {
          if (assetInfo.name === 'style.css') {
            return 'purchase-management.css'
          }
          return assetInfo.name || ''
        }
      }
    },
    sourcemap: true,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    reportCompressedSize: true,
    chunkSizeWarningLimit: 500
  },
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/__tests__/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
    exclude: ['node_modules', 'dist'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'coverage/**',
        'dist/**',
        '**/node_modules/**',
        '**/*.d.ts',
        'src/index.ts'
      ]
    }
  },
  server: {
    port: 3001,
    open: false,
    proxy: {
      '/api/v1/purchase': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/v1\/purchase/, '/purchase/api/v1/purchase')
      },
      '/api/admin/purchase': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/admin\/purchase/, '/purchase/api/admin/purchase')
      }
    }
  },
  preview: {
    port: 3002,
    open: true
  }
})