import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import compression from 'vite-plugin-compression'
import { createHtmlPlugin } from 'vite-plugin-html'
import viteImagemin from 'vite-plugin-imagemin'
import { visualizer } from 'rollup-plugin-visualizer'

// 性能优化配置
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const isProduction = mode === 'production'
  
  return {
    // 基础配置
    base: env.VITE_PUBLIC_PATH || '/',
    
    // 路径别名
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
        '@components': resolve(__dirname, 'src/components'),
        '@utils': resolve(__dirname, 'src/utils'),
        '@stores': resolve(__dirname, 'src/stores'),
        '@views': resolve(__dirname, 'src/views'),
        '@assets': resolve(__dirname, 'src/assets')
      }
    },
    
    // 插件配置
    plugins: [
      vue(),
      
      // HTML 注入优化
      createHtmlPlugin({
        minify: isProduction,
        inject: {
          data: {
            title: 'AI-Ready 智企连',
            injectScript: isProduction ? `<script>console.log('Production build')</script>` : ''
          }
        }
      }),
      
      // Gzip 压缩
      compression({
        algorithm: 'gzip',
        ext: '.gz',
        threshold: 10240, // 10KB 以上才压缩
        deleteOriginFile: false
      }),
      
      // Brotli 压缩
      isProduction && compression({
        algorithm: 'brotliCompress',
        ext: '.br',
        threshold: 10240
      }),
      
      // 图片压缩
      isProduction && viteImagemin({
        gifsicle: {
          optimizationLevel: 3,
          interlaced: false
        },
        mozjpeg: {
          quality: 80
        },
        optipng: {
          optimizationLevel: 7
        },
        pngquant: {
          quality: [0.8, 0.9],
          speed: 4
        },
        svgo: {
          plugins: [
            {
              name: 'removeViewBox',
              active: false
            }
          ]
        }
      }),
      
      // 打包分析
      isProduction && visualizer({
        open: false,
        gzipSize: true,
        brotliSize: true,
        filename: 'stats.html'
      })
    ].filter(Boolean),
    
    // 构建配置
    build: {
      target: 'es2015',
      
      // 代码分割策略
      rollupOptions: {
        output: {
          // 分包策略
          manualChunks: {
            // Vue 核心
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            // Ant Design Vue
            'antd-vendor': ['ant-design-vue', '@ant-design/icons-vue'],
            // 工具库
            'utils-vendor': ['axios', 'dayjs', 'lodash-es'],
            // 国际化
            'i18n-vendor': ['vue-i18n']
          },
          
          // 入口文件命名
          entryFileNames: 'js/[name]-[hash:8].js',
          
          // 分包文件命名
          chunkFileNames: 'js/[name]-[hash:8].js',
          
          // 静态资源命名
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name.split('.')
            let extType = info[info.length - 1]
            
            if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(assetInfo.name)) {
              return 'images/[name]-[hash:8].[ext]'
            }
            if (/\.(mp4|webm|ogg|mp3|wav|flac|aac)$/i.test(assetInfo.name)) {
              return 'media/[name]-[hash:8].[ext]'
            }
            if (/\.(woff2?|eot|ttf|otf)$/i.test(assetInfo.name)) {
              return 'fonts/[name]-[hash:8].[ext]'
            }
            if (/\.css$/i.test(assetInfo.name)) {
              return 'css/[name]-[hash:8].css'
            }
            return 'assets/[name]-[hash:8].[ext]'
          }
        }
      },
      
      // CSS 代码分割
      cssCodeSplit: true,
      
      // 最小化配置
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: isProduction,
          drop_debugger: isProduction,
          pure_funcs: isProduction ? ['console.log'] : []
        },
        format: {
          comments: false
        }
      },
      
      // 块大小警告限制
      chunkSizeWarningLimit: 1000,
      
      // 启用 source map
      sourcemap: !isProduction,
      
      // 内联小于 4KB 的资源
      assetsInlineLimit: 4096
    },
    
    // CSS 配置
    css: {
      modules: {
        localsConvention: 'camelCase'
      },
      preprocessorOptions: {
        less: {
          javascriptEnabled: true,
          modifyVars: {
            // Ant Design 主题变量
            'primary-color': '#1890ff'
          }
        }
      },
      devSourcemap: true
    },
    
    // 开发服务器配置
    server: {
      port: 3000,
      host: true,
      open: true,
      cors: true,
      
      // 热更新配置
      hmr: {
        overlay: true
      },
      
      // 代理配置
      proxy: {
        '/api': {
          target: env.VITE_API_URL || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    },
    
    // 预览服务器配置
    preview: {
      port: 4000,
      host: true,
      open: true
    },
    
    // 依赖优化
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'ant-design-vue',
        '@ant-design/icons-vue',
        'axios',
        'dayjs',
        'lodash-es'
      ],
      exclude: []
    },
    
    // 性能配置
    performance: {
      // 大依赖警告阈值
      maxAssetSize: 512000,
      maxEntrypointSize: 512000,
      hints: isProduction ? 'warning' : false
    }
  }
})