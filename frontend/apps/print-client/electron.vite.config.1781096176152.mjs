// electron.vite.config.ts
import { defineConfig } from "electron-vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";
var __electron_vite_injected_dirname = "I:\\AI-Ready\\frontend\\apps\\print-client";
var electron_vite_config_default = defineConfig({
  main: {
    build: {
      outDir: "dist/main",
      rollupOptions: {
        input: {
          index: resolve(__electron_vite_injected_dirname, "src/main/index.ts")
        },
        external: ["ws"]
      }
    }
  },
  preload: {
    build: {
      outDir: "dist/preload",
      rollupOptions: {
        input: {
          index: resolve(__electron_vite_injected_dirname, "src/preload/index.ts")
        }
      }
    }
  },
  renderer: {
    build: {
      outDir: "dist/renderer",
      rollupOptions: {
        input: {
          index: resolve(__electron_vite_injected_dirname, "src/renderer/index.html")
        }
      }
    },
    plugins: [vue()],
    resolve: {
      alias: {
        "@": resolve(__electron_vite_injected_dirname, "src/renderer")
      }
    }
  }
});
export {
  electron_vite_config_default as default
};
