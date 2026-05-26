# AI-Ready 监控告警模块 - Grafana Dockerfile
# 版本: v9.5.0
# 最后更新: 2026-04-27

# 使用官方Grafana镜像作为基础
FROM grafana/grafana:9.5.0 AS base

# 设置环境变量
ENV GF_VERSION=9.5.0
ENV GF_PATHS_HOME="/usr/share/grafana"
ENV GF_PATHS_DATA="/var/lib/grafana"
ENV GF_PATHS_LOGS="/var/log/grafana"
ENV GF_PATHS_PLUGINS="/var/lib/grafana/plugins"
ENV GF_PATHS_PROVISIONING="/etc/grafana/provisioning"
ENV GF_PATHS_CONFIG="/etc/grafana/grafana.ini"

# 设置工作目录
WORKDIR $GF_PATHS_HOME

# 创建必要的目录
RUN mkdir -p $GF_PATHS_DATA \
    && mkdir -p $GF_PATHS_LOGS \
    && mkdir -p $GF_PATHS_PLUGINS \
    && mkdir -p $GF_PATHS_PROVISIONING \
    && mkdir -p /etc/grafana/dashboards \
    && chown -R grafana:grafana $GF_PATHS_DATA \
    && chown -R grafana:grafana $GF_PATHS_LOGS \
    && chown -R grafana:grafana $GF_PATHS_PLUGINS \
    && chown -R grafana:grafana $GF_PATHS_PROVISIONING \
    && chown -R grafana:grafana /etc/grafana

# 复制配置文件
COPY grafana.ini $GF_PATHS_CONFIG
COPY provisioning/ $GF_PATHS_PROVISIONING/
COPY dashboards/ /etc/grafana/dashboards/

# 复制自定义脚本
COPY scripts/entrypoint.sh /entrypoint.sh
COPY scripts/healthcheck.sh /healthcheck.sh

# 设置脚本权限
RUN chmod +x /entrypoint.sh \
    && chmod +x /healthcheck.sh \
    && chown grafana:grafana /entrypoint.sh \
    && chown grafana:grafana /healthcheck.sh

# 安装必要的插件
RUN grafana-cli plugins install grafana-piechart-panel \
    && grafana-cli plugins install grafana-clock-panel \
    && grafana-cli plugins install mtanda-histogram-panel \
    && grafana-cli plugins install grafana-worldmap-panel \
    && grafana-cli plugins install satellogic-3d-globe-panel \
    && grafana-cli plugins install volkovlabs-variable-panel \
    && grafana-cli plugins install yesoreyeram-boomtable-panel \
    && grafana-cli plugins install marcusolsson-dynamictext-panel

# 清理插件缓存
RUN grafana-cli --pluginsDir "$GF_PATHS_PLUGINS" plugins update-all \
    && rm -rf /tmp/*

# 切换为grafana用户
USER grafana

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD ["/healthcheck.sh"]

# 设置入口点
ENTRYPOINT ["/entrypoint.sh"]

# 默认命令
CMD ["/run.sh"]

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
    python3 \
    py3-pip \
    && pip3 install grafana-api-client \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 安装Grafana渲染器（用于导出PDF/PNG）
RUN grafana-cli plugins install grafana-image-renderer

# 清理缓存
RUN rm -rf /var/cache/apk/* \
    && rm -rf /root/.cache/pip

# 切换回grafana用户
USER grafana

# 标签信息
LABEL org.opencontainers.image.title="AI-Ready Grafana"
LABEL org.opencontainers.image.description="Grafana visualization platform for AI-Ready monitoring"
LABEL org.opencontainers.image.version="${GF_VERSION}"
LABEL org.opencontainers.image.created="2026-04-27T01:33:00Z"
LABEL org.opencontainers.image.source="https://github.com/ai-ready/monitoring-alerting"
LABEL org.opencontainers.image.licenses="AGPL-3.0"
LABEL org.opencontainers.image.authors="devops-engineer@ai-ready.local"

# 构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# 设置构建元数据
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="ai-ready-grafana" \
      org.label-schema.description="Grafana dashboards for AI-Ready monitoring" \
      org.label-schema.url="https://ai-ready.local" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ai-ready/monitoring-alerting" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"

# 最终使用优化镜像
FROM optimized AS final

# 验证插件安装
RUN grafana-cli plugins ls

# 打印版本信息
RUN grafana-server -v 2>&1 | grep Version

# 创建默认数据源配置文件
COPY --chown=grafana:grafana provisioning/datasources/prometheus.yaml $GF_PATHS_PROVISIONING/datasources/

# 创建默认仪表盘配置文件
COPY --chown=grafana:grafana provisioning/dashboards/dashboards.yaml $GF_PATHS_PROVISIONING/dashboards/

# 预加载仪表盘
RUN mkdir -p /etc/grafana/dashboards/default \
    && cp -r /etc/grafana/dashboards/* /etc/grafana/dashboards/default/ 2>/dev/null || true

# 设置环境变量默认值
ENV GF_SECURITY_ADMIN_USER="admin"
ENV GF_SECURITY_ADMIN_PASSWORD="admin"
ENV GF_SECURITY_SECRET_KEY="ai-ready-grafana-secret-key"
ENV GF_AUTH_ANONYMOUS_ENABLED="false"
ENV GF_AUTH_BASIC_ENABLED="true"
ENV GF_USERS_ALLOW_SIGN_UP="false"
ENV GF_USERS_ALLOW_ORG_CREATE="false"
ENV GF_USERS_AUTO_ASSIGN_ORG="true"
ENV GF_USERS_AUTO_ASSIGN_ORG_ROLE="Viewer"
ENV GF_AUTH_PROXY_ENABLED="false"
ENV GF_SERVER_PROTOCOL="http"
ENV GF_SERVER_HTTP_PORT="3000"
ENV GF_SERVER_DOMAIN="localhost"
ENV GF_SERVER_ROOT_URL="%(protocol)s://%(domain)s:%(http_port)s/"
ENV GF_SERVER_SERVE_FROM_SUB_PATH="false"
ENV GF_SERVER_ENABLE_GZIP="true"
ENV GF_SERVER_ROUTER_LOGGING="true"
ENV GF_DATABASE_TYPE="sqlite3"
ENV GF_DATABASE_PATH="/var/lib/grafana/grafana.db"
ENV GF_DATABASE_MAX_IDLE_CONN="2"
ENV GF_DATABASE_MAX_OPEN_CONN="10"
ENV GF_DATABASE_CONN_MAX_LIFETIME="14400"
ENV GF_SESSION_PROVIDER="file"
ENV GF_SESSION_PROVIDER_CONFIG="/var/lib/grafana/sessions"
ENV GF_SESSION_COOKIE_NAME="grafana_sess"
ENV GF_SESSION_COOKIE_SECURE="false"
ENV GF_SESSION_LASTING_COOKIE="false"
ENV GF_ANALYTICS_REPORTING_ENABLED="false"
ENV GF_ANALYTICS_CHECK_FOR_UPDATES="false"
ENV GF_ANALYTICS_CHECK_FOR_PLUGIN_UPDATES="false"
ENV GF_SNAPSHOTS_EXTERNAL_ENABLED="false"
ENV GF_LOG_MODE="console"
ENV GF_LOG_LEVEL="info"
ENV GF_LOG_CONSOLE_FORMAT="json"
ENV GF_PATHS_DATA="/var/lib/grafana"
ENV GF_PATHS_LOGS="/var/log/grafana"
ENV GF_PATHS_PLUGINS="/var/lib/grafana/plugins"
ENV GF_PATHS_PROVISIONING="/etc/grafana/provisioning"

# 优化启动脚本
COPY --chown=grafana:grafana scripts/optimize-startup.sh /optimize-startup.sh
RUN chmod +x /optimize-startup.sh \
    && /optimize-startup.sh

# 验证配置
RUN grafana-cli --config "$GF_PATHS_CONFIG" admin reset-admin-password --homepath "$GF_PATHS_HOME" --config "$GF_PATHS_CONFIG" admin