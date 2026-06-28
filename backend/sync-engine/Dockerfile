FROM python:3.11-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 暴露API端口
EXPOSE 9800

# 默认以守护进程模式运行
CMD ["python", "main.py", "--mode", "daemon"]
