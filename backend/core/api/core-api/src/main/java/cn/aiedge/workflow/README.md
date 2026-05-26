# AI-Ready 审批流程模块文档

## 概述

本模块提供审批流程管理功能，支持流程定义、流程发起、审批操作、审批历史记录等功能。

## 模块结构

```
cn.aiedge.workflow
├── controller/
│   └── WorkflowController.java   # 审批流程接口
├── model/
│   ├── WorkflowDefinition.java   # 流程定义
│   ├── WorkflowInstance.java     # 流程实例
│   └── ApprovalRecord.java       # 审批记录
└── service/
    ├── WorkflowService.java      # 服务接口
    └── impl/
        └── WorkflowServiceImpl.java # 服务实现
```

## 内置流程

| 流程ID | 名称 | 类型 | 节点 |
|--------|------|------|------|
| order_approval | 订单审批流程 | order | 部门经理审批 |
| leave_approval | 请假审批流程 | leave | 部门主管审批 |
| expense_approval | 报销审批流程 | expense | 财务审批 |

## 流程节点类型

| 类型 | 说明 |
|------|------|
| start | 开始节点 |
| approval | 审批节点 |
| cc | 抄送节点 |
| condition | 条件节点 |
| end | 结束节点 |

## 审批人类型

| 类型 | 说明 |
|------|------|
| user | 指定用户 |
| role | 指定角色 |
| dept | 指定部门 |
| leader | 直接上级 |

## 审批方式

| 方式 | 说明 |
|------|------|
| any | 或签（任一人审批即可） |
| all | 会签（所有人都要审批） |
| sequence | 顺序签（按顺序审批） |

## API接口

### 1. 流程定义

```bash
# 获取流程定义列表
GET /api/workflow/definitions?type=order

# 获取流程定义详情
GET /api/workflow/definitions/{definitionId}

# 创建流程定义
POST /api/workflow/definitions
```

### 2. 发起流程

```bash
POST /api/workflow/start
Content-Type: application/json

{
  "definitionId": "order_approval",
  "businessType": "order",
  "businessId": "ORD001",
  "businessData": {
    "amount": 50000,
    "customer": "张三公司"
  }
}
```

响应示例：
```json
{
  "success": true,
  "instanceId": "abc123",
  "status": "approving",
  "message": "流程发起成功"
}
```

### 3. 待办/已办

```bash
# 获取我的待办
GET /api/workflow/pending?page=1&pageSize=20

# 获取待办数量
GET /api/workflow/pending/count

# 获取我的已办
GET /api/workflow/approved?page=1&pageSize=20

# 获取我发起的流程
GET /api/workflow/my-applications?page=1&pageSize=20
```

### 4. 审批操作

```bash
# 审批通过
POST /api/workflow/{instanceId}/approve
Content-Type: application/json

{
  "comment": "同意，金额合理"
}

# 审批拒绝
POST /api/workflow/{instanceId}/reject
Content-Type: application/json

{
  "comment": "金额过大，需要调整"
}

# 转交他人
POST /api/workflow/{instanceId}/transfer
Content-Type: application/json

{
  "toUserId": 100,
  "comment": "请财务总监审批"
}

# 撤回流程
POST /api/workflow/{instanceId}/withdraw
Content-Type: application/json

{
  "comment": "需要修改申请内容"
}
```

### 5. 审批记录

```bash
# 获取审批记录
GET /api/workflow/{instanceId}/records
```

响应示例：
```json
[
  {
    "recordId": 1,
    "nodeName": "发起申请",
    "approverName": "张三",
    "action": "submit",
    "comment": "提交审批申请",
    "approveTime": "2026-04-04T10:00:00",
    "status": "submitted"
  },
  {
    "recordId": 2,
    "nodeName": "部门经理审批",
    "approverName": "李四",
    "action": "approve",
    "comment": "同意",
    "approveTime": "2026-04-04T14:00:00",
    "status": "approved"
  }
]
```

## 流程状态

| 状态 | 说明 |
|------|------|
| pending | 待处理 |
| approving | 审批中 |
| approved | 已通过 |
| rejected | 已拒绝 |
| cancelled | 已取消 |
| withdrawn | 已撤回 |

## 使用示例

### Java代码调用

```java
@Service
public class OrderApprovalService {
    
    @Autowired
    private WorkflowService workflowService;
    
    public String submitOrderApproval(Order order, Long applicantId) {
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("orderId", order.getId());
        businessData.put("amount", order.getAmount());
        businessData.put("customer", order.getCustomerName());
        
        WorkflowInstance instance = workflowService.startWorkflow(
            "order_approval",
            "order",
            order.getId().toString(),
            businessData,
            applicantId,
            order.getTenantId()
        );
        
        return instance.getInstanceId();
    }
    
    public boolean approveOrder(String instanceId, Long userId, String comment) {
        return workflowService.approve(instanceId, userId, comment);
    }
}
```

### 前端集成

```javascript
// 发起流程
const startWorkflow = async (definitionId, businessType, businessId, businessData) => {
  const response = await fetch('/api/workflow/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      definitionId,
      businessType,
      businessId,
      businessData
    })
  });
  return await response.json();
};

// 获取待办列表
const getPendingApprovals = async (page = 1) => {
  const response = await fetch(`/api/workflow/pending?page=${page}`);
  return await response.json();
};

// 审批通过
const approve = async (instanceId, comment) => {
  const response = await fetch(`/api/workflow/${instanceId}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ comment })
  });
  return await response.json();
};
```

## 扩展说明

### 添加新流程类型

1. 创建流程定义对象
2. 定义审批节点
3. 注册到内置流程或保存到数据库

### 自定义审批规则

支持通过条件节点实现复杂的审批逻辑：

```java
WorkflowNode condition = new WorkflowNode();
condition.setNodeType("condition");
condition.setConditionExpression("amount > 10000");
condition.setBranches(List.of(
    new ConditionBranch("大额审批", "amount > 50000", "finance_director"),
    new ConditionBranch("普通审批", "amount <= 50000", "dept_manager")
));
```

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-04-04 | 初始版本，支持基础审批功能 |

---

*AI-Ready Team © 2026*
