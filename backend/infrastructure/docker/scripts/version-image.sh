#!/bin/bash

# Docker镜像版本号生成脚本
# 用途: 根据Git信息自动生成镜像版本号
# 作者: devops-engineer
# 创建日期: 2026-04-27

set -e

# 获取Git信息
GIT_SHA=$(git rev-parse HEAD | cut -c1-8)
GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
GIT_TAG=$(git describe --tags --exact-match 2>/dev/null || echo "")

# 生成版本号
if [ -n "$GIT_TAG" ]; then
    # 如果有Tag，使用Tag作为版本号
    IMAGE_VERSION="$GIT_TAG"
    IMAGE_TAG="${GIT_TAG}-${GIT_SHA}"
elif [ "$GIT_BRANCH" = "main" ]; then
    # main分支使用版本号
    IMAGE_VERSION="latest"
    IMAGE_TAG="latest-${GIT_SHA}"
elif [ "$GIT_BRANCH" = "develop" ]; then
    # develop分支使用test前缀
    IMAGE_VERSION="test-${GIT_SHA}"
    IMAGE_TAG="test-${GIT_SHA}"
else
    # 其他分支使用dev前缀
    IMAGE_VERSION="dev-${GIT_SHA}"
    IMAGE_TAG="dev-${GIT_SHA}"
fi

# 输出结果
echo "=== Docker镜像版本生成结果 ==="
echo "GIT_SHA: $GIT_SHA"
echo "GIT_BRANCH: $GIT_BRANCH"
echo "GIT_TAG: $GIT_TAG"
echo ""
echo "镜像版本号: $IMAGE_VERSION"
echo "镜像Tag: $IMAGE_TAG"
echo ""

# 验证生成的版本号
if [ -z "$IMAGE_TAG" ]; then
    echo "❌ 错误: 未能生成有效的镜像Tag"
    exit 1
fi

echo "✅ 镜像版本号生成成功"
echo ""

# 保存版本号到环境文件（供后续步骤使用）
echo "DOCKER_IMAGE_VERSION=$IMAGE_VERSION" > .docker_env
echo "DOCKER_IMAGE_TAG=$IMAGE_TAG" >> .docker_env
echo "DOCKER_IMAGE_GIT_SHA=$GIT_SHA" >> .docker_env

echo "✅ 版本号已保存到 .docker_env 文件"
echo ""
echo "使用示例:"
echo "  docker build -t myapp:\${DOCKER_IMAGE_TAG} ."
echo "  docker push myapp:\${DOCKER_IMAGE_TAG}"
