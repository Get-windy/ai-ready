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
COPY backend/pom.xml backend/.mvn ./backend/
COPY backend/core/pom.xml ./backend/core/
COPY backend/erp/pom.xml ./backend/erp/
COPY backend/crm/pom.xml ./backend/crm/
COPY backend/user/pom.xml ./backend/user/
COPY backend/infrastructure/pom.xml ./backend/infrastructure/
COPY backend/tests/pom.xml ./backend/tests/
COPY backend/core/base/pom.xml ./backend/core/base/
COPY backend/core/common/pom.xml ./backend/core/common/
COPY backend/core/api/pom.xml ./backend/core/api/
WORKDIR /app/backend
RUN mvn dependency:go-offline -B -q || true
COPY backend/ .
RUN mvn clean package -DskipTests -B -q

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
