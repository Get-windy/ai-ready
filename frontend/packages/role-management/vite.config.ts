import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/main.ts'),
      name: 'RoleManagement',
      fileName: (format) => `role-management.${format}.js`
    },
    rollupOptions: {
      external: ['vue', 'element-plus', 'pinia'],
      output: {
        globals: {
          vue: 'Vue',
          'element-plus': 'ElementPlus',
          pinia: 'Pinia'
        }
      }
    }
  },
  test: {
    globals: true,
    environment: 'happy-dom'
  }
})