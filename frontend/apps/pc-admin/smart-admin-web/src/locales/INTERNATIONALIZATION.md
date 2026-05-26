# AI-Ready 前端国际化扩展说明

## 概述

本扩展为 AI-Ready 前端项目添加了多语言支持，包括日语（日本語）和韩语（한국어），并实现了语言自动检测、切换动画和 SEO 优化功能。

## 完成的特性

### 1. 扩展语言包支持

**新增语言：**
- `ja-JP` - 日语 (日本語)
- `ko-KR` - 韩语 (한국어)

**文件位置：**
- `src/locales/locales/ja-JP.ts` - 日语语言包
- `src/locales/locales/ko-KR.ts` - 韩语语言包

**语言包结构：**
- common - 通用按钮和状态
- menu - 导航菜单
- login - 登录相关
- user - 用户管理
- role - 角色管理
- product - 商品管理
- order - 订单管理
- customer - 客户管理
- inventory - 库存管理
- settings - 系统设置
- validation - 验证消息
- error - 错误消息
- success - 成功消息

### 2. 语言自动检测

**检测优先级：**
1. 本地存储 (localStorage)
2. 浏览器语言设置
3. URL 参数 (`?lang=ja-JP`)
4. 默认语言 (zh-CN)

**支持的浏览器语言：**
- `zh`, `zh-CN`, `zh-TW`, `zh-HK` → 简体中文
- `en`, `en-US`, `en-GB` → English
- `ja`, `ja-JP` → 日本語
- `ko`, `ko-KR` → 한국어

### 3. 语言切换动画效果

**动画特性：**
- 淡入淡出过渡效果
- 切换时禁用交互（防止重复点击）
- 下拉菜单悬停效果
- 图标平滑过渡

**事件系统：**
- `locale:before-change` - 语言切换前事件
- `locale:changed` - 语言切换完成事件

### 4. 多语言 SEO 支持

**实现的功能：**
- HTML `lang` 属性动态更新
- Open Graph `og:locale` Meta 标签
- URL 参数同步 (`?lang=ja-JP`)
- 可扩展的 hreflang 标签生成

**SEO 工具函数：**
```typescript
import { seoUtils } from '@/locales'

// 生成单个 hreflang
seoUtils.generateHreflang('ja-JP')

// 生成所有 hreflang
seoUtils.generateAllHreflangs()

// 更新页面标题
seoUtils.updateTitle('Dashboard', 'zh-CN')
```

## API 参考

### 核心函数

| 函数 | 说明 | 参数 |
|------|------|------|
| `getCurrentLocale()` | 获取当前语言代码 | - |
| `setI18nLanguage(locale)` | 切换语言 | `LocaleCode` |
| `getLocaleInfo(locale)` | 获取语言配置信息 | `LocaleCode` |
| `getSupportedLocales()` | 获取支持的语言列表 | - |
| `setupI18n()` | 初始化 i18n | - |

### 类型定义

```typescript
type LocaleCode = 'zh-CN' | 'en-US' | 'ja-JP' | 'ko-KR'

interface LocaleConfig {
  value: LocaleCode
  label: string
  labelNative: string
  flag: string
  lang: string
}
```

### 语言配置

```typescript
import { localeConfigs } from '@/locales'

// 可用的语言列表
console.log(localeConfigs)
// 输出:
// [
//   { value: 'zh-CN', label: '简体中文', labelNative: '简体中文', flag: '🇨🇳', lang: 'zh-CN' },
//   { value: 'en-US', label: 'English', labelNative: 'English', flag: '🇺🇸', lang: 'en' },
//   { value: 'ja-JP', label: '日本語', labelNative: '日本語', flag: '🇯🇵', lang: 'ja' },
//   { value: 'ko-KR', label: '한국어', labelNative: '한국어', flag: '🇰🇷', lang: 'ko' }
// ]
```

## 使用示例

### 在组件中使用

```vue
<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { getCurrentLocale, setI18nLanguage } from '@/locales'

const { t } = useI18n()

// 切换语言
async function changeToJapanese() {
  await setI18nLanguage('ja-JP')
}
</script>

<template>
  <div>{{ t('common.confirm') }}</div>
</template>
```

### 监听语言变化

```typescript
window.addEventListener('locale:changed', (event: CustomEvent) => {
  const { locale } = event.detail
  console.log('Language changed to:', locale)
})
```

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/locales/locales/ja-JP.ts` | 新增 | 日语语言包 |
| `src/locales/locales/ko-KR.ts` | 新增 | 韩语语言包 |
| `src/locales/index.ts` | 修改 | 增强国际化功能 |
| `src/components/LocaleSwitcher.vue` | 修改 | 添加动画效果 |

## 扩展语言包

如需添加新语言，请执行以下步骤：

1. 在 `src/locales/locales/` 目录下创建新的语言包文件（如 `fr-FR.ts`）
2. 在 `src/locales/index.ts` 中：
   - 添加新的 import 语句
   - 在 `localeConfigs` 数组中添加配置
   - 在 `browserLangMap` 中添加浏览器语言映射
   - 在 `messages` 对象中添加语言包
   - 在 `datetimeFormats` 和 `numberFormats` 中添加格式配置

## 注意事项

1. 语言切换会同时更新 localStorage，用户再次访问时会自动使用上次选择的语言
2. 语言切换会更新 URL 参数，可用于分享特定语言版本的页面
3. SEO 功能会自动更新 HTML lang 属性和 Open Graph 标签
4. 组件已添加事件系统，支持在语言切换时执行自定义逻辑