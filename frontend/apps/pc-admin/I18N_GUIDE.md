# AI-Ready 国际化 (i18n) 使用指南

## 概述

AI-Ready 项目支持多语言国际化，包括简体中文、英语、日语和韩语。本指南将帮助您了解如何使用和扩展国际化功能。

## 支持的语言

| 语言 | 语言代码 | 图标 | 本地名称 |
|------|---------|------|---------|
| 简体中文 | zh-CN | 🇨🇳 | 简体中文 |
| 英语 | en-US | 🇺🇸 | English |
| 日语 | ja-JP | 🇯🇵 | 日本語 |
| 韩语 | ko-KR | 🇰🇷 | 한국어 |

## 快速开始

### 1. 在组件中使用翻译

```vue
<template>
  <div>
    <h1>{{ t('login.title') }}</h1>
    <button>{{ t('common.confirm') }}</button>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>
```

### 2. 切换语言

```typescript
import { setI18nLanguage } from '@/locales'

// 切换到英语
await setI18nLanguage('en-US')

// 切换到日语
await setI18nLanguage('ja-JP')
```

### 3. 使用语言切换器组件

```vue
<template>
  <div>
    <LocaleSwitcher />
  </div>
</template>

<script setup lang="ts">
import LocaleSwitcher from '@/components/LocaleSwitcher.vue'
</script>
```

## 核心 API

### 获取当前语言

```typescript
import { getCurrentLocale } from '@/locales'

const currentLocale = getCurrentLocale()
console.log(currentLocale) // 'zh-CN'
```

### 获取语言配置信息

```typescript
import { getLocaleInfo } from '@/locales'

const zhInfo = getLocaleInfo('zh-CN')
console.log(zhInfo)
// {
//   value: 'zh-CN',
//   label: '简体中文',
//   labelNative: '简体中文',
//   flag: '🇨🇳',
//   lang: 'zh-CN'
// }
```

### 获取支持的语言列表

```typescript
import { getSupportedLocales } from '@/locales'

const locales = getSupportedLocales()
console.log(locales)
// [
//   { code: 'zh-CN', name: '简体中文', icon: '🇨🇳' },
//   { code: 'en-US', name: 'English', icon: '🇺🇸' },
//   { code: 'ja-JP', name: '日本語', icon: '🇯🇵' },
//   { code: 'ko-KR', name: '한국어', icon: '🇰🇷' }
// ]
```

### 监听语言变化

```typescript
// 监听语言切换前事件
window.addEventListener('locale:before-change', (event) => {
  const { locale } = event.detail
  console.log('Language is about to change to:', locale)
})

// 监听语言切换完成事件
window.addEventListener('locale:changed', (event) => {
  const { locale } = event.detail
  console.log('Language changed to:', locale)
})
```

## 高级功能

### 1. 日期格式化

```vue
<template>
  <div>{{ d(new Date(), 'long') }}</div>
  <!-- 输出: 2026年4月11日 星期六 (中文) -->
  <!-- 输出: Saturday, April 11, 2026 (英文) -->
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { d } = useI18n()
</script>
```

### 2. 数字格式化

```vue
<template>
  <div>{{ n(1234.56, 'currency') }}</div>
  <!-- 输出: ¥1,234.56 (中文) -->
  <!-- 输出: $1,234.56 (英文) -->
  
  <div>{{ n(0.85, 'percent') }}</div>
  <!-- 输出: 85% -->
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { n } = useI18n()
</script>
```

### 3. 复数处理

```vue
<template>
  <div>{{ tc('message.count', count) }}</div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'

const { tc } = useI18n()
const count = ref(5)
</script>
```

### 4. 动态参数

```vue
<template>
  <div>{{ t('greeting', { name: 'John' }) }}</div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>
```

语言包定义：
```typescript
{
  greeting: 'Hello, {name}!'
}
```

### 5. SEO 优化

```typescript
import { seoUtils } from '@/locales'

// 生成 hreflang 标签（用于搜索引擎）
const hreflangTags = seoUtils.generateAllHreflangs()
console.log(hreflangTags)
/*
<link rel="alternate" hreflang="zh-CN" href="..." />
<link rel="alternate" hreflang="en" href="..." />
<link rel="alternate" hreflang="ja" href="..." />
<link rel="alternate" hreflang="ko" href="..." />
*/

// 更新页面标题
seoUtils.updateTitle('Dashboard', 'zh-CN')
```

## 语言包结构

### 语言包文件位置

```
src/locales/
├── index.ts           # 国际化配置和 API
├── locales/           # 语言包目录
│   ├── zh-CN.ts      # 简体中文
│   ├── en-US.ts      # 英语
│   ├── ja-JP.ts      # 日语
│   └── ko-KR.ts      # 韩语
```

### 语言包组织

每个语言包按模块组织：

```typescript
export default {
  // 通用
  common: {
    confirm: '确认',
    cancel: '取消',
    // ...
  },
  
  // 导航菜单
  menu: {
    dashboard: '仪表盘',
    user: '用户管理',
    // ...
  },
  
  // 登录
  login: {
    title: 'AI-Ready 智企连',
    // ...
  },
  
  // 用户管理
  user: {
    title: '用户管理',
    // ...
  },
  
  // ... 其他模块
}
```

## 添加新语言

### 步骤 1：创建语言包文件

在 `src/locales/locales/` 目录下创建新文件，例如 `fr-FR.ts`：

```typescript
export default {
  common: {
    confirm: 'Confirmer',
    cancel: 'Annuler',
    // ...
  },
  menu: {
    dashboard: 'Tableau de bord',
    // ...
  }
  // ... 其他模块
}
```

### 步骤 2：更新配置

在 `src/locales/index.ts` 中：

```typescript
import frFR from './locales/fr-FR'

// 添加语言类型
export type LocaleCode = 'zh-CN' | 'en-US' | 'ja-JP' | 'ko-KR' | 'fr-FR'

// 添加语言配置
export const localeConfigs: LocaleConfig[] = [
  // ... 其他语言
  { value: 'fr-FR', label: 'Français', labelNative: 'Français', flag: '🇫🇷', lang: 'fr' }
]

// 添加浏览器语言映射
const browserLangMap: Record<string, LocaleCode> = {
  // ... 其他映射
  'fr': 'fr-FR',
  'fr-fr': 'fr-FR'
}

// 添加到 i18n 配置
const i18n = createI18n({
  // ... 其他配置
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'ja-JP': jaJP,
    'ko-KR': koKR,
    'fr-FR': frFR
  },
  datetimeFormats: {
    'fr-FR': {
      short: { year: 'numeric', month: 'short', day: 'numeric' },
      long: { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }
      // ... 其他格式
    }
  },
  numberFormats: {
    'fr-FR': {
      currency: { style: 'currency', currency: 'EUR' },
      // ... 其他格式
    }
  }
})
```

### 步骤 3：测试

```typescript
import { setI18nLanguage } from '@/locales'

// 测试新语言
await setI18nLanguage('fr-FR')
```

## 最佳实践

### 1. 命名规范

- 使用嵌套结构组织翻译
- 模块名使用小写
- 键名使用小驼峰（camelCase）

```typescript
// ✅ 推荐
{
  user: {
    username: '用户名',
    email: '邮箱'
  }
}

// ❌ 不推荐
{
  'User Name': '用户名',
  user_email: '邮箱'
}
```

### 2. 参数占位符

使用 `{param}` 格式的占位符：

```typescript
// 语言包
{
  greeting: 'Hello, {name}!'
}

// 组件中使用
t('greeting', { name: 'John' })
```

### 3. 复数处理

使用 `$tc` 函数处理复数：

```typescript
// 语言包
{
  item: '{count} item | {count} items'
}

// 组件中使用
tc('item', 1)  // 1 item
tc('item', 5)  // 5 items
```

### 4. 保持翻译一致性

- 使用术语表确保关键术语翻译一致
- 定期审查翻译质量
- 考虑使用专业的翻译工具

### 5. 性能优化

- 按需加载语言包（对于大型应用）
- 使用翻译缓存
- 避免在模板中进行复杂的翻译计算

## 常见问题

### Q: 如何设置默认语言？

A: 默认语言在 `src/locales/index.ts` 的 `fallbackLocale` 中配置：

```typescript
const i18n = createI18n({
  fallbackLocale: 'zh-CN'
})
```

### Q: 如何持久化语言选择？

A: 语言选择会自动保存到 `localStorage`，用户下次访问时会自动使用上次选择的语言。

### Q: 如何在 URL 中显示语言？

A: 切换语言时会自动更新 URL 参数，例如 `?lang=en-US`。

### Q: 如何添加 Ant Design Vue 的国际化？

A: 在 `App.vue` 中已经配置：

```vue
<template>
  <a-config-provider :locale="antdLocale">
    <!-- 应用内容 -->
  </a-config-provider>
</template>
```

### Q: 如何处理缺失的翻译？

A: 使用 `fallbackLocale` 配置的默认语言作为回退。

## 相关资源

- [Vue I18n 官方文档](https://vue-i18n.intlify.dev/)
- [Ant Design Vue 国际化](https://antdv.com/docs/vue/i18n-cn)
- [Web 多语言国际化最佳实践](https://www.w3.org/International/)

## 更新日志

### v1.0.0 (2026-04-11)
- ✅ 完成基础国际化框架配置
- ✅ 实现中文、英文、日文、韩文语言包
- ✅ 实现语言自动检测和切换
- ✅ 添加语言切换器组件
- ✅ 实现 SEO 优化支持
- ✅ 编写使用文档

---

如有问题，请联系开发团队。