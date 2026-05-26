# 价格策略模块安全测试方法设计

## 1. 静态代码分析

### 1.1 源代码安全扫描

#### 1.1.1 扫描工具配置
```yaml
# SonarQube 配置文件
sonar.projectKey: erp-pricing-module
sonar.projectName: "ERP价格策略模块"
sonar.projectVersion: 1.0.0

# 扫描范围
sonar.sources: backend/erp/erp-sale/src/main/java
sonar.tests: backend/erp/erp-sale/src/test/java
sonar.java.binaries: backend/erp/erp-sale/target/classes

# 安全规则集
sonar.java.security.repositories: OWASP-2021, CWE/SANS-TOP25
sonar.qualitygate: security_high

# 排除文件
sonar.exclusions: **/target/**, **/*.min.js, **/*.css
```

#### 1.1.2 关键安全规则
| 规则类别 | 规则数量 | 优先级 | 示例规则 |
|----------|----------|--------|----------|
| SQL注入防护 | 15 | 高 | 使用参数化查询 |
| XSS防护 | 12 | 高 | 输出编码 |
| 命令注入 | 8 | 高 | 避免Runtime.exec |
| 路径遍历 | 6 | 高 | 文件路径验证 |
| 密码硬编码 | 5 | 高 | 避免明文密码 |
| 弱加密 | 7 | 中 | 使用强加密算法 |
| 日志泄露 | 4 | 中 | 敏感数据脱敏 |

### 1.2 依赖组件安全扫描

#### 1.2.1 依赖漏洞扫描配置
```xml
<!-- Maven安全扫描插件配置 -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.2</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <cveValidForHours>24</cveValidForHours>
        <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 1.2.2 关键依赖安全检查
| 依赖组件 | 版本 | 安全扫描 | 风险等级 |
|----------|------|----------|----------|
| Spring Security | 6.2.0 | CVE-2024-22234 | 低 |
| MyBatis | 3.5.13 | CVE-2024-16845 | 中 |
| FastJSON | 1.2.83 | CVE-2024-12345 | 高 |
| Log4j | 2.17.2 | CVE-2021-44228 | 高 |

## 2. 动态安全测试

### 2.1 OWASP ZAP自动化扫描

#### 2.1.1 ZAP扫描配置
```python
# ZAP自动化扫描脚本
from zapv2 import ZAPv2

# ZAP配置
zap = ZAPv2(proxies={'http': 'http://localhost:8080'})

# 扫描目标配置
target_url = "http://localhost:8080/api/sale/pricing"

# 上下文配置
context_name = "pricing-context"
context_id = zap.context.new_context(context_name)

# 包含URL模式
zap.context.include_in_context(context_name, ".*/api/sale/pricing/.*")

# 认证配置
zap.authentication.set_authentication_method(
    context_id, 
    "httpAuthentication",
    {
        "hostname": "localhost",
        "port": "8080",
        "realm": "ERP Pricing API"
    }
)
```

#### 2.1.2 扫描策略设计
| 扫描类型 | 攻击强度 | 扫描深度 | 测试用例数 |
|----------|----------|----------|------------|
| SQL注入 | 高 | 完整 | 1200+ |
| XSS | 高 | 完整 | 800+ |
| 路径遍历 | 中 | 完整 | 200+ |
| CSRF | 中 | 完整 | 150+ |
| 命令注入 | 高 | 完整 | 300+ |

### 2.2 渗透测试方案

#### 2.2.1 黑盒渗透测试
**第一阶段：信息收集**
```bash
# 1. 端口扫描
nmap -sV -sC -p 80,443,8080 target.com

# 2. 目录爆破
gobuster dir -u http://target.com -w /usr/share/wordlists/dirb/common.txt

# 3. API接口发现
katana -u http://target.com/api/sale/pricing -jc -aff
```

**第二阶段：漏洞探测**
```bash
# 1. SQL注入测试
sqlmap -u "http://target.com/api/sale/pricing/strategies?id=1" --batch --level=5

# 2. XSS测试
xsstrike -u "http://target.com/api/sale/pricing" --crawl

# 3. 权限绕过测试
使用Burp Suite测试不同用户角色的权限边界
```

**第三阶段：深入测试**
```bash
# 1. 业务逻辑漏洞测试
测试价格计算公式解析逻辑
测试策略优先级逻辑
测试折扣计算逻辑

# 2. 数据泄露测试
测试敏感数据访问控制
测试日志信息泄露
测试错误信息泄露
```

#### 2.2.2 白盒渗透测试
**代码审计重点：**
```java
// 1. SQL注入风险点审计
@GetMapping("/strategies")
public Result<Page<PriceStrategyDTO>> pageStrategies(
        @RequestParam(required = false) String name) {
    // 危险：字符串拼接SQL
    // String sql = "SELECT * FROM erp_pricing_strategy WHERE name LIKE '%" + name + "%'";
    
    // 安全：使用MyBatis参数化查询
    // Page<PriceStrategyDTO> result = mapper.selectByCondition(name);
}

// 2. XSS风险点审计
@PostMapping("/strategies")
public Result<Long> createStrategy(@RequestBody PriceStrategyDTO dto) {
    // 危险：直接输出用户输入
    // return Result.ok(dto.getDescription());
    
    // 安全：HTML编码输出
    // String safeDesc = HtmlUtils.htmlEscape(dto.getDescription());
}
```

### 2.3 业务逻辑安全测试

#### 2.3.1 价格计算逻辑测试
**测试用例设计：**
```java
public class PriceCalculationSecurityTest {
    
    @Test
    public void testNegativePriceCalculation() {
        // 测试负价格计算
        PriceCalculationRequest request = new PriceCalculationRequest();
        request.setBasePrice(new BigDecimal("-100.00"));
        request.setDiscountRate(new BigDecimal("0.10"));
        
        // 预期：应拒绝负价格或转换为0
        assertThrows(IllegalArgumentException.class, () -> 
            service.calculatePrice(request));
    }
    
    @Test
    public void testExcessiveDiscount() {
        // 测试过度折扣
        PriceCalculationRequest request = new PriceCalculationRequest();
        request.setBasePrice(new BigDecimal("1000.00"));
        request.setDiscountRate(new BigDecimal("5.00")); // 500%折扣
        
        // 预期：折扣率应在合理范围内
        assertThrows(BusinessException.class, () -> 
            service.calculatePrice(request));
    }
    
    @Test
    public void testFormulaInjection() {
        // 测试公式注入攻击
        PriceStrategyDTO strategy = new PriceStrategyDTO();
        strategy.setFormulaConfig("{\"formula\": \"price * 0.9; System.exit(0)\"}");
        
        // 预期：公式解析应安全，不能执行系统命令
        assertThrows(SecurityException.class, () -> 
            service.validateFormula(strategy.getFormulaConfig()));
    }
}
```

#### 2.3.2 策略优先级逻辑测试
**测试用例设计：**
```java
@Test
public void testPriorityBypassAttack() {
    // 测试优先级绕过攻击
    // 场景：低优先级用户创建高优先级策略
    Long lowPriorityUserId = 1001L;
    PriceStrategyDTO strategy = createTestStrategy();
    strategy.setPriority(1); // 最高优先级
    
    // 模拟低权限用户请求
    mockUserContext(lowPriorityUserId, "SALES_USER");
    
    // 预期：应拒绝低权限用户设置高优先级
    assertThrows(AccessDeniedException.class, () -> 
        pricingController.createStrategy(strategy));
}
```

## 3. 安全审计设计

### 3.1 配置安全审计

#### 3.1.1 应用配置审计
**审计检查清单：**
```yaml
# 应用配置审计清单
security_config_audit:
  - item: "数据库连接加密"
    check: "是否使用SSL/TLS"
    severity: "高"
    
  - item: "API密钥管理"
    check: "密钥是否硬编码"
    severity: "高"
    
  - item: "会话配置"
    check: "会话超时时间是否合理"
    severity: "中"
    
  - item: "CORS配置"
    check: "是否过于宽松"
    severity: "中"
    
  - item: "错误处理"
    check: "是否泄露敏感信息"
    severity: "高"
```

#### 3.1.2 环境配置审计
**审计脚本：**
```bash
#!/bin/bash
# 环境安全审计脚本

echo "=== 环境安全审计开始 ==="

# 1. 检查文件权限
echo "检查文件权限..."
find /app/config -type f -perm /o+w -ls | head -10

# 2. 检查敏感文件
echo "检查敏感文件..."
find /app -name "*.properties" -o -name "*.yml" -o -name "*.yaml" | xargs grep -l "password\|secret\|key" 2>/dev/null

# 3. 检查网络配置
echo "检查网络配置..."
netstat -tulpn | grep -E ":(80|443|8080|8443)"

echo "=== 环境安全审计结束 ==="
```

### 3.2 合规性审计

#### 3.2.1 数据保护合规审计
**GDPR合规检查清单：**
```markdown
## GDPR合规审计清单

### 数据最小化原则
- [ ] 价格策略数据是否只收集必要信息
- [ ] 数据保留期限是否符合要求
- [ ] 数据访问是否受控

### 用户权利保障
- [ ] 是否支持数据访问权（Right of Access）
- [ ] 是否支持数据删除权（Right to Erasure）
- [ ] 是否支持数据可携权（Right to Data Portability）

### 数据处理记录
- [ ] 数据处理活动是否有记录
- [ ] 数据泄露是否及时上报
- [ ] 数据保护影响评估是否完成
```

#### 3.2.2 安全基线合规审计
**安全基线检查脚本：**
```python
# 安全基线检查脚本
import requests
import json

def check_security_baseline(api_url):
    baseline_checks = [
        {
            "name": "TLS配置",
            "check": lambda: check_tls_config(api_url),
            "severity": "high"
        },
        {
            "name": "安全头配置",
            "check": lambda: check_security_headers(api_url),
            "severity": "medium"
        },
        {
            "name": "API认证",
            "check": lambda: check_api_auth(api_url),
            "severity": "high"
        }
    ]
    
    results = []
    for check in baseline_checks:
        try:
            result = check["check"]()
            results.append({
                "name": check["name"],
                "result": result,
                "severity": check["severity"]
            })
        except Exception as e:
            results.append({
                "name": check["name"],
                "result": f"检查失败: {str(e)}",
                "severity": check["severity"]
            })
    
    return results
```

## 4. 测试执行策略

### 4.1 测试周期安排

#### 4.1.1 日常测试
| 测试类型 | 频率 | 执行时间 | 负责人 |
|----------|------|----------|--------|
| 单元安全测试 | 每次提交 | 开发时 | 开发人员 |
| 代码安全扫描 | 每日 | 夜间 | CI/CD |
| API安全扫描 | 每日 | 夜间 | 安全团队 |

#### 4.1.2 定期测试
| 测试类型 | 频率 | 执行时间 | 负责人 |
|----------|------|----------|--------|
| 渗透测试 | 每月 | 第二周 | 安全团队 |
| 配置审计 | 每月 | 第三周 | 运维团队 |
| 合规审计 | 每季度 | 季度末 | 合规团队 |

### 4.2 测试环境管理

#### 4.2.1 环境分类
```yaml
# 测试环境配置
environments:
  dev:
    purpose: "开发测试"
    data: "模拟数据"
    isolation: "项目隔离"
    
  staging:
    purpose: "集成测试"
    data: "脱敏数据"
    isolation: "环境隔离"
    
  security:
    purpose: "安全测试"
    data: "攻击载荷数据"
    isolation: "完全隔离"
```

#### 4.2.2 数据管理
```sql
-- 安全测试数据管理
-- 1. 创建测试用户
INSERT INTO users (username, password_hash, role) VALUES
('security_tester', 'hashed_password', 'SECURITY_TESTER'),
('admin_attacker', 'hashed_password', 'ADMIN'),
('sales_user', 'hashed_password', 'SALES_USER');

-- 2. 创建测试价格策略
INSERT INTO erp_pricing_strategy (name, formula_config, tenant_id) VALUES
('测试策略1', '{"formula": "price * 0.9"}', 1),
('测试策略2', '{"formula": "price * 0.8"}', 1),
('跨租户策略', '{"formula": "price * 0.7"}', 2);  -- 不同租户
```

## 5. 测试结果分析

### 5.1 漏洞严重程度分级

| 等级 | 描述 | 响应时间 | 影响范围 |
|------|------|----------|----------|
| 严重 | 可导致系统完全失控 | 24小时内 | 全部功能 |
| 高危 | 可导致重大数据泄露 | 48小时内 | 核心功能 |
| 中危 | 存在安全风险 | 1周内 | 部分功能 |
| 低危 | 轻微安全问题 | 2周内 | 边缘功能 |

### 5.2 测试报告模板

```markdown
# 安全测试报告

## 测试概述
- 测试时间: 2026-05-01
- 测试范围: 价格策略模块
- 测试方法: 渗透测试 + 代码审计

## 测试结果统计
| 漏洞等级 | 发现数量 | 修复数量 | 修复率 |
|----------|----------|----------|--------|
| 严重 | 2 | 2 | 100% |
| 高危 | 5 | 4 | 80% |
| 中危 | 8 | 6 | 75% |
| 低危 | 12 | 10 | 83% |

## 关键发现
1. **严重漏洞**: SQL注入风险
   - 位置: PricingController.pageStrategies
   - 修复: 使用参数化查询
   
2. **高危漏洞**: 权限绕过
   - 位置: 策略删除接口
   - 修复: 加强权限验证

## 改进建议
1. 加强输入验证
2. 完善审计日志
3. 定期安全培训
```

---

**文档版本**: 1.0.0  
**创建时间**: 2026-05-01  
**创建人**: 安全测试团队  
**适用范围**: 价格策略模块安全测试方法设计