# 资源监控与成本分析服务 Dockerfile

# 第一阶段：构建阶段
FROM python:3.11-slim as builder

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --user --no-cache-dir -r requirements.txt

# 第二阶段：运行阶段
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/root/.local/bin:$PATH" \
    PROMETHEUS_URL="http://localhost:9090" \
    POSTGRES_URL="" \
    REDIS_URL="" \
    COLLECTION_INTERVAL="300" \
    COST_CALCULATION_INTERVAL="3600" \
    ALERT_CHECK_INTERVAL="60" \
    LOG_LEVEL="INFO" \
    TZ="UTC"

# 安装运行时依赖
RUN apt-get update && apt-get install -y \
    libpq-dev \
    curl \
    netcat \
    && rm -rf /var/lib/apt/lists/*

# 创建非root用户
RUN groupadd -r monitoring && useradd -r -g monitoring monitoring

# 从构建阶段复制已安装的Python包
COPY --from=builder /root/.local /root/.local

# 复制应用程序代码
COPY . .

# 创建必要的目录
RUN mkdir -p /app/data /app/logs /app/config && \
    chown -R monitoring:monitoring /app

# 切换到非root用户
USER monitoring

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# 启动命令
CMD ["python", "-m", "src.main"]