// vite.config.ts
import { defineConfig } from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/vite@5.4.21_@types+node@20.19.41_sass-embedded@1.100.0_terser@5.48.0/node_modules/vite/dist/node/index.js";
import vue from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/@vitejs+plugin-vue@5.2.4_vite@5.4.21_vue@3.5.34/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import Components from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/unplugin-vue-components@0.26.0_vue@3.5.34/node_modules/unplugin-vue-components/dist/vite.js";
import { VantResolver } from "file:///I:/AI-Ready/frontend/node_modules/.pnpm/@vant+auto-import-resolver@1.3.0/node_modules/@vant/auto-import-resolver/dist/index.js";
import { resolve } from "path";
var __vite_injected_original_dirname = "I:\\AI-Ready\\frontend\\apps\\pda-warehouse";
var vite_config_default = defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [VantResolver()]
    })
  ],
  resolve: {
    alias: {
      "@": resolve(__vite_injected_original_dirname, "src"),
      "@ai-ready/components": resolve(__vite_injected_original_dirname, "../../packages/components/src")
    }
  },
  server: {
    port: 3003,
    host: true
  },
  build: {
    outDir: "dist",
    sourcemap: true
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJJOlxcXFxBSS1SZWFkeVxcXFxmcm9udGVuZFxcXFxhcHBzXFxcXHBkYS13YXJlaG91c2VcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkk6XFxcXEFJLVJlYWR5XFxcXGZyb250ZW5kXFxcXGFwcHNcXFxccGRhLXdhcmVob3VzZVxcXFx2aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vSTovQUktUmVhZHkvZnJvbnRlbmQvYXBwcy9wZGEtd2FyZWhvdXNlL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXHJcbmltcG9ydCBDb21wb25lbnRzIGZyb20gJ3VucGx1Z2luLXZ1ZS1jb21wb25lbnRzL3ZpdGUnXHJcbmltcG9ydCB7IFZhbnRSZXNvbHZlciB9IGZyb20gJ0B2YW50L2F1dG8taW1wb3J0LXJlc29sdmVyJ1xyXG5pbXBvcnQgeyByZXNvbHZlIH0gZnJvbSAncGF0aCdcclxuXHJcbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XHJcbiAgcGx1Z2luczogW1xyXG4gICAgdnVlKCksXHJcbiAgICBDb21wb25lbnRzKHtcclxuICAgICAgcmVzb2x2ZXJzOiBbVmFudFJlc29sdmVyKCldXHJcbiAgICB9KVxyXG4gIF0sXHJcbiAgcmVzb2x2ZToge1xyXG4gICAgYWxpYXM6IHtcclxuICAgICAgJ0AnOiByZXNvbHZlKF9fZGlybmFtZSwgJ3NyYycpLFxyXG4gICAgICAnQGFpLXJlYWR5L2NvbXBvbmVudHMnOiByZXNvbHZlKF9fZGlybmFtZSwgJy4uLy4uL3BhY2thZ2VzL2NvbXBvbmVudHMvc3JjJyksXHJcbiAgICB9XHJcbiAgfSxcclxuICBzZXJ2ZXI6IHtcclxuICAgIHBvcnQ6IDMwMDMsXHJcbiAgICBob3N0OiB0cnVlXHJcbiAgfSxcclxuICBidWlsZDoge1xyXG4gICAgb3V0RGlyOiAnZGlzdCcsXHJcbiAgICBzb3VyY2VtYXA6IHRydWVcclxuICB9XHJcbn0pIl0sCiAgIm1hcHBpbmdzIjogIjtBQUFpVCxTQUFTLG9CQUFvQjtBQUM5VSxPQUFPLFNBQVM7QUFDaEIsT0FBTyxnQkFBZ0I7QUFDdkIsU0FBUyxvQkFBb0I7QUFDN0IsU0FBUyxlQUFlO0FBSnhCLElBQU0sbUNBQW1DO0FBTXpDLElBQU8sc0JBQVEsYUFBYTtBQUFBLEVBQzFCLFNBQVM7QUFBQSxJQUNQLElBQUk7QUFBQSxJQUNKLFdBQVc7QUFBQSxNQUNULFdBQVcsQ0FBQyxhQUFhLENBQUM7QUFBQSxJQUM1QixDQUFDO0FBQUEsRUFDSDtBQUFBLEVBQ0EsU0FBUztBQUFBLElBQ1AsT0FBTztBQUFBLE1BQ0wsS0FBSyxRQUFRLGtDQUFXLEtBQUs7QUFBQSxNQUM3Qix3QkFBd0IsUUFBUSxrQ0FBVywrQkFBK0I7QUFBQSxJQUM1RTtBQUFBLEVBQ0Y7QUFBQSxFQUNBLFFBQVE7QUFBQSxJQUNOLE1BQU07QUFBQSxJQUNOLE1BQU07QUFBQSxFQUNSO0FBQUEsRUFDQSxPQUFPO0FBQUEsSUFDTCxRQUFRO0FBQUEsSUFDUixXQUFXO0FBQUEsRUFDYjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
