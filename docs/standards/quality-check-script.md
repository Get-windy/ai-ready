# 文档质量检查脚本

**版本**: v1.0  
**创建日期**: 2026-04-27  
**项目**: AI-Ready (企智连系统)

---

## 📋 概述

本文档提供自动化文档质量检查脚本，确保所有测试环境文档符合规范。

---

## 🔧 检查项目

### 1. 格式检查

```bash
#!/bin/bash
# format-check.sh

echo "=== Markdown格式检查 ==="

# 检查标题层级
for file in docs/**/*.md; do
    echo "检查: $file"
    
    # 检查是否有H1标题
    if ! grep -q "^# " "$file"; then
        echo "  ✗ 缺少一级标题"
    fi
    
    # 检查代码块语言标识
    grep -n "^\`\`\`$" "$file" && echo "  ✗ 发现无语言标识的代码块"
    
    # 检查表格格式
    grep -n "|" "$file" | grep -v "|-.\+|" | grep -v "|.*|.*|" && echo "  ✗ 表格格式可能不正确"
done
```

### 2. 链接检查

```bash
#!/bin/bash
# link-check.sh

echo "=== 链接有效性检查 ==="

# 检查内部链接
for file in docs/**/*.md; do
    echo "检查: $file"
    
    # 提取内部链接
    grep -oE '\[([^]]+)\]\(([^)]+)\)' "$file" | while read link; do
        url=$(echo "$link" | sed 's/.*](\(.*\))/\1/')
        
        # 检查是否为内部链接
        if [[ "$url" == ./* ]] || [[ "$url" == ../* ]]; then
            # 获取链接所在目录
            dir=$(dirname "$file")
            target="$dir/$url"
            
            if [ ! -f "$target" ]; then
                echo "  ✗ 链接不存在: $url"
            fi
        fi
    done
done
```

### 3. 内容完整性检查

```bash
#!/bin/bash
# content-check.sh

echo "=== 内容完整性检查 ==="

for file in docs/**/*.md; do
    echo "检查: $file"
    
    # 检查元数据
    if ! grep -q "版本" "$file"; then
        echo "  ✗ 缺少版本信息"
    fi
    
    if ! grep -q "创建日期\|日期" "$file"; then
        echo "  ✗ 缺少日期信息"
    fi
    
    # 检查目录结构
    if ! grep -q "## " "$file"; then
        echo "  ✗ 缺少二级标题（目录结构）"
    fi
    
    # 检查占位符
    if grep -q '\[.*\]' "$file"; then
        echo "  ⚠ 发现占位符未替换"
    fi
done
```

### 4. 综合检查脚本

```bash
#!/bin/bash
# docs-quality-check.sh

set -e

DOCS_DIR="docs"
ERRORS=0

echo "=========================================="
echo "文档质量检查"
echo "=========================================="
echo ""

# 1. 格式检查
echo "1. Markdown格式检查"
echo "----------------------------------------"
for file in $DOCS_DIR/**/*.md; do
    if [ -f "$file" ]; then
        # 检查标题层级
        h1_count=$(grep -c "^# " "$file" || true)
        if [ "$h1_count" -eq 0 ]; then
            echo "  ✗ $file: 缺少一级标题"
            ERRORS=$((ERRORS + 1))
        elif [ "$h1_count" -gt 1 ]; then
            echo "  ✗ $file: 多个一级标题"
            ERRORS=$((ERRORS + 1))
        fi
        
        # 检查空行
        if grep -q "^$" "$file"; then
            : # 空行是正常的
        fi
    fi
done
echo ""

# 2. 链接检查
echo "2. 链接有效性检查"
echo "----------------------------------------"
for file in $DOCS_DIR/**/*.md; do
    if [ -f "$file" ]; then
        dir=$(dirname "$file")
        
        # 提取并检查内部链接
        grep -oE '\[([^]]+)\]\(([^)]+)\)' "$file" | while read link; do
            url=$(echo "$link" | sed 's/.*](\(.*\))/\1/' | sed 's/#.*//')
            
            if [[ "$url" == ./* ]] || [[ "$url" == ../* ]]; then
                target="$dir/$url"
                if [ ! -f "$target" ]; then
                    echo "  ✗ $file: 链接不存在 - $url"
                fi
            fi
        done
    fi
done
echo ""

# 3. 内容检查
echo "3. 内容完整性检查"
echo "----------------------------------------"
for file in $DOCS_DIR/**/*.md; do
    if [ -f "$file" ]; then
        # 检查必填字段
        if ! grep -q "版本" "$file"; then
            echo "  ✗ $file: 缺少版本信息"
            ERRORS=$((ERRORS + 1))
        fi
        
        # 检查占位符
        placeholders=$(grep -o '\[.*\]' "$file" | grep -v ']' | wc -l || true)
        if [ "$placeholders" -gt 0 ]; then
            echo "  ⚠ $file: 发现未替换的占位符"
        fi
    fi
done
echo ""

# 4. 统计信息
echo "4. 文档统计"
echo "----------------------------------------"
doc_count=$(find $DOCS_DIR -name "*.md" | wc -l)
total_size=$(find $DOCS_DIR -name "*.md" -exec stat -f%z {} + | awk '{sum+=$1} END {print sum}')
echo "  文档数量: $doc_count"
echo "  总大小: $total_size bytes"
echo ""

# 5. 检查结果
echo "=========================================="
if [ $ERRORS -eq 0 ]; then
    echo "✅ 所有检查通过！"
else
    echo "❌ 发现 $ERRORS 个问题"
    exit 1
fi
echo "=========================================="
```

---

## 🚀 使用方式

### 1. 手动执行

```bash
# 执行所有检查
bash docs/standards/quality-check-script.md

# 或执行具体检查
bash format-check.sh
bash link-check.sh
bash content-check.sh
```

### 2. 集成到CI/CD

```yaml
# .github/workflows/docs-check.yml
name: Documentation Quality Check

on:
  push:
    paths:
      - 'docs/**'
  pull_request:
    paths:
      - 'docs/**'

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Run Quality Check
        run: |
          chmod +x docs/standards/docs-quality-check.sh
          bash docs/standards/docs-quality-check.sh
```

### 3. Git Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

# 检查文档变更
if git diff --cached --name-only | grep -q "^docs/"; then
    echo "检查文档质量..."
    bash docs/standards/docs-quality-check.sh
    
    if [ $? -ne 0 ]; then
        echo "文档检查失败，请修复问题后重新提交"
        exit 1
    fi
fi
```

---

## 📊 检查标准

### 通过标准

| 检查项 | 通过条件 |
|--------|---------|
| 格式检查 | 无格式错误 |
| 链接检查 | 所有内部链接有效 |
| 内容检查 | 元数据完整，无占位符 |
| 结构检查 | 标题层级正确 |

### 问题等级

| 等级 | 说明 | 处理方式 |
|------|------|---------|
| 🔴 错误 | 必须修复 | 阻塞发布 |
| 🟡 警告 | 建议修复 | 不阻塞但需记录 |
| 🟢 提示 | 仅供参考 | 记录即可 |

---

## 📚 附录

### A. 常见问题

**Q: 检查脚本执行失败**

```bash
# 检查权限
chmod +x docs-quality-check.sh

# 检查依赖
which grep sed awk
```

**Q: 链接检查误报**

```bash
# 手动验证链接
ls -la docs/path/to/file.md
```

### B. 扩展检查

可以添加以下扩展检查：

1. 拼写检查
2. 术语一致性检查
3. 图片大小检查
4. 敏感信息检查

---

**文档版本**: v1.0  
**最后更新**: 2026-04-27  
**作者**: doc-writer
