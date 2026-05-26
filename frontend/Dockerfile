# 多阶段构建 Dockerfile
# 阶段1: 构建阶段
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制包管理文件
COPY package.json pnpm-lock.yaml ./
COPY pnpm-workspace.yaml ./

# 安装 pnpm
RUN npm install -g pnpm

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制源代码
COPY . .

# 设置环境变量
ARG VITE_APP_ENV=production
ENV VITE_APP_ENV=${VITE_APP_ENV}

# 构建应用
RUN pnpm run build

# 阶段2: Nginx服务阶段
FROM nginx:alpine AS production

# 复制构建产物到Nginx目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/nginx.conf
COPY nginx.default.conf /etc/nginx/conf.d/default.conf

# 复制健康检查脚本
COPY healthcheck.sh /usr/local/bin/healthcheck.sh
RUN chmod +x /usr/local/bin/healthcheck.sh

# 创建非root用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 更改Nginx文件权限
RUN chown -R appuser:appgroup /var/cache/nginx && \
    chown -R appuser:appgroup /var/log/nginx && \
    chown -R appuser:appgroup /etc/nginx/conf.d

# 切换用户
USER appuser

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD /usr/local/bin/healthcheck.sh || exit 1

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]

# 阶段3: 开发阶段（可选）
FROM node:18-alpine AS development

# 设置工作目录
WORKDIR /app

# 复制包管理文件
COPY package.json pnpm-lock.yaml ./
COPY pnpm-workspace.yaml ./

# 安装 pnpm
RUN npm install -g pnpm

# 安装依赖（包括开发依赖）
RUN pnpm install --frozen-lockfile

# 复制源代码
COPY . .

# 暴露端口
EXPOSE 5173

# 启动开发服务器
CMD ["pnpm", "run", "dev"]