# AI-Ready 监控告警模块 - 自定义Exporter Dockerfile
# 版本: v1.0.0
# 最后更新: 2026-04-27

# 使用官方Python镜像作为基础
FROM python:3.11-slim AS builder

# 设置环境变量
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV PIP_NO_CACHE_DIR=1
ENV PIP_DISABLE_PIP_VERSION_CHECK=1
ENV EXPORTER_VERSION=v1.0.0
ENV EXPORTER_HOME="/app"
ENV EXPORTER_PORT="9095"
ENV METRICS_PATH="/metrics"
ENV EXPORTER_NAME="ai-ready-custom-exporter"

# 设置工作目录
WORKDIR $EXPORTER_HOME

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt \
    && rm -rf /root/.cache/pip

# 复制应用代码
COPY . .

# 运行阶段
FROM python:3.11-slim AS runtime

# 设置环境变量
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV EXPORTER_HOME="/app"
ENV EXPORTER_PORT="9095"
ENV METRICS_PATH="/metrics"
ENV EXPORTER_NAME="ai-ready-custom-exporter"
ENV LOG_LEVEL="INFO"
ENV METRICS_UPDATE_INTERVAL="15"
ENV ENABLE_HEALTH_CHECK="true"
ENV ENABLE_METRICS_VALIDATION="true"
ENV MAX_METRICS_COUNT="1000"
ENV HTTP_TIMEOUT="30"
ENV GUNICORN_WORKERS="4"
ENV GUNICORN_THREADS="2"
ENV GUNICORN_WORKER_CLASS="sync"
ENV GUNICORN_MAX_REQUESTS="1000"
ENV GUNICORN_MAX_REQUESTS_JITTER="50"

# 创建非root用户
RUN groupadd -r exporter && useradd -r -g exporter exporter

# 设置工作目录
WORKDIR $EXPORTER_HOME

# 创建必要的目录
RUN mkdir -p $EXPORTER_HOME \
    && mkdir -p $EXPORTER_HOME/logs \
    && mkdir -p $EXPORTER_HOME/data \
    && mkdir -p $EXPORTER_HOME/config \
    && chown -R exporter:exporter $EXPORTER_HOME

# 从构建阶段复制已安装的包
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# 复制应用代码
COPY --chown=exporter:exporter . $EXPORTER_HOME

# 复制配置文件
COPY --chown=exporter:exporter config/config.yaml $EXPORTER_HOME/config/
COPY --chown=exporter:exporter config/metrics_definitions.yaml $EXPORTER_HOME/config/

# 复制自定义脚本
COPY --chown=exporter:exporter scripts/entrypoint.sh /entrypoint.sh
COPY --chown=exporter:exporter scripts/healthcheck.sh /healthcheck.sh
COPY --chown=exporter:exporter scripts/collect-metrics.sh /collect-metrics.sh

# 设置脚本权限
RUN chmod +x /entrypoint.sh \
    && chmod +x /healthcheck.sh \
    && chmod +x /collect-metrics.sh

# 安装必要的系统包
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        jq \
        netcat-openbsd \
        procps \
    && rm -rf /var/lib/apt/lists/*

# 安装Prometheus客户端库
RUN pip install --no-cache-dir \
    prometheus-client==0.19.0 \
    flask==3.0.0 \
    gunicorn==21.2.0 \
    psutil==5.9.6 \
    requests==2.31.0 \
    pyyaml==6.0.1 \
    redis==5.0.1 \
    pymongo==4.5.0 \
    mysql-connector-python==8.2.0 \
    psycopg2-binary==2.9.9

# 清理缓存
RUN apt-get clean \
    && rm -rf /var/cache/apt/* \
    && rm -rf /root/.cache/pip

# 切换为非root用户
USER exporter

# 暴露端口
EXPOSE $EXPORTER_PORT

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD ["/healthcheck.sh"]

# 设置入口点
ENTRYPOINT ["/entrypoint.sh"]

# 默认命令
CMD ["gunicorn", \
     "--bind", "0.0.0.0:9095", \
     "--workers", "4", \
     "--threads", "2", \
     "--worker-class", "sync", \
     "--max-requests", "1000", \
     "--max-requests-jitter", "50", \
     "--access-logfile", "-", \
     "--error-logfile", "-", \
     "--log-level", "info", \
     "exporter.app:app"]

# 多阶段构建 - 优化镜像
FROM runtime AS optimized

# 安装额外的监控工具
USER root

# 安装额外工具
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        htop \
        iotop \
        iftop \
        net-tools \
        dnsutils \
        telnet \
        vim \
        tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 安装Prometheus node_exporter文本收集器
RUN mkdir -p /etc/node_exporter/textfile_collector \
    && chown exporter:exporter /etc/node_exporter/textfile_collector

# 切换回exporter用户
USER exporter

# 标签信息
LABEL org.opencontainers.image.title="AI-Ready Custom Exporter"
LABEL org.opencontainers.image.description="Custom Prometheus exporter for AI-Ready platform"
LABEL org.opencontainers.image.version="${EXPORTER_VERSION}"
LABEL org.opencontainers.image.created="2026-04-27T01:33:00Z"
LABEL org.opencontainers.image.source="https://github.com/ai-ready/monitoring-alerting"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.authors="devops-engineer@ai-ready.local"

# 构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# 设置构建元数据
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="ai-ready-custom-exporter" \
      org.label-schema.description="Custom Prometheus exporter for AI-Ready monitoring" \
      org.label-schema.url="https://ai-ready.local" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ai-ready/monitoring-alerting" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# 最终使用优化镜像
FROM optimized AS final

# 验证Python包安装
RUN python -c "import prometheus_client; import flask; print('Dependencies verified')"

# 创建默认配置文件
COPY --chown=exporter:exporter config/defaults/ $EXPORTER_HOME/config/defaults/

# 创建示例指标
COPY --chown=exporter:exporter examples/ $EXPORTER_HOME/examples/

# 设置环境变量默认值
ENV EXPORTER_METRICS=[
  "system_cpu_usage",
  "system_memory_usage", 
  "system_disk_usage",
  "application_throughput",
  "application_error_rate",
  "business_transaction_count",
  "business_success_rate",
  "database_connection_pool",
  "cache_hit_rate",
  "queue_length"
]

ENV EXPORTER_LABELS={
  "cluster": "ai-ready",
  "environment": "development",
  "region": "cn-east-1",
  "team": "devops"
}

ENV EXPORTER_TAGS=[
  "monitoring",
  "exporter",
  "prometheus",
  "metrics",
  "observability"
]

ENV LOGGING_CONFIG={
  "version": 1,
  "disable_existing_loggers": false,
  "formatters": {
    "standard": {
      "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    },
    "json": {
      "format": "%(asctime)s %(name)s %(levelname)s %(message)s",
      "class": "pythonjsonlogger.jsonlogger.JsonFormatter"
    }
  },
  "handlers": {
    "console": {
      "class": "logging.StreamHandler",
      "formatter": "standard",
      "level": "INFO"
    },
    "file": {
      "class": "logging.handlers.RotatingFileHandler",
      "formatter": "json",
      "filename": "/app/logs/exporter.log",
      "maxBytes": 10485760,
      "backupCount": 5,
      "level": "DEBUG"
    }
  },
  "loggers": {
    "": {
      "handlers": ["console", "file"],
      "level": "INFO",
      "propagate": true
    },
    "exporter": {
      "handlers": ["console", "file"],
      "level": "DEBUG",
      "propagate": false
    }
  }
}

# 优化启动性能
COPY --chown=exporter:exporter scripts/optimize-performance.sh /optimize-performance.sh
RUN chmod +x /optimize-performance.sh \
    && /optimize-performance.sh

# 验证应用启动
RUN python -c "from exporter.app import app; print('Application import successful')"

# 创建测试端点
COPY --chown=exporter:exporter scripts/create-test-endpoints.sh /create-test-endpoints.sh
RUN chmod +x /create-test-endpoints.sh \
    && /create-test-endpoints.sh

# 最终验证
RUN echo "Custom exporter build completed successfully" \
    && echo "Version: $EXPORTER_VERSION" \
    && echo "Port: $EXPORTER_PORT" \
    && echo "Metrics path: $METRICS_PATH" \
    && echo "Home directory: $EXPORTER_HOME"