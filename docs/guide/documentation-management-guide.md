# 测试环境文档管理指南

**版本**: v1.0  
**创建日期**: 2026-04-27  
**项目**: AI-Ready (企智连系统)  
**适用阶段**: Sprint 27+1 测试环境配置专项

---

## 📋 目录

1. [快速开始](#快速开始)
2. [文档创建流程](#文档创建流程)
3. [文档维护流程](#文档维护流程)
4. [文档发布流程](#文档发布流程)
5. [常见问题](#常见问题)
6. [附录](#附录)

---

## 🚀 快速开始

### 1.1 准备工作

在开始编写文档前，请确保：

1. 阅读 [文档管理规范](../standards/documentation-standards.md)
2. 确认文档类型，选择对应模板
3. 准备必要的信息和数据

### 1.2 文档模板位置

| 文档类型 | 模板位置 |
|---------|---------|
| 部署文档 | `docs/templates/deployment/` |
| 用户手册 | `docs/templates/manual/` |
| 配置文档 | `docs/templates/deployment/` |

### 1.3 选择正确的文档目录

根据文档内容选择存放位置：

```
docs/
├── standards/     # 规范类文档
├── templates/     # 模板文件
├── guide/         # 使用指南
├── deploy/        # 部署文档
├── test-env/      # 测试环境专项
├── manual/        # 用户手册
└── api/           # API文档
```

---

## 📝 文档创建流程

### 2.1 创建步骤

#### 步骤1: 选择模板

根据文档类型复制对应模板：

```bash
# 复制部署文档模板
cp docs/templates/deployment/deployment-guide-template.md \
   docs/deploy/test-env/my-deployment.md

# 复制用户手册模板
cp docs/templates/manual/user-guide-template.md \
   docs/manual/user/my-manual.md
```

#### 步骤2: 填写元数据

在文档顶部填写基本信息：

```markdown
**文档名称**: [名称]
**版本号**: v1.0
**创建日期**: YYYY-MM-DD
**作者**: [你的名字]
```

#### 步骤3: 编写内容

按照模板结构编写内容：

1. 保留模板中的标题结构
2. 填写 `[方括号]` 占位符内容
3. 删除不需要的章节
4. 添加实际示例和截图

#### 步骤4: 格式检查

```bash
# 检查Markdown格式
markdownlint docs/my-document.md

# 检查链接有效性
markdown-link-check docs/my-document.md
```

#### 步骤5: 提交审核

提交前检查清单：

- [ ] 内容完整，无遗漏
- [ ] 格式符合规范
- [ ] 链接有效
- [ ] 无错别字
- [ ] 版本号已更新

---

## 🔧 文档维护流程

### 3.1 日常维护

#### 3.1.1 定期更新

| 检查项 | 频率 | 责任人 |
|--------|------|--------|
| 配置文档 | 配置变更时 | devops-engineer |
| 使用手册 | 功能变更时 | qa-lead |
| API文档 | 接口变更时 | backend-dev |
| 故障排查 | 问题解决后 | devops-engineer |

#### 3.1.2 更新步骤

1. **备份旧版本**
   ```bash
   git tag doc-v1.0
   ```

2. **修改文档**
   ```bash
   vim docs/my-document.md
   ```

3. **更新版本号**
   ```markdown
   **版本号**: v1.1
   **最后更新**: 2026-04-27
   ```

4. **记录变更**
   ```markdown
   ## 版本历史
   | 版本 | 日期 | 修改说明 | 作者 |
   |------|------|---------|------|
   | v1.1 | 2026-04-27 | 添加新功能说明 | doc-writer |
   ```

### 3.2 批量更新

当需要批量更新多个文档时：

```bash
# 查找需要更新的文档
grep -r "旧版本号" docs/

# 批量替换
sed -i 's/旧版本号/新版本号/g' docs/**/*.md

# 提交变更
git add docs/
git commit -m "docs: 批量更新文档版本号"
```

---

## 📢 文档发布流程

### 4.1 发布前准备

#### 4.1.1 质量检查

```bash
# 运行自动化检查
npm run docs:check

# 检查输出
# ✓ 所有文档格式正确
# ✓ 所有链接有效
# ✓ 所有图片可访问
```

#### 4.1.2 审核流程

```
文档作者 → 技术审核 → 内容审核 → 发布
   ↓           ↓           ↓
 编写      检查技术    检查语言
           准确性      规范性
```

### 4.2 发布方式

#### 4.2.1 Git发布

```bash
# 提交到Git
git add docs/
git commit -m "docs: 发布测试环境文档 v1.0"

# 打标签
git tag -a docs-v1.0 -m "测试环境文档 v1.0"

# 推送
git push origin main --tags
```

#### 4.2.2 通知团队

```markdown
📢 文档更新通知

文档名称: 测试环境部署指南
版本: v1.0 → v1.1
变更内容:
- 添加Docker部署说明
- 更新配置文件示例
- 修复已知问题

查看文档: [链接]
```

### 4.3 发布后工作

1. 更新文档索引
2. 通知相关团队成员
3. 归档旧版本（如需要）
4. 更新相关链接

---

## ❓ 常见问题

### Q1: 如何创建新类型的文档？

**A**: 
1. 参考现有模板创建新模板
2. 更新文档管理规范
3. 通知团队成员

### Q2: 文档格式检查失败怎么办？

**A**:
```bash
# 查看具体错误
markdownlint docs/my-document.md --verbose

# 自动修复部分问题
markdownlint docs/my-document.md --fix
```

### Q3: 如何处理文档冲突？

**A**:
1. 使用Git解决冲突
2. 合并两个版本的内容
3. 更新版本号
4. 重新提交

### Q4: 文档中的敏感信息如何处理？

**A**:
1. 使用占位符替代真实密码
2. 配置文件中不包含敏感信息
3. 敏感信息存储在环境变量中

### Q5: 如何批量生成文档？

**A**:
```bash
# 使用脚本批量生成
node scripts/generate-docs.js

# 或使用模板引擎
npm run docs:generate
```

---

## 📚 附录

### A. 常用工具

| 工具 | 用途 | 命令 |
|------|------|------|
| markdownlint | Markdown格式检查 | `markdownlint *.md` |
| markdown-link-check | 链接检查 | `markdown-link-check *.md` |
| git | 版本控制 | `git add/commit/push` |
| sed | 文本替换 | `sed -i 's/old/new/g'` |

### B. 快捷键

| 操作 | 快捷键 |
|------|--------|
| 格式化文档 | Ctrl + Shift + I |
| 预览Markdown | Ctrl + Shift + V |
| 查找 | Ctrl + F |
| 替换 | Ctrl + H |

### C. 参考文档

- [文档管理规范](../standards/documentation-standards.md)
- [Markdown语法指南](https://www.markdownguide.org/)
- [Git工作流](https://git-scm.com/book/zh/v2/Git-%E5%88%86%E6%94%AF-%E5%88%86%E6%94%AF%E7%AE%80%E4%BB%8B)

### D. 联系方式

- **文档维护**: doc-writer
- **技术支持**: devops-engineer
- **内容审核**: qa-lead

---

**文档版本**: v1.0  
**最后更新**: 2026-04-27  
**作者**: doc-writer
