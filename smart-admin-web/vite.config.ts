import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import viteCompression from 'vite-plugin-compression'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia', { 'vue-i18n': ['useI18n'] }],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [
        AntDesignVueResolver({
          importStyle: false,
        }),
      ],
      dts: 'src/components.d.ts',
    }),
    // Gzip 压缩
    viteCompression({
      algorithm: 'gzip',
      threshold: 10240, // 大于 10KB 的文件压缩
      deleteOriginFile: false,
    }),
    // Brotli 压缩（更好的压缩率）
    viteCompression({
      algorithm: 'brotliCompress',
      threshold: 10240,
      deleteOriginFile: false,
    }),
  ],
  
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  
  // CSS 配置
  css: {
    // CSS 预处理器配置
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
      },
    },
    // 开发时的 CSS source map
    devSourcemap: false,
  },
  
  server: {
    port: 3000,
    host: true, // 允许外部访问
    open: false, // 不自动打开浏览器
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
    // 预热常用文件（提升开发启动速度）
    warmup: {
      clientFiles: [
        './src/main.ts',
        './src/App.vue',
        './src/router/index.ts',
        './src/stores/user.ts',
      ],
    },
  },
  
  // 构建优化
  build: {
    // 目标浏览器
    target: 'es2020',
    
    // 输出目录
    outDir: 'dist',
    
    // 静态资源目录
    assetsDir: 'assets',
    
    // 是否生成 source map
    sourcemap: false,
    
    // chunk 大小警告阈值
    chunkSizeWarningLimit: 1000,
    
    // 压缩配置
    minify: 'terser',
    terserOptions: {
      compress: {
        // 生产环境移除 console
        drop_console: true,
        drop_debugger: true,
        // 移除注释
        pure_funcs: ['console.log'],
      },
    },
    
    // Rollup 配置
    rollupOptions: {
      output: {
        // 入口文件名
        entryFileNames: 'js/[name]-[hash].js',
        
        // chunk 文件名
        chunkFileNames: 'js/[name]-[hash].js',
        
        // 静态资源文件名
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split('.')
          const ext = info[info.length - 1]
          
          if (/png|jpe?g|gif|svg|webp|ico/.test(ext)) {
            return 'images/[name]-[hash].[ext]'
          }
          if (/woff2?|eot|ttf|otf/.test(ext)) {
            return 'fonts/[name]-[hash].[ext]'
          }
          if (/css/.test(ext)) {
            return 'css/[name]-[hash].[ext]'
          }
          return 'assets/[name]-[hash].[ext]'
        },
        
        // 代码分割策略
        manualChunks: (id) => {
          // 第三方库分割
          if (id.includes('node_modules')) {
            // Ant Design Vue 相关
            if (id.includes('ant-design-vue') || id.includes('@ant-design')) {
              return 'antd'
            }
            
            // Vue 核心库
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
              return 'vue-vendor'
            }
            
            // 工具库
            if (id.includes('lodash') || id.includes('dayjs') || id.includes('axios')) {
              return 'utils-vendor'
            }
            
            // 图表库（如果使用）
            if (id.includes('echarts') || id.includes('chart')) {
              return 'charts-vendor'
            }
            
            // 其他第三方库
            return 'vendor'
          }
          
          // 业务模块分割
          if (id.includes('src/views/erp')) {
            return 'erp-module'
          }
          if (id.includes('src/views/crm')) {
            return 'crm-module'
          }
          if (id.includes('src/views/system')) {
            return 'system-module'
          }
          if (id.includes('src/views/dashboard')) {
            return 'dashboard-module'
          }
          
          // API 模块
          if (id.includes('src/api')) {
            return 'api-module'
          }
          
          // 公共组件
          if (id.includes('src/components') || id.includes('src/layouts')) {
            return 'common-components'
          }
          
          // 工具函数
          if (id.includes('src/utils') || id.includes('src/composables')) {
            return 'common-utils'
          }
        },
      },
    },
  },
  
  // 优化依赖预构建
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'vue-i18n',
      'ant-design-vue',
      '@ant-design/icons-vue',
      'axios',
      'dayjs',
      'lodash-es',
      'nprogress',
    ],
    exclude: [],
  },
  
  // 预加载配置
  experimental: {
    // 预渲染支持（可选）
    renderBuiltUrl: (filename, { hostType }) => {
      if (hostType === 'js') {
        return { runtime: `new URL(${filename}, import.meta.url).href` }
      }
      return { relative: filename }
    },
  },
})