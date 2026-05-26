# 构建测试容器镜像
docker build -t ai-ready-test-container:latest -f Dockerfile .

# 运行测试容器
docker run -it --rm ai-ready-test-container:latest

# 运行测试容器并挂载本地代码
docker run -it --rm \
  -v "$(pwd)":/app \
  -w /app \
  ai-ready-test-container:latest \
  pytest tests/ -v

# 运行测试容器并生成HTML报告
docker run -it --rm \
  -v "$(pwd)":/app \
  -w /app \
  ai-ready-test-container:latest \
  pytest tests/ -v --html=reports/report.html --self-contained-html

# 查看容器日志
docker logs ai-ready-test-container

# 进入容器调试
docker run -it --rm ai-ready-test-container:latest sh

# 启动并进入bash
docker run -it --rm ai-ready-test-container:latest bash
