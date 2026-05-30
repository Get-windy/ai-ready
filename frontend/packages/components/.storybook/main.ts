import type { StorybookConfig } from '@storybook/vue3-vite'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

const config: StorybookConfig = {
  stories: ['../src/**/*.stories.@(ts|js)'],
  addons: [
    '@storybook/addon-essentials',
    '@storybook/addon-interactions',
    '@storybook/addon-a11y',
    '@storybook/addon-links',
  ],
  framework: {
    name: '@storybook/vue3-vite',
    options: {},
  },
  core: {
    disableTelemetry: true,
  },
  docs: {
    autodocs: 'tag',
    defaultName: 'Documentation',
  },
  async viteFinal(config, { configType }) {
    // Merge existing aliases with our own
    const existingAliases = config.resolve?.alias ?? {}

    return {
      ...config,
      resolve: {
        ...config.resolve,
        alias: {
          ...(Array.isArray(existingAliases) ? {} : existingAliases),
          '@': resolve(__dirname, '../src'),
        },
      },
      css: {
        ...config.css,
        preprocessorOptions: {
          ...config.css?.preprocessorOptions,
          scss: {
            ...(config.css?.preprocessorOptions as Record<string, unknown> | undefined)
              ?.scss as Record<string, unknown> | undefined,
            additionalData: `@use "${resolve(__dirname, '../src/styles/variables.scss').replace(/\\/g, '/')}" as *;\n`,
          },
        },
      },
    }
  },
}

export default config
