import type { Preview } from '@storybook/vue3'
import { setup } from '@storybook/vue3'
import { ref, watch } from 'vue'
import '../src/styles/variables.scss'
import '../src/styles/index.scss'

/**
 * Theme decorator — wraps every story in a theme-aware container.
 * Reads the global "theme" toolbar item and applies `data-theme` to the root.
 */
const themeProvider = (story, context) => {
  const theme = context.globals.theme || 'light'

  // Apply data-theme attribute to the story root element
  return {
    components: { story },
    setup() {
      const currentTheme = ref(theme)

      watch(
        () => context.globals.theme,
        (newTheme) => {
          currentTheme.value = newTheme || 'light'
          // Set data-theme at document level so CSS variables resolve
          document.documentElement.setAttribute('data-theme', currentTheme.value)
        },
        { immediate: true }
      )

      // Apply theme immediately on setup
      document.documentElement.setAttribute('data-theme', theme)

      return { currentTheme }
    },
    template: `
      <div :data-theme="currentTheme" style="min-height: 100vh">
        <story />
      </div>
    `,
  }
}

// Register the essentials (actions, controls, etc.)
setup((app) => {
  // Global component registration or plugins can go here
})

const preview: Preview = {
  decorators: [themeProvider],

  globalTypes: {
    theme: {
      name: 'Theme',
      description: '切换亮色 / 暗色主题',
      defaultValue: 'light',
      toolbar: {
        icon: 'circlehollow',
        items: [
          { value: 'light', icon: 'sun', title: '亮色主题' },
          { value: 'dark', icon: 'moon', title: '暗色主题' },
        ],
        showName: true,
        dynamicTitle: true,
      },
    },
  },

  parameters: {
    actions: { argTypesRegex: '^on[A-Z].*' },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
      expanded: true,
      sort: 'requiredFirst',
    },
    backgrounds: {
      default: 'light',
      values: [
        { name: 'light', value: '#ffffff' },
        { name: 'dark', value: '#141414' },
        { name: 'layout', value: '#f2f3f5' },
        { name: 'brand-light', value: '#ecf5ff' },
      ],
      grid: {
        cellSize: 8,
        opacity: 0.5,
      },
    },
    viewport: {
      viewports: {
        desktop: {
          name: 'Desktop',
          styles: { width: '1440px', height: '900px' },
        },
        tablet: {
          name: 'Tablet',
          styles: { width: '768px', height: '1024px' },
        },
        mobile: {
          name: 'Mobile',
          styles: { width: '375px', height: '812px' },
        },
        wide: {
          name: 'Ultra Wide',
          styles: { width: '1920px', height: '1080px' },
        },
      },
      defaultViewport: 'desktop',
    },
    layout: 'padded',
    docs: {
      toc: true,
      source: { state: 'open' },
    },
    a11y: {
      config: {
        rules: [
          { id: 'color-contrast', enabled: true },
        ],
      },
    },
  },

  tags: ['autodocs'],
}

export default preview
