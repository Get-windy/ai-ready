# AI-Ready 监控告警模块 - AlertManager Dockerfile
# 版本: v0.26.0
# 最后更新: 2026-04-27

# 使用官方AlertManager镜像作为基础
FROM prom/alertmanager:v0.26.0 AS base

# 设置环境变量
ENV ALERTMANAGER_VERSION=v0.26.0
ENV ALERTMANAGER_HOME="/alertmanager"
ENV ALERTMANAGER_CONFIG_FILE="/etc/alertmanager/alertmanager.yml"
ENV ALERTMANAGER_DATA_DIR="/alertmanager/data"
ENV ALERTMANAGER_STORAGE_PATH="/alertmanager/data"
ENV ALERTMANAGER_WEB_LISTEN_ADDRESS=":9093"
ENV ALERTMANAGER_CLUSTER_LISTEN_ADDRESS=":9094"
ENV ALERTMANAGER_EXTERNAL_URL="http://localhost:9093"
ENV ALERTMANAGER_CLUSTER_PEER=""
ENV ALERTMANAGER_LOG_LEVEL="info"
ENV ALERTMANAGER_LOG_FORMAT="json"

# 设置工作目录
WORKDIR $ALERTMANAGER_HOME

# 创建必要的目录
RUN mkdir -p $ALERTMANAGER_DATA_DIR \
    && mkdir -p /etc/alertmanager \
    && mkdir -p /etc/alertmanager/templates \
    && mkdir -p $ALERTMANAGER_HOME/silences \
    && chown -R nobody:nobody $ALERTMANAGER_HOME \
    && chown -R nobody:nobody $ALERTMANAGER_DATA_DIR \
    && chown -R nobody:nobody /etc/alertmanager

# 复制配置文件
COPY alertmanager.yml $ALERTMANAGER_CONFIG_FILE
COPY templates/*.tmpl /etc/alertmanager/templates/

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
EXPOSE 9093 9094

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD ["/healthcheck.sh"]

# 设置入口点
ENTRYPOINT ["/entrypoint.sh"]

# 默认命令
CMD ["--config.file=/etc/alertmanager/alertmanager.yml", \
     "--storage.path=/alertmanager/data", \
     "--web.external-url=http://localhost:9093", \
     "--cluster.listen-address=:9094", \
     "--log.level=info", \
     "--log.format=json"]

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
LABEL org.opencontainers.image.title="AI-Ready AlertManager"
LABEL org.opencontainers.image.description="AlertManager for AI-Ready monitoring system"
LABEL org.opencontainers.image.version="${ALERTMANAGER_VERSION}"
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
      org.label-schema.name="ai-ready-alertmanager" \
      org.label-schema.description="AlertManager for AI-Ready monitoring alerts" \
      org.label-schema.url="https://ai-ready.local" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ai-ready/monitoring-alerting" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# 最终使用优化镜像
FROM optimized AS final

# 验证配置
RUN amtool check-config $ALERTMANAGER_CONFIG_FILE

# 打印版本信息
RUN alertmanager --version 2>&1 | grep alertmanager

# 创建默认静默规则
COPY --chown=nobody:nobody silences/default.json $ALERTMANAGER_HOME/silences/

# 设置环境变量默认值
ENV ALERTMANAGER_CLUSTER_ENABLED="true"
ENV ALERTMANAGER_CLUSTER_ADVERTISE_ADDRESS=""
ENV ALERTMANAGER_CLUSTER_GOSSIP_INTERVAL="200ms"
ENV ALERTMANAGER_CLUSTER_PUSH_PULL_INTERVAL="1m0s"
ENV ALERTMANAGER_CLUSTER_TCP_TIMEOUT="10s"
ENV ALERTMANAGER_CLUSTER_PROBE_TIMEOUT="500ms"
ENV ALERTMANAGER_CLUSTER_PROBE_INTERVAL="1s"
ENV ALERTMANAGER_CLUSTER_SETTLE_TIMEOUT="1m0s"
ENV ALERTMANAGER_CLUSTER_REJOIN_INTERVAL="10s"
ENV ALERTMANAGER_WEB_GET_CONCURRENCY="0"
ENV ALERTMANAGER_WEB_TIMEOUT="0"
ENV ALERTMANAGER_WEB_MAX_CONNS="512"
ENV ALERTMANAGER_WEB_EXTERNAL_URL="http://localhost:9093"
ENV ALERTMANAGER_DATA_RETENTION="120h"
ENV ALERTMANAGER_ALERT_GC_INTERVAL="30m"
ENV ALERTMANAGER_NOTIFICATION_GC_INTERVAL="5m"
ENV ALERTMANAGER_SILENCE_GC_INTERVAL="5m"
ENV ALERTMANAGER_ROUTE_PREFIX="/"
ENV ALERTMANAGER_WEB_PREFIX="/"
ENV ALERTMANAGER_WEB_TELEMETRY_PATH="/metrics"
ENV ALERTMANAGER_WEB_TELEMETRY_ENABLED="true"
ENV ALERTMANAGER_WEB_CORS_ORIGIN=".*"
ENV ALERTMANAGER_WEB_CORS_CREDENTIALS="true"
ENV ALERTMANAGER_WEB_CORS_MAX_AGE="7200"
ENV ALERTMANAGER_LOG_LEVEL="info"
ENV ALERTMANAGER_LOG_FORMAT="json"
ENV ALERTMANAGER_LOG_SOURCE_LOCATION="false"
ENV ALERTMANAGER_LOG_DISABLE_TIMESTAMP="false"
ENV ALERTMANAGER_LOG_DISABLE_COLOR="true"
ENV ALERTMANAGER_LOG_OUTPUT="stderr"
ENV ALERTMANAGER_LOG_JSON_FIELDS=""
ENV ALERTMANAGER_LOG_JSON_FIELDS_NAMES=""

# 优化启动脚本
COPY --chown=nobody:nobody scripts/optimize-startup.sh /optimize-startup.sh
RUN chmod +x /optimize-startup.sh \
    && /optimize-startup.sh

# 创建配置检查脚本
COPY --chown=nobody:nobody scripts/check-config.sh /check-config.sh
RUN chmod +x /check-config.sh

# 验证集群配置
RUN if [ "$ALERTMANAGER_CLUSTER_ENABLED" = "true" ]; then \
    echo "Cluster mode enabled, checking configuration..." \
    && amtool cluster status --alertmanager.url=http://localhost:9093 2>/dev/null || true; \
    else \
    echo "Cluster mode disabled"; \
    fi