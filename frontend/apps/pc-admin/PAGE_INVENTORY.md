# pc-admin 页面清单

## 基础路由（无需登录）
1. /login - 登录页
2. /register - 注册页
3. /tenant-register - 企业注册
4. /403 - 无权限
5. /500 - 服务器错误

## 仪表盘
6. /dashboard - 工作台
7. /erp/dashboard - ERP仪表盘
8. /charts - 图表

## 系统管理
9. /system/user - 用户管理
10. /system/role - 角色管理
11. /system/menu - 菜单管理
12. /system/permission - 权限管理
13. /system/department - 部门管理
14. /system/position - 岗位管理
15. /system/config - 系统配置
16. /system/dict - 字典管理
17. /system/log - 操作日志
18. /system/tenant - 租户管理
19. /system/tenant-approval - 租户审批
20. /system/data-import - 数据导入
21. /admin/sys/permissions - 系统权限管理
22. /admin/tenant/permissions - 租户权限管理

## CRM模块
23. /crm/customer - 客户管理
24. /crm/contract - 合同管理
25. /crm/lead - 线索管理
26. /crm/opportunity - 商机管理
27. /crm/quotation - 报价管理
28. /crm/invoice - 发票管理
29. /crm/supplier - CRM供应商

## 采购管理
30. /purchase - 采购管理
31. /erp/purchase - ERP采购管理
32. /erp/purchase-exchange - 采购换货

## 销售管理
33. /sale - 销售管理
34. /erp/sale - ERP销售管理
35. /erp/sales-analysis - 销售分析
36. /erp/sales-report - 销售报表

## 库存管理
37. /stock - 库存管理
38. /erp/stock - ERP库存管理
39. /erp/stock-in - 入库管理
40. /erp/stocktake - 库存盘点
41. /erp/return - 退货管理
42. /erp/shipment - 发货管理
43. /erp/batch - 批次管理
44. /erp/serial - 序列号管理
45. /erp/stock-cost-adjust - 成本调整
46. /erp/stock-overflow - 盘盈管理
47. /erp/stock-damage - 报损管理
48. /erp/stock-transfer - 调拨管理
49. /erp/stock-replenishment - 补货管理
50. /erp/stock-alert-config - 库存预警
51. /erp/stock-bom - BOM管理
52. /erp/stock-assemble - 组装管理
53. /erp/stock-split - 拆分管理

## 财务管理
54. /finance - 财务管理
55. /finance/subject - 科目管理
56. /finance/voucher - 凭证管理
57. /finance/report - 财务报表
58. /finance/receivable - 应收账款
59. /finance/payable - 应付账款
60. /finance/receipt - 收款单
61. /finance/payment - 付款单
62. /finance/pre-receipt - 预收款
63. /finance/pre-payment - 预付款
64. /finance/deposit - 定金押金
65. /finance/write-off - 收付款核销
66. /finance/offset - 往来对冲
67. /finance/capital-flow - 资金流水
68. /finance/reconciliation - 对账
69. /finance/accounts-receivable - 应收明细
70. /finance/accounts-payable - 应付明细

## 产品管理
71. /erp/product - 产品管理
72. /erp/product/price-batch - 批量价格
73. /erp/product/inventory-mode - 库存模式

## 往来单位
74. /erp/partner - 往来单位管理
75. /supplier - 供应商管理
76. /supplier/inquiry - 供应商询价
77. /supplier/performance - 供应商绩效

## 定价管理
78. /erp/pricing/customer-grade - 客户等级定价
79. /erp/pricing/approval - 定价审批
80. /erp/pricing/tiers - 价格层级
81. /pricing - 定价中心

## 打印管理
82. /printing/template - 打印模板
83. /printing/chain - 打印链路
84. /printing/client - 打印客户端
85. /printing/task - 打印任务
86. /printing/designer - 模板设计器

## 固定资产
87. /fixed-asset - 固定资产总览
88. /fixed-asset/asset - 资产列表
89. /fixed-asset/category - 资产分类
90. /fixed-asset/depreciation - 折旧管理
91. /fixed-asset/transfer - 资产调拨
92. /fixed-asset/disposal - 资产处置
93. /fixed-asset/inventory - 资产盘点
94. /fixed-asset/report - 资产报表
95. /fixed-asset/purchase - 资产采购

## 费用管理
96. /erp/expense/application - 费用申请
97. /erp/expense/reimbursement - 费用报销
98. /erp/expense/approval - 费用审批
99. /erp/expense/payment - 费用付款
100. /erp/expense/statistics - 费用统计

## 预算管理
101. /budget - 预算总览
102. /budget/template - 预算模板
103. /budget/annual - 年度预算
104. /budget/adjustment - 预算调整
105. /budget/report - 预算报表

## 商城管理
106. /erp/mall/config - 商城配置
107. /erp/mall/user-audit - 用户审核
108. /erp/mall/banner - 轮播图管理
109. /erp/mall/order - 订单管理
110. /erp/mall/product - 商品管理

## 其他
111. /notification - 通知公告
112. /profile - 个人中心
113. /order-center - 订单中心
114. /workflow/instance-monitor - 流程监控
115. /workflow/task-management - 任务管理
116. /workflow/process-analysis - 流程分析

## 隐藏路由（详情页等）
117. /erp/product/:id - 产品详情
118. /erp/product/create - 新增产品
119. /purchase/order/:id - 采购订单详情
120. /purchase/inquiry/:id - 询价详情
121. /purchase/inbound/:id - 入库详情
122. /sale/order/:id - 销售订单详情
123. /crm/customer/:id - 客户详情
124. /stock/detail/:id - 库存详情
125. /erp/partner/:id - 单位详情
126. /erp/partner/create - 新增单位
127. /erp/stock-in/:id - 入库单详情
128. /erp/shipment/:id - 出库单详情
129. /erp/stocktake/:id - 盘点单详情
130. /erp/return/:id - 退货单详情
131. /erp/serial/:id - 序列号追溯
132. /supplier/detail/:id - 供应商详情
133. /supplier/create - 新增供应商
134. /supplier/edit/:id - 编辑供应商
135. /fixed-asset/asset/detail - 资产详情
136. /budget/annual/detail - 预算详情
137. /erp/expense/application/detail - 费用详情
138. /erp/expense/reimbursement/detail - 报销详情
