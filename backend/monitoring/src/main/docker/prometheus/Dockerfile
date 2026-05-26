# AI-Ready 监控告警模块 - Prometheus Dockerfile
# 版本: v2.45.0
# 最后更新: 2026-04-27

# 使用官方Prometheus镜像作为基础
FROM prom/prometheus:v2.45.0 AS base

# 设置环境变量
ENV PROMETHEUS_VERSION=v2.45.0
ENV PROMETHEUS_HOME="/prometheus"
ENV PROMETHEUS_CONFIG_DIR="/etc/prometheus"
ENV PROMETHEUS_DATA_DIR="/prometheus/data"
ENV PROMETHEUS_WEB_LISTEN_ADDRESS=":9090"
ENV PROMETHEUS_WEB_ENABLE_ADMIN_API="true"
ENV PROMETHEUS_WEB_ENABLE_LIFECYCLE="true"
ENV PROMETHEUS_STORAGE_TSDB_RETENTION_TIME="30d"
ENV PROMETHEUS_STORAGE_TSDB_RETENTION_SIZE="15GB"
ENV PROMETHEUS_STORAGE_TSDB_WAL_COMPRESSION="true"
ENV PROMETHEUS_STORAGE_TSDB_MAX_BLOCKS="10000"

# 设置工作目录
WORKDIR $PROMETHEUS_HOME

# 创建必要的目录
RUN mkdir -p $PROMETHEUS_CONFIG_DIR \
    && mkdir -p $PROMETHEUS_DATA_DIR \
    && mkdir -p $PROMETHEUS_HOME/rules \
    && mkdir -p $PROMETHEUS_HOME/alerts \
    && chown -R nobody:nobody $PROMETHEUS_HOME \
    && chown -R nobody:nobody $PROMETHEUS_CONFIG_DIR \
    && chown -R nobody:nobody $PROMETHEUS_DATA_DIR

# 复制配置文件
COPY prometheus.yml $PROMETHEUS_CONFIG_DIR/
COPY rules/*.yml $PROMETHEUS_HOME/rules/
COPY alerts/*.yml $PROMETHEUS_HOME/alerts/

# 复制自定义脚本
COPY scripts/entrypoint.sh /entrypoint.sh
COPY scripts/healthcheck.sh /healthcheck.sh

# 设置脚本权限
RUN chmod +x /entrypoint.sh \
    && chmod +x /healthcheck.sh \
    && chown nobody:nobody /entrypoint.sh \
    && chown nobody:nobody /healthcheck.sh

# 切换为非root用户
USER nobody

# 暴露端口
EXPOSE 9090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD ["/healthcheck.sh"]

# 设置入口点
ENTRYPOINT ["/entrypoint.sh"]

# 默认命令
CMD ["--config.file=/etc/prometheus/prometheus.yml", \
     "--storage.tsdb.path=/prometheus/data", \
     "--web.console.libraries=/etc/prometheus/console_libraries", \
     "--web.console.templates=/etc/prometheus/consoles", \
     "--web.enable-lifecycle", \
     "--web.enable-admin-api"]

# 多阶段构建 - 优化镜像
FROM base AS optimized

# 安装必要的工具用于调试和监控
USER root

# 安装基础工具
RUN apk add --no-cache \
    curl \
    jq \
    busybox-extras \
    tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 清理缓存
RUN rm -rf /var/cache/apk/*

# 切换回非root用户
USER nobody

# 标签信息
LABEL org.opencontainers.image.title="AI-Ready Prometheus"
LABEL org.opencontainers.image.description="Prometheus monitoring system for AI-Ready platform"
LABEL org.opencontainers.image.version="${PROMETHEUS_VERSION}"
LABEL org.opencontainers.image.created="2026-04-27T01:33:00Z"
LABEL org.opencontainers.image.source="https://github.com/ai-ready/monitoring-alerting"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.authors="devops-engineer@ai-ready.local"

# 构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# 设置构建元数据
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="ai-ready-prometheus" \
      org.label-schema.description="Prometheus monitoring for AI-Ready" \
      org.label-schema.url="https://ai-ready.local" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ai-ready/monitoring-alerting" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# 最终使用优化镜像
FROM optimized AS final

# 验证配置
RUN promtool check config /etc/prometheus/prometheus.yml

# 打印版本信息
RUN prometheus --version 2>&1 | grep prometheus