# 企智连性能测试执行手册

## 1. 环境准备

### 1.1 安装JMeter

```bash
# 下载JMeter 5.6.3
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip

# 解压
unzip apache-jmeter-5.6.3.zip -d /opt/

# 配置环境变量
export JMETER_HOME=/opt/apache-jmeter-5.6.3
export PATH=$PATH:$JMETER_HOME/bin
```

### 1.2 安装插件

1. 下载JMeter Plugins Manager
2. 安装以下插件：
   - Custom Thread Groups
   - 3 Basic Graphs
   - PerfMon Metrics Collector
   - Dummy Sampler

### 1.3 分布式环境配置

**Master节点配置**:
```bash
# jmeter.properties
remote_hosts=slave1:1099,slave2:1099,slave3:1099
server.rmi.ssl.disable=true
```

**Slave节点配置**:
```bash
# 启动Slave
./jmeter-server -Djava.rmi.server.hostname=slave1
```

## 2. 测试数据准备

### 2.1 用户数据

文件：`data/users.csv`
```csv
username,password
admin,admin123
test_user_001,Test@123
...
```

### 2.2 订单数据

文件：`data/orders.csv`
```csv
customerId,materialId,quantity,unitPrice
1,1,10,100.00
2,2,5,200.00
...
```

## 3. 测试执行

### 3.1 本地执行

```bash
# 登录测试
jmeter -n -t scripts/Login_Test.jmx -l results/login_result.jtl -e -o reports/login_report

# 订单测试
jmeter -n -t scripts/Order_Test.jmx -l results/order_result.jtl -e -o reports/order_report

# 报表测试
jmeter -n -t scripts/Report_Test.jmx -l results/report_result.jtl -e -o reports/report_report
```

### 3.2 分布式执行

```bash
# 在Master节点执行
jmeter -n -t scripts/Login_Test.jmx -R slave1,slave2,slave3 -l results/login_result.jtl
```

### 3.3 命令行参数

| 参数 | 说明 | 示例 |
|------|------|------|
| -n | 非GUI模式 | -n |
| -t | 指定jmx文件 | -t Login_Test.jmx |
| -l | 指定结果文件 | -l result.jtl |
| -e | 生成报告 | -e |
| -o | 指定报告目录 | -o report/ |
| -R | 远程Slave列表 | -R slave1,slave2 |
| -J | 设置属性 | -JthreadCount=100 |

## 4. 常用测试场景

### 4.1 基准测试

```bash
jmeter -n -t scripts/Login_Test.jmx \
  -JthreadCount=10 \
  -JrampUpTime=10 \
  -Jduration=300 \
  -l results/baseline.jtl
```

### 4.2 负载测试

```bash
jmeter -n -t scripts/Login_Test.jmx \
  -JthreadCount=100 \
  -JrampUpTime=60 \
  -Jduration=600 \
  -l results/load_test.jtl
```

### 4.3 压力测试

```bash
jmeter -n -t scripts/Login_Test.jmx \
  -JthreadCount=500 \
  -JrampUpTime=120 \
  -Jduration=600 \
  -l results/stress_test.jtl
```

### 4.4 峰值测试

```bash
jmeter -n -t scripts/Login_Test.jmx \
  -JthreadCount=1000 \
  -JrampUpTime=180 \
  -Jduration=300 \
  -l results/spike_test.jtl
```

## 5. 监控配置

### 5.1 服务器监控

在被测服务器上启动ServerAgent：
```bash
./startAgent.sh --udp-port 0 --tcp-port 4444
```

### 5.2 JMeter监听器配置

添加PerfMon Metrics Collector，配置服务器地址和端口。

## 6. 结果分析

### 6.1 查看HTML报告

```bash
# 生成报告
jmeter -g results/result.jtl -o reports/html_report

# 打开报告
open reports/html_report/index.html
```

### 6.2 关键指标

| 指标 | 说明 | 关注点 |
|------|------|--------|
| 响应时间 | 请求处理时间 | P50, P95, P99 |
| 吞吐量 | TPS/QPS | 每秒处理请求数 |
| 错误率 | 失败请求占比 | 应<1% |
| 并发数 | 同时在线用户数 | 系统承载能力 |

## 7. 常见问题

### 7.1 内存不足

```bash
# 修改jmeter.sh，增加内存
HEAP="-Xms2g -Xmx8g"
```

### 7.2 端口被占用

```bash
# 修改jmeter.properties
server_port=1099
server.rmi.localport=1099
```

### 7.3 报告生成失败

```bash
# 确保结果文件不为空
# 使用绝对路径
```

## 8. CI/CD集成

### 8.1 Jenkins Pipeline

```groovy
stage('Performance Test') {
    steps {
        sh '''
            jmeter -n -t scripts/Login_Test.jmx \
                -JthreadCount=${THREAD_COUNT} \
                -l results/result.jtl \
                -e -o reports/
        '''
    }
    post {
        always {
            publishHTML([...])
        }
    }
}
```

## 9. 性能优化建议

### 9.1 JMeter优化

1. 使用非GUI模式执行
2. 禁用不必要的监听器
3. 使用CSV Data Set Config替代User Defined Variables
4. 增加JVM内存
5. 使用分布式测试

### 9.2 脚本优化

1. 使用参数化数据
2. 添加适当的断言
3. 使用事务控制器
4. 合理设置思考时间

---

**文档版本**: v1.0  
**创建日期**: 2026-04-14  
**负责人**: QA Lead
