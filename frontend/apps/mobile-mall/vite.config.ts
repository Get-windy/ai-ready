import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'
import { resolve } from 'path'
import { execSync } from 'child_process'
import fs from 'fs'

/**
 * 自定义 Vite 插件: 编译 Service Worker
 *
 * 在生产构建时将 src/sw.ts 编译为独立的 sw.js 文件输出到 dist 目录。
 * 使用 esbuild (Vite 内置依赖) 进行编译，支持 TypeScript 转译和压缩。
 */
function buildServiceWorker(): Plugin {
  return {
    name: 'build-service-worker',
    apply: 'build',
    enforce: 'post',

    closeBundle() {
      const swSource = resolve(__dirname, 'src/sw.ts')
      const swOutput = resolve(__dirname, 'dist/sw.js')

      console.log('\n[build-sw] 编译 Service Worker…')

      try {
        // 查找本地 esbuild 安装路径
        const esbuildPath = resolve(__dirname, '../../node_modules/.bin/esbuild')
        const esbuildCmd = fs.existsSync(esbuildPath) ? `"${esbuildPath}"` : 'npx esbuild'

        execSync(
          [
            esbuildCmd,
            `"${swSource}"`,
            '--bundle',
            '--format=iife',
            '--target=es2017',
            '--outfile=' + `"${swOutput}"`,
            '--minify',
          ].join(' '),
          { stdio: 'inherit' },
        )

        // 复制一份到 public 目录方便 dev preview
        const swPublic = resolve(__dirname, 'public/sw.js')
        fs.copyFileSync(swOutput, swPublic)

        console.log(`[build-sw] 完成 → ${swOutput}\n`)
      } catch (err) {
        console.error('[build-sw] 编译失败:', err)

        // 兜底: 如果 esbuild 不可用，使用 public/sw.js 直接复制
        const swFallback = resolve(__dirname, 'public/sw.js')
        if (fs.existsSync(swFallback)) {
          fs.mkdirSync(resolve(__dirname, 'dist'), { recursive: true })
          fs.copyFileSync(swFallback, swOutput)
          console.log('[build-sw] 使用备用 public/sw.js 完成')
        }
      }
    },
  }
}

export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [VantResolver()]
    }),
    buildServiceWorker(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@ai-ready/components': resolve(__dirname, '../../packages/components/src'),
    }
  },
  server: {
    port: 3002,
    host: true
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})