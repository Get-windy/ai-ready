# 文档自动化工具配置

**版本**: v1.0  
**创建日期**: 2026-04-27  
**项目**: AI-Ready (企智连系统)

---

## 📋 概述

本文档介绍测试环境文档自动化工具的配置和使用，包括文档生成、检查、发布等自动化流程。

---

## 🛠️ 工具配置

### 1. MkDocs配置

```yaml
# mkdocs.yml
site_name: AI-Ready 测试环境文档
site_description: 企智连系统测试环境完整文档
site_author: AI-Ready Team

theme:
  name: material
  palette:
    - scheme: default
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode
    - scheme: slate
      primary: indigo
      accent: indigo
      toggle:
        icon: material/brightness-4
        name: Switch to light mode
  features:
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - search.suggest
    - search.highlight
    - content.code.copy

plugins:
  - search
  - minify:
      minify_html: true

markdown_extensions:
  - pymdownx.highlight:
      anchor_linenums: true
  - pymdownx.inlinehilite
  - pymdownx.snippets
  - pymdownx.superfences
  - admonition
  - pymdownx.details
  - pymdownx.tabbed:
      alternate_style: true
  - tables
  - toc:
      permalink: true

nav:
  - 首页: index.md
  - 规范:
    - 文档管理规范: standards/documentation-standards.md
    - 质量检查: standards/quality-check-script.md
  - 指南:
    - 文档管理指南: guide/documentation-management-guide.md
  - 模板:
    - 部署模板: templates/deployment/
    - 用户手册模板: templates/manual/
  - API文档:
    - 概述: api/README.md
```

### 2. package.json配置

```json
{
  "name": "ai-ready-docs",
  "version": "1.0.0",
  "description": "AI-Ready 测试环境文档",
  "scripts": {
    "docs:dev": "mkdocs serve",
    "docs:build": "mkdocs build",
    "docs:deploy": "mkdocs gh-deploy",
    "docs:check": "bash docs/standards/docs-quality-check.sh",
    "docs:lint": "markdownlint docs/**/*.md",
    "docs:links": "markdown-link-check docs/**/*.md",
    "docs:generate": "node scripts/generate-docs.js"
  },
  "devDependencies": {
    "markdownlint-cli": "^0.37.0",
    "markdown-link-check": "^3.11.2"
  }
}
```

### 3. 自动化脚本

```javascript
// scripts/generate-docs.js
const fs = require('fs');
const path = require('path');

// 文档生成配置
const config = {
  templateDir: 'docs/templates',
  outputDir: 'docs/generated',
  metadata: {
    version: 'v1.0',
    date: new Date().toISOString().split('T')[0],
    author: 'doc-writer'
  }
};

// 生成文档索引
function generateIndex() {
  const docs = [];
  
  function scanDir(dir) {
    const items = fs.readdirSync(dir);
    
    items.forEach(item => {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      
      if (stat.isDirectory()) {
        scanDir(fullPath);
      } else if (item.endsWith('.md')) {
        docs.push({
          path: fullPath,
          title: extractTitle(fullPath)
        });
      }
    });
  }
  
  scanDir('docs');
  
  // 生成索引文件
  const indexContent = generateIndexContent(docs);
  fs.writeFileSync('docs/index.md', indexContent);
  
  console.log('文档索引已生成');
}

// 提取文档标题
function extractTitle(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const match = content.match(/^# (.+)$/m);
  return match ? match[1] : path.basename(filePath, '.md');
}

// 生成索引内容
function generateIndexContent(docs) {
  let content = `# AI-Ready 测试环境文档\n\n`;
  content += `**最后更新**: ${config.metadata.date}\n\n`;
  content += `## 文档列表\n\n`;
  
  docs.forEach(doc => {
    const relativePath = path.relative('docs', doc.path);
    content += `- [${doc.title}](${relativePath})\n`;
  });
  
  return content;
}

// 主函数
function main() {
  console.log('开始生成文档...');
  generateIndex();
  console.log('文档生成完成！');
}

main();
```

---

## 🚀 使用方式

### 1. 本地开发

```bash
# 安装依赖
npm install

# 启动本地服务器
npm run docs:dev

# 访问 http://localhost:8000
```

### 2. 构建文档

```bash
# 构建静态站点
npm run docs:build

# 输出到 site/ 目录
```

### 3. 质量检查

```bash
# 运行所有检查
npm run docs:check

# 单独运行检查
npm run docs:lint
npm run docs:links
```

### 4. 自动发布

```bash
# 生成并发布
npm run docs:generate
npm run docs:build
npm run docs:deploy
```

---

## 📊 CI/CD集成

### GitHub Actions配置

```yaml
# .github/workflows/docs.yml
name: Documentation

on:
  push:
    branches: [ main ]
    paths:
      - 'docs/**'
  pull_request:
    paths:
      - 'docs/**'

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: npm install
      
      - name: Run quality checks
        run: npm run docs:check
      
      - name: Run linter
        run: npm run docs:lint
      
      - name: Check links
        run: npm run docs:links

  build:
    needs: check
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.10'
      
      - name: Install MkDocs
        run: |
          pip install mkdocs-material
          pip install mkdocs-minify-plugin
      
      - name: Build documentation
        run: mkdocs build
      
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: documentation
          path: site/

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Python
        uses: actions/setup-python@v4
        with:
          python-version: '3.10'
      
      - name: Install MkDocs
        run: |
          pip install mkdocs-material
          pip install mkdocs-minify-plugin
      
      - name: Deploy to GitHub Pages
        run: mkdocs gh-deploy --force
```

---

## 📚 附录

### A. 常用命令

| 命令 | 说明 |
|------|------|
| `npm run docs:dev` | 启动开发服务器 |
| `npm run docs:build` | 构建文档 |
| `npm run docs:check` | 质量检查 |
| `npm run docs:lint` | 格式检查 |
| `npm run docs:links` | 链接检查 |
| `npm run docs:generate` | 生成索引 |

### B. 目录结构

```
docs/
├── standards/          # 规范文档
├── templates/          # 模板文件
├── guide/             # 使用指南
├── scripts/           # 自动化脚本
├── mkdocs.yml         # MkDocs配置
└── package.json       # Node.js配置
```

---

**文档版本**: v1.0  
**最后更新**: 2026-04-27  
**作者**: doc-writer
