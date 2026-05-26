# 测试容器Dockerfile

# 基于Alpine Linux的轻量级容器，用于测试框架运行
FROM python:3.11-alpine

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apk add --no-cache \
    curl \
    wget \
    bash \
    git \
    make \
    gcc \
    musl-dev \
    libffi-dev

# 安装Python依赖
COPY requirements-test.txt /app/
RUN pip install --no-cache-dir -r requirements-test.txt

# 创建测试用户（非root运行）
RUN addgroup -g 1000 testuser && \
    adduser -u 1000 -G testuser -s /bin/bash -D testuser

# 切换到非root用户
USER testuser

# 复制测试框架代码
COPY --chown=testuser:testuser . /app/

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV pytestopts="-v --tb=short"

# 容器启动命令
CMD ["python", "-m", "pytest", "--co"]
