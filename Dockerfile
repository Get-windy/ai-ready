# ── 前端构建阶段 ──
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
RUN corepack enable
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
COPY frontend/packages ./packages
COPY frontend/apps/pc-admin ./apps/pc-admin
RUN pnpm install --frozen-lockfile && pnpm build

# ── 后端构建阶段 ──
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app
# 复制所有 pom.xml 用于依赖缓存（仅 pom 层，源码变更后此层可复用）
COPY backend/pom.xml ./backend/
COPY backend/core/pom.xml ./backend/core/
COPY backend/core/base/core-base/pom.xml ./backend/core/base/core-base/
COPY backend/core/api/core-api/pom.xml ./backend/core/api/core-api/
COPY backend/core/agent/core-agent/pom.xml ./backend/core/agent/core-agent/
COPY backend/core/platform/core-platform/pom.xml ./backend/core/platform/core-platform/
COPY backend/core/notification/core-notification/pom.xml ./backend/core/notification/core-notification/
COPY backend/core/payment/pom.xml ./backend/core/payment/
COPY backend/core/payment/core-payment/pom.xml ./backend/core/payment/core-payment/
COPY backend/erp/pom.xml ./backend/erp/
COPY backend/erp/erp-stock/pom.xml ./backend/erp/erp-stock/
COPY backend/erp/erp-purchase/pom.xml ./backend/erp/erp-purchase/
COPY backend/erp/erp-sales/pom.xml ./backend/erp/erp-sales/
COPY backend/erp/erp-partner/pom.xml ./backend/erp/erp-partner/
COPY backend/erp/erp-pricing/pom.xml ./backend/erp/erp-pricing/
COPY backend/erp/erp-finance/pom.xml ./backend/erp/erp-finance/
COPY backend/erp/erp-supplier-portal/pom.xml ./backend/erp/erp-supplier-portal/
COPY backend/erp/erp-printing/pom.xml ./backend/erp/erp-printing/
COPY backend/erp/erp-delivery-route/pom.xml ./backend/erp/erp-delivery-route/
COPY backend/erp/erp-observability/pom.xml ./backend/erp/erp-observability/
COPY backend/erp/erp-budget/pom.xml ./backend/erp/erp-budget/
COPY backend/erp/erp-b2b-mall/pom.xml ./backend/erp/erp-b2b-mall/
COPY backend/erp/erp-fixed-asset/pom.xml ./backend/erp/erp-fixed-asset/
COPY backend/crm/pom.xml ./backend/crm/
COPY backend/wms/pom.xml ./backend/wms/
COPY backend/dms/pom.xml ./backend/dms/
COPY backend/hr/pom.xml ./backend/hr/
COPY backend/hr/hr-base/pom.xml ./backend/hr/hr-base/
COPY backend/tests/pom.xml ./backend/tests/
WORKDIR /app/backend
RUN mvn dependency:go-offline -B -q || true
COPY backend/ .
RUN mvn clean package -DskipTests -B -q -Dfile.encoding=UTF-8

# ── 运行阶段 ──
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=backend-builder /app/backend/core/api/core-api/target/*-exec.jar app.jar
COPY --from=frontend-builder /app/frontend/apps/pc-admin/dist /app/static
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
