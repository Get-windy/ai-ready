// vite.config.ts
import { defineConfig } from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/vite@5.4.21_@types+node@20.19.41_sass-embedded@1.100.0_terser@5.48.0/node_modules/vite/dist/node/index.js";
import vue from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/@vitejs+plugin-vue@5.2.4_vite@5.4.21_vue@3.5.34/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import { resolve } from "path";
import AutoImport from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/unplugin-auto-import@0.17.8/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/unplugin-vue-components@0.26.0_vue@3.5.34/node_modules/unplugin-vue-components/dist/vite.js";
import { AntDesignVueResolver } from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/unplugin-vue-components@0.26.0_vue@3.5.34/node_modules/unplugin-vue-components/dist/resolvers.js";
import viteCompression from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/vite-plugin-compression@0.5.1_vite@5.4.21/node_modules/vite-plugin-compression/dist/index.mjs";
import { visualizer } from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/rollup-plugin-visualizer@7.0.1/node_modules/rollup-plugin-visualizer/dist/plugin/index.js";
import viteImagemin from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/vite-plugin-imagemin@0.6.1_vite@5.4.21/node_modules/vite-plugin-imagemin/dist/index.mjs";
var __vite_injected_original_dirname = "I:\\AI-Ready\\frontend\\apps\\pc-admin";
var vite_config_default = defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ["vue", "vue-router", "pinia", { "vue-i18n": ["useI18n"] }],
      dts: "src/auto-imports.d.ts"
    }),
    Components({
      resolvers: [
        AntDesignVueResolver({
          importStyle: false
          // 使用 CSS-in-JS（按需引入样式）
        })
      ],
      dts: "src/components.d.ts"
    }),
    // Gzip 压缩
    viteCompression({
      algorithm: "gzip",
      threshold: 10240,
      // 大于 10KB 的文件压缩
      deleteOriginFile: false
    }),
    // Brotli 压缩（更好的压缩率）
    viteCompression({
      algorithm: "brotliCompress",
      threshold: 10240,
      deleteOriginFile: false
    }),
    // 性能分析（仅生产环境）
    visualizer({
      open: false,
      gzipSize: true,
      brotliSize: true,
      filename: "dist/stats.html"
    }),
    // 图片优化
    viteImagemin({
      gifsicle: {
        optimizationLevel: 7,
        interlaced: false
      },
      optipng: {
        optimizationLevel: 7
      },
      mozjpeg: {
        quality: 80
      },
      pngquant: {
        quality: [0.8, 0.9],
        speed: 4
      },
      svgo: {
        plugins: [
          {
            name: "removeViewBox",
            active: false
          },
          {
            name: "removeEmptyAttrs",
            active: false
          }
        ]
      }
    })
  ],
  resolve: {
    alias: {
      "@": resolve(__vite_injected_original_dirname, "src"),
      "@ai-ready/components": resolve(__vite_injected_original_dirname, "../../packages/components/src")
    }
  },
  // CSS 配置
  css: {
    // CSS 预处理器配置
    preprocessorOptions: {
      less: {
        javascriptEnabled: true
      }
    },
    // 开发时的 CSS source map
    devSourcemap: false,
    // 模块化 CSS（更好的 tree-shaking）
    modules: {
      scopeBehaviour: "local"
    }
  },
  server: {
    port: 5656,
    host: true,
    // 允许外部访问
    open: false,
    // 不自动打开浏览器
    headers: {
      "Cache-Control": "no-store, no-cache, must-revalidate, max-age=0",
      "Pragma": "no-cache",
      "Expires": "0"
    },
    proxy: {
      "/api": {
        target: "http://localhost:5655",
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on("proxyRes", (proxyRes) => {
            proxyRes.headers["Cache-Control"] = "no-store, no-cache, must-revalidate, max-age=0";
            proxyRes.headers["Pragma"] = "no-cache";
            proxyRes.headers["Expires"] = "0";
          });
        }
      },
      "/ws": {
        target: "http://localhost:5655",
        ws: true,
        changeOrigin: true
      }
    },
    // 预热常用文件（提升开发启动速度）
    warmup: {
      clientFiles: [
        "./src/main.ts",
        "./src/App.vue",
        "./src/router/index.ts",
        "./src/stores/user.ts"
      ]
    }
  },
  // 构建优化
  build: {
    // 目标浏览器
    target: "es2020",
    // 输出目录
    outDir: "dist",
    // 静态资源目录
    assetsDir: "assets",
    // 是否生成 source map
    sourcemap: false,
    // chunk 大小警告阈值（降低到 500KB）
    chunkSizeWarningLimit: 500,
    // 跳过 TypeScript 类型检查
    typescript: {
      ignoreBuildErrors: true
    },
    // 压缩配置
    minify: "terser",
    terserOptions: {
      compress: {
        // 生产环境移除 console
        drop_console: true,
        drop_debugger: true,
        // 移除注释
        pure_funcs: ["console.log"]
      },
      format: {
        // 移除注释
        comments: false
      }
    },
    // Rollup 配置
    rollupOptions: {
      output: {
        // 入口文件名
        entryFileNames: "js/[name]-[hash].js",
        // chunk 文件名
        chunkFileNames: "js/[name]-[hash].js",
        // 静态资源文件名
        assetFileNames: (assetInfo) => {
          const info = assetInfo.name.split(".");
          const ext = info[info.length - 1];
          if (/png|jpe?g|gif|svg|webp|ico/.test(ext)) {
            return "images/[name]-[hash].[ext]";
          }
          if (/woff2?|eot|ttf|otf/.test(ext)) {
            return "fonts/[name]-[hash].[ext]";
          }
          if (/css/.test(ext)) {
            return "css/[name]-[hash].[ext]";
          }
          return "assets/[name]-[hash].[ext]";
        },
        // 代码分割策略
        manualChunks: (id) => {
          if (id.includes("node_modules")) {
            if (id.includes("ant-design-vue") || id.includes("@ant-design")) {
              return "antd";
            }
            if (id.includes("vue") || id.includes("vue-router") || id.includes("pinia")) {
              return "vue-vendor";
            }
            if (id.includes("lodash") || id.includes("dayjs") || id.includes("axios")) {
              return "utils-vendor";
            }
            if (id.includes("echarts") || id.includes("chart")) {
              return "charts-vendor";
            }
            return "vendor";
          }
          if (id.includes("src/views/erp")) {
            return "erp-module";
          }
          if (id.includes("src/views/crm")) {
            return "crm-module";
          }
          if (id.includes("src/views/system")) {
            return "system-module";
          }
          if (id.includes("src/views/dashboard")) {
            return "dashboard-module";
          }
          if (id.includes("src/api")) {
            return "api-module";
          }
          if (id.includes("src/components") || id.includes("src/layouts")) {
            return "common-components";
          }
          if (id.includes("src/utils") || id.includes("src/composables")) {
            return "common-utils";
          }
        }
      }
    }
  },
  // 优化依赖预构建
  optimizeDeps: {
    include: [
      "vue",
      "vue-router",
      "pinia",
      "vue-i18n",
      "ant-design-vue",
      "@ant-design/icons-vue",
      "axios",
      "dayjs",
      "lodash-es",
      "nprogress"
    ],
    exclude: []
  },
  // 实验性功能
  experimental: {
    renderBuiltUrl: (filename, { hostType }) => {
      if (hostType === "js") {
        return { runtime: `new URL(${filename}, import.meta.url).href` };
      }
      return { relative: filename };
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJJOlxcXFxBSS1SZWFkeVxcXFxmcm9udGVuZFxcXFxhcHBzXFxcXHBjLWFkbWluXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJJOlxcXFxBSS1SZWFkeVxcXFxmcm9udGVuZFxcXFxhcHBzXFxcXHBjLWFkbWluXFxcXHZpdGUuY29uZmlnLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9JOi9BSS1SZWFkeS9mcm9udGVuZC9hcHBzL3BjLWFkbWluL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXHJcbmltcG9ydCB7IHJlc29sdmUgfSBmcm9tICdwYXRoJ1xyXG5pbXBvcnQgQXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJ1xyXG5pbXBvcnQgQ29tcG9uZW50cyBmcm9tICd1bnBsdWdpbi12dWUtY29tcG9uZW50cy92aXRlJ1xyXG5pbXBvcnQgeyBBbnREZXNpZ25WdWVSZXNvbHZlciB9IGZyb20gJ3VucGx1Z2luLXZ1ZS1jb21wb25lbnRzL3Jlc29sdmVycydcclxuaW1wb3J0IHZpdGVDb21wcmVzc2lvbiBmcm9tICd2aXRlLXBsdWdpbi1jb21wcmVzc2lvbidcclxuaW1wb3J0IHsgdmlzdWFsaXplciB9IGZyb20gJ3JvbGx1cC1wbHVnaW4tdmlzdWFsaXplcidcclxuaW1wb3J0IHZpdGVJbWFnZW1pbiBmcm9tICd2aXRlLXBsdWdpbi1pbWFnZW1pbidcclxuXHJcbi8vIGh0dHBzOi8vdml0ZWpzLmRldi9jb25maWcvXHJcbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XHJcbiAgcGx1Z2luczogW1xyXG4gICAgdnVlKCksXHJcbiAgICBBdXRvSW1wb3J0KHtcclxuICAgICAgaW1wb3J0czogWyd2dWUnLCAndnVlLXJvdXRlcicsICdwaW5pYScsIHsgJ3Z1ZS1pMThuJzogWyd1c2VJMThuJ10gfV0sXHJcbiAgICAgIGR0czogJ3NyYy9hdXRvLWltcG9ydHMuZC50cycsXHJcbiAgICB9KSxcclxuICAgIENvbXBvbmVudHMoe1xyXG4gICAgICByZXNvbHZlcnM6IFtcclxuICAgICAgICBBbnREZXNpZ25WdWVSZXNvbHZlcih7XHJcbiAgICAgICAgICBpbXBvcnRTdHlsZTogZmFsc2UsIC8vIFx1NEY3Rlx1NzUyOCBDU1MtaW4tSlNcdUZGMDhcdTYzMDlcdTk3MDBcdTVGMTVcdTUxNjVcdTY4MzdcdTVGMEZcdUZGMDlcclxuICAgICAgICB9KSxcclxuICAgICAgXSxcclxuICAgICAgZHRzOiAnc3JjL2NvbXBvbmVudHMuZC50cycsXHJcbiAgICB9KSxcclxuICAgIC8vIEd6aXAgXHU1MzhCXHU3RjI5XHJcbiAgICB2aXRlQ29tcHJlc3Npb24oe1xyXG4gICAgICBhbGdvcml0aG06ICdnemlwJyxcclxuICAgICAgdGhyZXNob2xkOiAxMDI0MCwgLy8gXHU1OTI3XHU0RThFIDEwS0IgXHU3Njg0XHU2NTg3XHU0RUY2XHU1MzhCXHU3RjI5XHJcbiAgICAgIGRlbGV0ZU9yaWdpbkZpbGU6IGZhbHNlLFxyXG4gICAgfSksXHJcbiAgICAvLyBCcm90bGkgXHU1MzhCXHU3RjI5XHVGRjA4XHU2NkY0XHU1OTdEXHU3Njg0XHU1MzhCXHU3RjI5XHU3Mzg3XHVGRjA5XHJcbiAgICB2aXRlQ29tcHJlc3Npb24oe1xyXG4gICAgICBhbGdvcml0aG06ICdicm90bGlDb21wcmVzcycsXHJcbiAgICAgIHRocmVzaG9sZDogMTAyNDAsXHJcbiAgICAgIGRlbGV0ZU9yaWdpbkZpbGU6IGZhbHNlLFxyXG4gICAgfSksXHJcbiAgICAvLyBcdTYwMjdcdTgwRkRcdTUyMDZcdTY3OTBcdUZGMDhcdTRFQzVcdTc1MUZcdTRFQTdcdTczQUZcdTU4ODNcdUZGMDlcclxuICAgIHZpc3VhbGl6ZXIoe1xyXG4gICAgICBvcGVuOiBmYWxzZSxcclxuICAgICAgZ3ppcFNpemU6IHRydWUsXHJcbiAgICAgIGJyb3RsaVNpemU6IHRydWUsXHJcbiAgICAgIGZpbGVuYW1lOiAnZGlzdC9zdGF0cy5odG1sJyxcclxuICAgIH0pLFxyXG4gICAgLy8gXHU1NkZFXHU3MjQ3XHU0RjE4XHU1MzE2XHJcbiAgICB2aXRlSW1hZ2VtaW4oe1xyXG4gICAgICBnaWZzaWNsZToge1xyXG4gICAgICAgIG9wdGltaXphdGlvbkxldmVsOiA3LFxyXG4gICAgICAgIGludGVybGFjZWQ6IGZhbHNlLFxyXG4gICAgICB9LFxyXG4gICAgICBvcHRpcG5nOiB7XHJcbiAgICAgICAgb3B0aW1pemF0aW9uTGV2ZWw6IDcsXHJcbiAgICAgIH0sXHJcbiAgICAgIG1vempwZWc6IHtcclxuICAgICAgICBxdWFsaXR5OiA4MCxcclxuICAgICAgfSxcclxuICAgICAgcG5ncXVhbnQ6IHtcclxuICAgICAgICBxdWFsaXR5OiBbMC44LCAwLjldLFxyXG4gICAgICAgIHNwZWVkOiA0LFxyXG4gICAgICB9LFxyXG4gICAgICBzdmdvOiB7XHJcbiAgICAgICAgcGx1Z2luczogW1xyXG4gICAgICAgICAge1xyXG4gICAgICAgICAgICBuYW1lOiAncmVtb3ZlVmlld0JveCcsXHJcbiAgICAgICAgICAgIGFjdGl2ZTogZmFsc2UsXHJcbiAgICAgICAgICB9LFxyXG4gICAgICAgICAge1xyXG4gICAgICAgICAgICBuYW1lOiAncmVtb3ZlRW1wdHlBdHRycycsXHJcbiAgICAgICAgICAgIGFjdGl2ZTogZmFsc2UsXHJcbiAgICAgICAgICB9LFxyXG4gICAgICAgIF0sXHJcbiAgICAgIH0sXHJcbiAgICB9KSxcclxuICBdLFxyXG5cclxuICByZXNvbHZlOiB7XHJcbiAgICBhbGlhczoge1xyXG4gICAgICAnQCc6IHJlc29sdmUoX19kaXJuYW1lLCAnc3JjJyksXHJcbiAgICAgICdAYWktcmVhZHkvY29tcG9uZW50cyc6IHJlc29sdmUoX19kaXJuYW1lLCAnLi4vLi4vcGFja2FnZXMvY29tcG9uZW50cy9zcmMnKSxcclxuICAgIH0sXHJcbiAgfSxcclxuXHJcbiAgLy8gQ1NTIFx1OTE0RFx1N0Y2RVxyXG4gIGNzczoge1xyXG4gICAgLy8gQ1NTIFx1OTg4NFx1NTkwNFx1NzQwNlx1NTY2OFx1OTE0RFx1N0Y2RVxyXG4gICAgcHJlcHJvY2Vzc29yT3B0aW9uczoge1xyXG4gICAgICBsZXNzOiB7XHJcbiAgICAgICAgamF2YXNjcmlwdEVuYWJsZWQ6IHRydWUsXHJcbiAgICAgIH0sXHJcbiAgICB9LFxyXG4gICAgLy8gXHU1RjAwXHU1M0QxXHU2NUY2XHU3Njg0IENTUyBzb3VyY2UgbWFwXHJcbiAgICBkZXZTb3VyY2VtYXA6IGZhbHNlLFxyXG4gICAgLy8gXHU2QTIxXHU1NzU3XHU1MzE2IENTU1x1RkYwOFx1NjZGNFx1NTk3RFx1NzY4NCB0cmVlLXNoYWtpbmdcdUZGMDlcclxuICAgIG1vZHVsZXM6IHtcclxuICAgICAgc2NvcGVCZWhhdmlvdXI6ICdsb2NhbCcsXHJcbiAgICB9LFxyXG4gIH0sXHJcblxyXG4gIHNlcnZlcjoge1xyXG4gICAgcG9ydDogNTY1NixcclxuICAgIGhvc3Q6IHRydWUsIC8vIFx1NTE0MVx1OEJCOFx1NTkxNlx1OTBFOFx1OEJCRlx1OTVFRVxyXG4gICAgb3BlbjogZmFsc2UsIC8vIFx1NEUwRFx1ODFFQVx1NTJBOFx1NjI1M1x1NUYwMFx1NkQ0Rlx1ODlDOFx1NTY2OFxyXG4gICAgaGVhZGVyczoge1xyXG4gICAgICAnQ2FjaGUtQ29udHJvbCc6ICduby1zdG9yZSwgbm8tY2FjaGUsIG11c3QtcmV2YWxpZGF0ZSwgbWF4LWFnZT0wJyxcclxuICAgICAgJ1ByYWdtYSc6ICduby1jYWNoZScsXHJcbiAgICAgICdFeHBpcmVzJzogJzAnXHJcbiAgICB9LFxyXG4gICAgcHJveHk6IHtcclxuICAgICAgJy9hcGknOiB7XHJcbiAgICAgICAgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo1NjU1JyxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXHJcbiAgICAgICAgY29uZmlndXJlOiAocHJveHkpID0+IHtcclxuICAgICAgICAgIHByb3h5Lm9uKCdwcm94eVJlcycsIChwcm94eVJlcykgPT4ge1xyXG4gICAgICAgICAgICAvLyBcdTc4NkVcdTRGRERcdTYyNDBcdTY3MDlcdTRFRTNcdTc0MDZcdTU0Q0RcdTVFOTRcdTkwRkRcdTVFMjZcdTk2MzJcdTdGMTNcdTVCNThcdTU5MzRcdUZGMENcdTkwN0ZcdTUxNERcdTZENEZcdTg5QzhcdTU2NjhcdTdGMTNcdTVCNThcdTk1MTlcdThCRUZcdTU0Q0RcdTVFOTRcclxuICAgICAgICAgICAgcHJveHlSZXMuaGVhZGVyc1snQ2FjaGUtQ29udHJvbCddID0gJ25vLXN0b3JlLCBuby1jYWNoZSwgbXVzdC1yZXZhbGlkYXRlLCBtYXgtYWdlPTAnO1xyXG4gICAgICAgICAgICBwcm94eVJlcy5oZWFkZXJzWydQcmFnbWEnXSA9ICduby1jYWNoZSc7XHJcbiAgICAgICAgICAgIHByb3h5UmVzLmhlYWRlcnNbJ0V4cGlyZXMnXSA9ICcwJztcclxuICAgICAgICAgIH0pO1xyXG4gICAgICAgIH0sXHJcbiAgICAgIH0sXHJcbiAgICAgICcvd3MnOiB7XHJcbiAgICAgICAgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo1NjU1JyxcclxuICAgICAgICB3czogdHJ1ZSxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXHJcbiAgICAgIH0sXHJcbiAgICB9LFxyXG4gICAgLy8gXHU5ODg0XHU3MEVEXHU1RTM4XHU3NTI4XHU2NTg3XHU0RUY2XHVGRjA4XHU2M0QwXHU1MzQ3XHU1RjAwXHU1M0QxXHU1NDJGXHU1MkE4XHU5MDFGXHU1RUE2XHVGRjA5XHJcbiAgICB3YXJtdXA6IHtcclxuICAgICAgY2xpZW50RmlsZXM6IFtcclxuICAgICAgICAnLi9zcmMvbWFpbi50cycsXHJcbiAgICAgICAgJy4vc3JjL0FwcC52dWUnLFxyXG4gICAgICAgICcuL3NyYy9yb3V0ZXIvaW5kZXgudHMnLFxyXG4gICAgICAgICcuL3NyYy9zdG9yZXMvdXNlci50cycsXHJcbiAgICAgIF0sXHJcbiAgICB9LFxyXG4gIH0sXHJcblxyXG4gIC8vIFx1Njc4NFx1NUVGQVx1NEYxOFx1NTMxNlxyXG4gIGJ1aWxkOiB7XHJcbiAgICAvLyBcdTc2RUVcdTY4MDdcdTZENEZcdTg5QzhcdTU2NjhcclxuICAgIHRhcmdldDogJ2VzMjAyMCcsXHJcblxyXG4gICAgLy8gXHU4RjkzXHU1MUZBXHU3NkVFXHU1RjU1XHJcbiAgICBvdXREaXI6ICdkaXN0JyxcclxuXHJcbiAgICAvLyBcdTk3NTlcdTYwMDFcdThENDRcdTZFOTBcdTc2RUVcdTVGNTVcclxuICAgIGFzc2V0c0RpcjogJ2Fzc2V0cycsXHJcblxyXG4gICAgLy8gXHU2NjJGXHU1NDI2XHU3NTFGXHU2MjEwIHNvdXJjZSBtYXBcclxuICAgIHNvdXJjZW1hcDogZmFsc2UsXHJcblxyXG4gICAgLy8gY2h1bmsgXHU1OTI3XHU1QzBGXHU4QjY2XHU1NDRBXHU5NjA4XHU1MDNDXHVGRjA4XHU5NjREXHU0RjRFXHU1MjMwIDUwMEtCXHVGRjA5XHJcbiAgICBjaHVua1NpemVXYXJuaW5nTGltaXQ6IDUwMCxcclxuXHJcbiAgICAvLyBcdThERjNcdThGQzcgVHlwZVNjcmlwdCBcdTdDN0JcdTU3OEJcdTY4QzBcdTY3RTVcclxuICAgIHR5cGVzY3JpcHQ6IHtcclxuICAgICAgaWdub3JlQnVpbGRFcnJvcnM6IHRydWUsXHJcbiAgICB9LFxyXG5cclxuICAgIC8vIFx1NTM4Qlx1N0YyOVx1OTE0RFx1N0Y2RVxyXG4gICAgbWluaWZ5OiAndGVyc2VyJyxcclxuICAgIHRlcnNlck9wdGlvbnM6IHtcclxuICAgICAgY29tcHJlc3M6IHtcclxuICAgICAgICAvLyBcdTc1MUZcdTRFQTdcdTczQUZcdTU4ODNcdTc5RkJcdTk2NjQgY29uc29sZVxyXG4gICAgICAgIGRyb3BfY29uc29sZTogdHJ1ZSxcclxuICAgICAgICBkcm9wX2RlYnVnZ2VyOiB0cnVlLFxyXG4gICAgICAgIC8vIFx1NzlGQlx1OTY2NFx1NkNFOFx1OTFDQVxyXG4gICAgICAgIHB1cmVfZnVuY3M6IFsnY29uc29sZS5sb2cnXSxcclxuICAgICAgfSxcclxuICAgICAgZm9ybWF0OiB7XHJcbiAgICAgICAgLy8gXHU3OUZCXHU5NjY0XHU2Q0U4XHU5MUNBXHJcbiAgICAgICAgY29tbWVudHM6IGZhbHNlLFxyXG4gICAgICB9LFxyXG4gICAgfSxcclxuXHJcbiAgICAvLyBSb2xsdXAgXHU5MTREXHU3RjZFXHJcbiAgICByb2xsdXBPcHRpb25zOiB7XHJcbiAgICAgIG91dHB1dDoge1xyXG4gICAgICAgIC8vIFx1NTE2NVx1NTNFM1x1NjU4N1x1NEVGNlx1NTQwRFxyXG4gICAgICAgIGVudHJ5RmlsZU5hbWVzOiAnanMvW25hbWVdLVtoYXNoXS5qcycsXHJcblxyXG4gICAgICAgIC8vIGNodW5rIFx1NjU4N1x1NEVGNlx1NTQwRFxyXG4gICAgICAgIGNodW5rRmlsZU5hbWVzOiAnanMvW25hbWVdLVtoYXNoXS5qcycsXHJcblxyXG4gICAgICAgIC8vIFx1OTc1OVx1NjAwMVx1OEQ0NFx1NkU5MFx1NjU4N1x1NEVGNlx1NTQwRFxyXG4gICAgICAgIGFzc2V0RmlsZU5hbWVzOiAoYXNzZXRJbmZvKSA9PiB7XHJcbiAgICAgICAgICBjb25zdCBpbmZvID0gYXNzZXRJbmZvLm5hbWUuc3BsaXQoJy4nKVxyXG4gICAgICAgICAgY29uc3QgZXh0ID0gaW5mb1tpbmZvLmxlbmd0aCAtIDFdXHJcblxyXG4gICAgICAgICAgaWYgKC9wbmd8anBlP2d8Z2lmfHN2Z3x3ZWJwfGljby8udGVzdChleHQpKSB7XHJcbiAgICAgICAgICAgIHJldHVybiAnaW1hZ2VzL1tuYW1lXS1baGFzaF0uW2V4dF0nXHJcbiAgICAgICAgICB9XHJcbiAgICAgICAgICBpZiAoL3dvZmYyP3xlb3R8dHRmfG90Zi8udGVzdChleHQpKSB7XHJcbiAgICAgICAgICAgIHJldHVybiAnZm9udHMvW25hbWVdLVtoYXNoXS5bZXh0XSdcclxuICAgICAgICAgIH1cclxuICAgICAgICAgIGlmICgvY3NzLy50ZXN0KGV4dCkpIHtcclxuICAgICAgICAgICAgcmV0dXJuICdjc3MvW25hbWVdLVtoYXNoXS5bZXh0XSdcclxuICAgICAgICAgIH1cclxuICAgICAgICAgIHJldHVybiAnYXNzZXRzL1tuYW1lXS1baGFzaF0uW2V4dF0nXHJcbiAgICAgICAgfSxcclxuXHJcbiAgICAgICAgLy8gXHU0RUUzXHU3ODAxXHU1MjA2XHU1MjcyXHU3QjU2XHU3NTY1XHJcbiAgICAgICAgbWFudWFsQ2h1bmtzOiAoaWQpID0+IHtcclxuICAgICAgICAgIC8vIFx1N0IyQ1x1NEUwOVx1NjVCOVx1NUU5M1x1NTIwNlx1NTI3MlxyXG4gICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdub2RlX21vZHVsZXMnKSkge1xyXG4gICAgICAgICAgICAvLyBBbnQgRGVzaWduIFZ1ZSBcdTc2RjhcdTUxNzNcclxuICAgICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdhbnQtZGVzaWduLXZ1ZScpIHx8IGlkLmluY2x1ZGVzKCdAYW50LWRlc2lnbicpKSB7XHJcbiAgICAgICAgICAgICAgcmV0dXJuICdhbnRkJ1xyXG4gICAgICAgICAgICB9XHJcblxyXG4gICAgICAgICAgICAvLyBWdWUgXHU2ODM4XHU1RkMzXHU1RTkzXHJcbiAgICAgICAgICAgIGlmIChpZC5pbmNsdWRlcygndnVlJykgfHwgaWQuaW5jbHVkZXMoJ3Z1ZS1yb3V0ZXInKSB8fCBpZC5pbmNsdWRlcygncGluaWEnKSkge1xyXG4gICAgICAgICAgICAgIHJldHVybiAndnVlLXZlbmRvcidcclxuICAgICAgICAgICAgfVxyXG5cclxuICAgICAgICAgICAgLy8gXHU1REU1XHU1MTc3XHU1RTkzXHJcbiAgICAgICAgICAgIGlmIChpZC5pbmNsdWRlcygnbG9kYXNoJykgfHwgaWQuaW5jbHVkZXMoJ2RheWpzJykgfHwgaWQuaW5jbHVkZXMoJ2F4aW9zJykpIHtcclxuICAgICAgICAgICAgICByZXR1cm4gJ3V0aWxzLXZlbmRvcidcclxuICAgICAgICAgICAgfVxyXG5cclxuICAgICAgICAgICAgLy8gXHU1NkZFXHU4ODY4XHU1RTkzXHVGRjA4XHU1OTgyXHU2NzlDXHU0RjdGXHU3NTI4XHVGRjA5XHJcbiAgICAgICAgICAgIGlmIChpZC5pbmNsdWRlcygnZWNoYXJ0cycpIHx8IGlkLmluY2x1ZGVzKCdjaGFydCcpKSB7XHJcbiAgICAgICAgICAgICAgcmV0dXJuICdjaGFydHMtdmVuZG9yJ1xyXG4gICAgICAgICAgICB9XHJcblxyXG4gICAgICAgICAgICAvLyBcdTUxNzZcdTRFRDZcdTdCMkNcdTRFMDlcdTY1QjlcdTVFOTNcclxuICAgICAgICAgICAgcmV0dXJuICd2ZW5kb3InXHJcbiAgICAgICAgICB9XHJcblxyXG4gICAgICAgICAgLy8gXHU0RTFBXHU1MkExXHU2QTIxXHU1NzU3XHU1MjA2XHU1MjcyXHJcbiAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ3NyYy92aWV3cy9lcnAnKSkge1xyXG4gICAgICAgICAgICByZXR1cm4gJ2VycC1tb2R1bGUnXHJcbiAgICAgICAgICB9XHJcbiAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ3NyYy92aWV3cy9jcm0nKSkge1xyXG4gICAgICAgICAgICByZXR1cm4gJ2NybS1tb2R1bGUnXHJcbiAgICAgICAgICB9XHJcbiAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ3NyYy92aWV3cy9zeXN0ZW0nKSkge1xyXG4gICAgICAgICAgICByZXR1cm4gJ3N5c3RlbS1tb2R1bGUnXHJcbiAgICAgICAgICB9XHJcbiAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ3NyYy92aWV3cy9kYXNoYm9hcmQnKSkge1xyXG4gICAgICAgICAgICByZXR1cm4gJ2Rhc2hib2FyZC1tb2R1bGUnXHJcbiAgICAgICAgICB9XHJcblxyXG4gICAgICAgICAgLy8gQVBJIFx1NkEyMVx1NTc1N1xyXG4gICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdzcmMvYXBpJykpIHtcclxuICAgICAgICAgICAgcmV0dXJuICdhcGktbW9kdWxlJ1xyXG4gICAgICAgICAgfVxyXG5cclxuICAgICAgICAgIC8vIFx1NTE2Q1x1NTE3MVx1N0VDNFx1NEVGNlxyXG4gICAgICAgICAgaWYgKGlkLmluY2x1ZGVzKCdzcmMvY29tcG9uZW50cycpIHx8IGlkLmluY2x1ZGVzKCdzcmMvbGF5b3V0cycpKSB7XHJcbiAgICAgICAgICAgIHJldHVybiAnY29tbW9uLWNvbXBvbmVudHMnXHJcbiAgICAgICAgICB9XHJcblxyXG4gICAgICAgICAgLy8gXHU1REU1XHU1MTc3XHU1MUZEXHU2NTcwXHJcbiAgICAgICAgICBpZiAoaWQuaW5jbHVkZXMoJ3NyYy91dGlscycpIHx8IGlkLmluY2x1ZGVzKCdzcmMvY29tcG9zYWJsZXMnKSkge1xyXG4gICAgICAgICAgICByZXR1cm4gJ2NvbW1vbi11dGlscydcclxuICAgICAgICAgIH1cclxuICAgICAgICB9LFxyXG4gICAgICB9LFxyXG4gICAgfSxcclxuICB9LFxyXG5cclxuICAvLyBcdTRGMThcdTUzMTZcdTRGOURcdThENTZcdTk4ODRcdTY3ODRcdTVFRkFcclxuICBvcHRpbWl6ZURlcHM6IHtcclxuICAgIGluY2x1ZGU6IFtcclxuICAgICAgJ3Z1ZScsXHJcbiAgICAgICd2dWUtcm91dGVyJyxcclxuICAgICAgJ3BpbmlhJyxcclxuICAgICAgJ3Z1ZS1pMThuJyxcclxuICAgICAgJ2FudC1kZXNpZ24tdnVlJyxcclxuICAgICAgJ0BhbnQtZGVzaWduL2ljb25zLXZ1ZScsXHJcbiAgICAgICdheGlvcycsXHJcbiAgICAgICdkYXlqcycsXHJcbiAgICAgICdsb2Rhc2gtZXMnLFxyXG4gICAgICAnbnByb2dyZXNzJyxcclxuICAgIF0sXHJcbiAgICBleGNsdWRlOiBbXSxcclxuICB9LFxyXG5cclxuICAvLyBcdTVCOUVcdTlBOENcdTYwMjdcdTUyOUZcdTgwRkRcclxuICBleHBlcmltZW50YWw6IHtcclxuICAgIHJlbmRlckJ1aWx0VXJsOiAoZmlsZW5hbWUsIHsgaG9zdFR5cGUgfSkgPT4ge1xyXG4gICAgICBpZiAoaG9zdFR5cGUgPT09ICdqcycpIHtcclxuICAgICAgICByZXR1cm4geyBydW50aW1lOiBgbmV3IFVSTCgke2ZpbGVuYW1lfSwgaW1wb3J0Lm1ldGEudXJsKS5ocmVmYCB9XHJcbiAgICAgIH1cclxuICAgICAgcmV0dXJuIHsgcmVsYXRpdmU6IGZpbGVuYW1lIH1cclxuICAgIH0sXHJcbiAgfSxcclxufSkiXSwKICAibWFwcGluZ3MiOiAiO0FBQWtTLFNBQVMsb0JBQW9CO0FBQy9ULE9BQU8sU0FBUztBQUNoQixTQUFTLGVBQWU7QUFDeEIsT0FBTyxnQkFBZ0I7QUFDdkIsT0FBTyxnQkFBZ0I7QUFDdkIsU0FBUyw0QkFBNEI7QUFDckMsT0FBTyxxQkFBcUI7QUFDNUIsU0FBUyxrQkFBa0I7QUFDM0IsT0FBTyxrQkFBa0I7QUFSekIsSUFBTSxtQ0FBbUM7QUFXekMsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUztBQUFBLElBQ1AsSUFBSTtBQUFBLElBQ0osV0FBVztBQUFBLE1BQ1QsU0FBUyxDQUFDLE9BQU8sY0FBYyxTQUFTLEVBQUUsWUFBWSxDQUFDLFNBQVMsRUFBRSxDQUFDO0FBQUEsTUFDbkUsS0FBSztBQUFBLElBQ1AsQ0FBQztBQUFBLElBQ0QsV0FBVztBQUFBLE1BQ1QsV0FBVztBQUFBLFFBQ1QscUJBQXFCO0FBQUEsVUFDbkIsYUFBYTtBQUFBO0FBQUEsUUFDZixDQUFDO0FBQUEsTUFDSDtBQUFBLE1BQ0EsS0FBSztBQUFBLElBQ1AsQ0FBQztBQUFBO0FBQUEsSUFFRCxnQkFBZ0I7QUFBQSxNQUNkLFdBQVc7QUFBQSxNQUNYLFdBQVc7QUFBQTtBQUFBLE1BQ1gsa0JBQWtCO0FBQUEsSUFDcEIsQ0FBQztBQUFBO0FBQUEsSUFFRCxnQkFBZ0I7QUFBQSxNQUNkLFdBQVc7QUFBQSxNQUNYLFdBQVc7QUFBQSxNQUNYLGtCQUFrQjtBQUFBLElBQ3BCLENBQUM7QUFBQTtB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
