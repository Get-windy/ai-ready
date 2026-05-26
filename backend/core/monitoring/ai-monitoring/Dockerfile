# AI模型质量监控系统 Docker镜像
# 基于Python 3.9

FROM python:3.9-slim

LABEL maintainer="AI-Ready Team"
LABEL version="1.0.0"
LABEL description="AI模型质量监控系统"

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV MODEL_NAME="ai_ready_model"
ENV MONITOR_PORT=8000

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 创建必要的目录
RUN mkdir -p logs evaluations

# 暴露端口
EXPOSE ${MONITOR_PORT}

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:${MONITOR_PORT}/health')"

# 启动命令
CMD ["python", "ai_model_monitor.py"]