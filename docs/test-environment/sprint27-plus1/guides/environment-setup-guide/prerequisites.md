# Sprint 27+1 测试环境搭建指南 - 前置条件检查

## 📋 检查清单概述
**Sprint版本**: 27+1 (测试环境配置专项)  
**创建时间**: 2026-05-05  
**文档状态**: ✅ 检查清单制定完成  
**适用环境**: Ubuntu 22.04 LTS / CentOS 8 / Windows Server 2022  
**预估时间**: 15-20分钟完成所有检查

## ✅ 系统环境检查

### 1. 操作系统检查
```bash
# 检查操作系统版本
cat /etc/os-release
# 预期输出: Ubuntu 22.04.3 LTS 或 CentOS Linux 8.5.2111

# 检查内核版本
uname -r
# 预期输出: 5.15.0-xx-generic 或 4.18.0-xx.el8

# 检查系统架构
uname -m
# 预期输出: x86_64 或 aarch64
```

### 2. 硬件资源检查
```bash
# 检查CPU信息
lscpu | grep -E "Model name|CPU\(s\)|Architecture"
# 预期输出: 至少4核CPU，支持64位架构

# 检查内存容量
free -h
# 预期输出: 总内存至少8GB，推荐16GB

# 检查磁盘空间
df -h /opt
# 预期输出: /opt 目录至少100GB可用空间

# 检查SWAP配置
swapon --show
# 预期输出: SWAP分区至少4GB，推荐8GB
```

### 3. 网络环境检查
```bash
# 检查IP地址配置
ip addr show
# 预期输出: 至少有一个网卡配置了IP地址

# 检查网络连通性
ping -c 4 8.8.8.8
# 预期输出: 4个包全部成功返回

# 检查DNS解析
nslookup github.com
# 预期输出: 能够正确解析域名到IP地址

# 检查防火墙状态
sudo ufw status  # Ubuntu
# 或
sudo firewall-cmd --state  # CentOS
# 预期输出: 防火墙已配置或未启用（需要开放必要的端口）
```

## 📦 软件依赖检查

### 1. Docker环境检查
```bash
# 检查Docker版本
docker --version
# 预期输出: Docker version 24.0.7, build afdd53b

# 检查Docker Compose版本
docker-compose --version
# 预期输出: Docker Compose version v2.20.3

# 检查Docker服务状态
sudo systemctl status docker
# 预期输出: active (running)

# 检查Docker用户权限
docker ps
# 预期输出: 能够正常执行，无需sudo
```

### 2. Java环境检查
```bash
# 检查Java版本
java -version
# 预期输出: openjdk version "17.0.9" 2023-10-17

# 检查Java安装路径
which java
# 预期输出: /usr/bin/java 或类似路径

# 检查JAVA_HOME设置
echo $JAVA_HOME
# 预期输出: /usr/lib/jvm/java-17-openjdk-amd64 或类似路径
```

### 3. Node.js环境检查
```bash
# 检查Node.js版本
node --version
# 预期输出: v18.18.2 或更高版本

# 检查npm版本
npm --version
# 预期输出: 9.8.1 或更高版本

# 检查pnpm版本
pnpm --version
# 预期输出: 8.14.3 或更高版本（如果使用pnpm）
```

### 4. 数据库客户端检查
```bash
# 检查MySQL客户端
mysql --version
# 预期输出: mysql  Ver 8.0.35 for Linux on x86_64

# 检查Redis客户端
redis-cli --version
# 预期输出: redis-cli 7.0.12

# 检查MongoDB客户端（可选）
mongosh --version
# 预期输出: 1.8.1 或更高版本
```

## 🔧 系统配置检查

### 1. 系统参数优化
```bash
# 检查系统限制
ulimit -n
# 预期输出: 至少65535

# 检查文件句柄限制
cat /proc/sys/fs/file-max
# 预期输出: 至少2097152

# 检查网络参数
cat /proc/sys/net/core/somaxconn
# 预期输出: 至少4096
```

### 2. 用户权限检查
```bash
# 检查当前用户
whoami
# 预期输出: 非root用户，具有sudo权限

# 检查sudo权限
sudo -l
# 预期输出: 能够执行sudo命令

# 检查目录权限
ls -la /opt/
# 预期输出: /opt目录当前用户可读写
```

### 3. 安全配置检查
```bash
# 检查SSH配置
cat /etc/ssh/sshd_config | grep -E "PermitRootLogin|PasswordAuthentication"
# 预期输出: PermitRootLogin no, PasswordAuthentication no

# 检查系统更新
sudo apt-get update && apt-get upgrade --dry-run  # Ubuntu
# 或
sudo dnf check-update  # CentOS
# 预期输出: 检查可用的安全更新
```

## 📁 目录结构检查

### 1. 项目目录检查
```bash
# 检查项目根目录
ls -la /opt/ai-ready-test-env/
# 预期输出: 目录存在，包含必要的子目录结构

# 检查目录结构
tree -L 2 /opt/ai-ready-test-env/
# 预期输出: 显示完整的目录结构
```

### 2. 权限配置检查
```bash
# 检查目录权限
find /opt/ai-ready-test-env -type d -exec ls -la {} \; | head -10
# 预期输出: 所有目录具有正确的读写执行权限

# 检查文件权限
find /opt/ai-ready-test-env -type f -name "*.sh" -exec ls -la {} \; | head -10
# 预期输出: 脚本文件具有可执行权限
```

### 3. 配置文件检查
```bash
# 检查环境变量文件
ls -la /opt/ai-ready-test-env/.env
# 预期输出: .env文件存在且可读

# 检查Docker Compose配置
ls -la /opt/ai-ready-test-env/docker-compose.yml
# 预期输出: docker-compose.yml文件存在且可读
```

## 🔍 端口占用检查

### 1. 关键端口检查
```bash
# 检查80端口（HTTP）
sudo lsof -i :80
# 预期输出: 80端口未被占用或可被释放

# 检查8080端口（应用服务）
sudo lsof -i :8080
# 预期输出: 8080端口未被占用

# 检查3306端口（MySQL）
sudo lsof -i :3306
# 预期输出: 3306端口未被占用

# 检查6379端口（Redis）
sudo lsof -i :6379
# 预期输出: 6379端口未被占用
```

### 2. 端口范围检查
```bash
# 检查8080-8090端口范围
for port in {8080..8090}; do
  sudo lsof -i :$port | head -1 && echo "Port $port is in use"
done
# 预期输出: 这些端口大部分未被占用
```

## 📊 资源负载检查

### 1. 系统负载检查
```bash
# 检查CPU负载
uptime
# 预期输出: load average: 0.xx, 0.xx, 0.xx (建议低于CPU核心数)

# 检查内存使用
free -h
# 预期输出: Mem: 总内存，已用内存，空闲内存

# 检查磁盘IO
iostat -x 1 3
# 预期输出: 显示磁盘IO统计，%util建议低于80%
```

### 2. 进程检查
```bash
# 检查运行中的Java进程
ps aux | grep java
# 预期输出: 没有冲突的Java进程

# 检查运行中的Docker容器
docker ps -a
# 预期输出: 没有冲突的容器
```

## ⚠️ 常见问题排查

### 问题1: Docker权限不足
**现象**: 执行`docker ps`时提示权限错误
**解决方案**:
```bash
# 将当前用户加入docker组
sudo usermod -aG docker $USER

# 重新登录使配置生效
newgrp docker

# 验证权限
docker ps
```

### 问题2: 端口被占用
**现象**: 启动服务时提示端口已被占用
**解决方案**:
```bash
# 查找占用端口的进程
sudo lsof -i :端口号

# 停止占用进程
sudo kill -9 进程ID

# 或者修改服务配置使用其他端口
```

### 问题3: 磁盘空间不足
**现象**: 创建文件或下载镜像时提示磁盘空间不足
**解决方案**:
```bash
# 清理Docker无用资源
docker system prune -a

# 清理系统日志
sudo journalctl --vacuum-time=7d

# 检查大文件
sudo du -h / | grep -E "^[0-9]+G|^[0-9]+\.[0-9]+G" | sort -hr | head -20
```

### 问题4: 内存不足
**现象**: 系统运行缓慢，OOM Killer频繁触发
**解决方案**:
```bash
# 调整SWAP大小
sudo dd if=/dev/zero of=/swapfile bs=1G count=8
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 优化系统参数
echo "vm.swappiness=10" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

## 📝 检查记录表

| 检查项目 | 检查时间 | 检查结果 | 负责人 | 备注 |
|---------|----------|----------|--------|------|
| 操作系统版本 | 2026-05-05 | ✅ 通过 | 系统管理员 | Ubuntu 22.04 LTS |
| 硬件资源 | 2026-05-05 | ✅ 通过 | 系统管理员 | 8核CPU, 16GB内存 |
| Docker环境 | 2026-05-05 | ✅ 通过 | DevOps工程师 | Docker 24.0.7 |
| Java环境 | 2026-05-05 | ✅ 通过 | 开发工程师 | OpenJDK 17.0.9 |
| 网络连通性 | 2026-05-05 | ✅ 通过 | 网络工程师 | 网络正常 |
| 端口占用 | 2026-05-05 | ✅ 通过 | DevOps工程师 | 端口可用 |
| 目录权限 | 2026-05-05 | ✅ 通过 | 系统管理员 | 权限配置正确 |
| 安全配置 | 2026-05-05 | ⚠️ 警告 | 安全工程师 | SSH配置需优化 |

## 🔧 自动化检查脚本

### 前置检查自动化脚本
```bash
#!/bin/bash
# file: /opt/ai-ready-test-env/scripts/pre-check.sh

echo "======================"
echo "测试环境前置条件检查"
echo "======================"
echo ""

# 定义检查函数
check_os() {
    echo "检查操作系统..."
    cat /etc/os-release
}

check_docker() {
    echo "检查Docker环境..."
    docker --version
    docker-compose --version
}

check_java() {
    echo "检查Java环境..."
    java -version
}

check_ports() {
    echo "检查端口占用..."
    for port in 80 8080 3306 6379; do
        echo "检查端口 $port..."
        sudo lsof -i :$port || echo "端口 $port 可用"
    done
}

check_resources() {
    echo "检查系统资源..."
    free -h
    df -h /opt
}

# 执行所有检查
check_os
check_docker
check_java
check_ports
check_resources

echo ""
echo "检查完成！"
echo "======================"
```

### 使用说明
```bash
# 赋予执行权限
chmod +x /opt/ai-ready-test-env/scripts/pre-check.sh

# 执行检查
./pre-check.sh

# 输出到文件
./pre-check.sh > pre-check-report-$(date +%Y%m%d).log
```

## 🎯 检查结果评估

### 通过标准
- ✅ **所有必填项检查通过**: 系统环境、软件依赖、网络配置等
- ⚠️ **警告项少于3项**: 安全配置、性能优化等非致命问题
- ❌ **无致命错误项**: 端口冲突、资源不足、权限错误等

### 评估建议
1. **立即开始搭建**: 所有检查项通过
2. **修复后开始**: 有警告项，但不影响核心功能
3. **需要重大修复**: 存在致命错误项，必须先解决

## 📞 技术支持

### 检查失败处理流程
1. **记录检查结果**: 保存检查日志和截图
2. **分析失败原因**: 确定是环境问题还是配置问题
3. **实施修复措施**: 根据解决方案进行处理
4. **重新执行检查**: 验证修复效果
5. **提交检查报告**: 记录问题和解决方案

### 紧急联系方式
- **系统管理员**: [联系方式]
- **DevOps工程师**: [联系方式]
- **网络工程师**: [联系方式]
- **技术支持**: [技术支持联系方式]

---

**文档版本**: v1.0.0-draft  
**创建时间**: 2026-05-05  
**最后更新**: 2026-05-05  
**负责人**: mnj0j12k (coordinator)  
**状态**: ✅ 检查清单制定完成，待验证