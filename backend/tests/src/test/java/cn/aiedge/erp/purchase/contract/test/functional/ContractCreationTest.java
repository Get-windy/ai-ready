package cn.aiedge.erp.purchase.contract.test.functional;

import cn.aiedge.erp.purchase.contract.test.data.TestDataFactory;
import cn.aiedge.erp.purchase.contract.test.framework.ApiTestBase;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * 采购合同创建功能测试
 * 测试合同创建API的各种场景
 */
@Epic("采购合同管理")
@Feature("合同创建功能")
@Tag("functional")
@Tag("contract-creation")
@DisplayName("采购合同创建功能测试")
public class ContractCreationTest extends ApiTestBase {
    
    @Test
    @Story("创建基础采购合同")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("测试创建基础采购合同")
    @Description("测试成功创建包含基本信息的采购合同")
    void testCreateBasicPurchaseContract() {
        // 生成测试数据
        TestDataFactory.PurchaseContractData contractData = TestDataFactory.generatePurchaseContractData();
        TestDataFactory.printDataSummary(contractData);
        
        // 记录测试步骤
        logStep("准备创建采购合同测试数据");
        logTestData("合同编号", contractData.getBasicInfo().getContractNumber());
        logTestData("供应商名称", contractData.getBasicInfo().getSupplierName());
        logTestData("合同总额", contractData.getBasicInfo().getTotalAmount());
        
        // 执行API调用
        logStep("执行创建采购合同API请求");
        Response response = createPurchaseContract(contractData);
        
        // 验证响应
        logStep("验证API响应");
        verifyStatusCode(response, 201); // 创建成功应该返回201
        
        // 验证响应字段
        verifyFieldNotNull(response, "data.id");
        verifyFieldNotNull(response, "data.contractNumber");
        verifyFieldNotNull(response, "data.status");
        verifyFieldValue(response, "data.contractNumber", contractData.getBasicInfo().getContractNumber());
        verifyFieldValue(response, "data.supplierName", contractData.getBasicInfo().getSupplierName());
        verifyFieldValue(response, "data.totalAmount", contractData.getBasicInfo().getTotalAmount());
        
        // 验证响应时间
        verifyResponseTime(response, 5000); // 响应时间应小于5秒
        
        // 提取并记录合同ID
        String contractId = extractFieldValue(response, "data.id");
        logTestData("创建的合同ID", contractId);
        
        // 验证合同状态为草稿
        verifyFieldValue(response, "data.status", "DRAFT");
        
        logStep("基础采购合同创建测试完成");
    }
    
    @Test
    @Story("创建带多个条目的采购合同")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("测试创建包含多个条目的采购合同")
    @Description("测试创建包含5个以上采购条目的复杂合同")
    void testCreateContractWithMultipleItems() {
        // 生成包含10个条目的合同数据
        TestDataFactory.PurchaseContractData contractData = 
            TestDataFactory.generatePurchaseContractData(TestDataFactory.ContractStatus.DRAFT, 10);
        
        logStep("准备创建包含多个条目的采购合同");
        logTestData("合同条目数量", contractData.getItems().size());
        logTestData("合同总额", contractData.getBasicInfo().getTotalAmount());
        
        // 执行API调用
        Response response = createPurchaseContract(contractData);
        
        // 验证响应
        verifyStatusCode(response, 201);
        verifyFieldNotNull(response, "data.id");
        verifyFieldNotNull(response, "data.items");
        
        // 验证条目数量
        Integer itemCount = extractFieldValue(response, "data.items.size()");
        logVerification("验证合同条目数量: " + itemCount);
        
        if (itemCount != null) {
            org.junit.jupiter.api.Assertions.assertEquals(10, itemCount, 
                "合同条目数量应等于10");
        }
        
        // 验证每个条目都有必要字段
        for (int i = 0; i < 10; i++) {
            String itemPath = "data.items[" + i + "]";
            verifyFieldNotNull(response, itemPath + ".itemCode");
            verifyFieldNotNull(response, itemPath + ".itemName");
            verifyFieldNotNull(response, itemPath + ".quantity");
            verifyFieldNotNull(response, itemPath + ".unitPrice");
        }
        
        logStep("多条目采购合同创建测试完成");
    }
    
    @Test
    @Story("创建带附件的采购合同")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("测试创建包含附件的采购合同")
    @Description("测试创建包含PDF、文档等附件的采购合同")
    void testCreateContractWithAttachments() {
        // 生成带附件的合同数据
        TestDataFactory.PurchaseContractData contractData = TestDataFactory.generatePurchaseContractData();
        contractData.setAttachments(TestDataFactory.generateAttachments(3));
        
        logStep("准备创建带附件的采购合同");
        logTestData("附件数量", contractData.getAttachments().size());
        logTestData("附件列表", contractData.getAttachments());
        
        // 执行API调用
        Response response = createPurchaseContract(contractData);
        
        // 验证响应
        verifyStatusCode(response, 201);
        verifyFieldNotNull(response, "data.id");
        verifyFieldNotNull(response, "data.attachments");
        
        // 验证附件数量
        Integer attachmentCount = extractFieldValue(response, "data.attachments.size()");
        if (attachmentCount != null) {
            org.junit.jupiter.api.Assertions.assertEquals(3, attachmentCount, 
                "附件数量应等于3");
        }
        
        // 验证附件信息
        for (int i = 0; i < 3; i++) {
            String attachmentPath = "data.attachments[" + i + "]";
            verifyFieldNotNull(response, attachmentPath + ".fileName");
            verifyFieldNotNull(response, attachmentPath + ".fileSize");
            verifyFieldNotNull(response, attachmentPath + ".uploadTime");
        }
        
        logStep("带附件采购合同创建测试完成");
    }
    
    @Test
    @Story("验证合同创建必填字段")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("测试合同创建必填字段验证")
    @Description("验证缺少必填字段时合同创建失败")
    void testRequiredFieldValidation() {
        logStep("测试缺少供应商ID的合同创建");
        
        // 准备缺少供应商ID的合同数据
        TestDataFactory.PurchaseContractData contractData = TestDataFactory.generatePurchaseContractData();
        contractData.getBasicInfo().setSupplierId(null);
        
        Response response = createPurchaseContract(contractData);
        
        // 验证响应为400错误
        verifyStatusCode(response, 400);
        verifyFieldNotNull(response, "error.code");
        verifyFieldNotEmpty(response, "error.message");
        verifyFieldValue(response, "error.code", "VALIDATION_ERROR");
        
        logStep("缺少供应商ID验证测试完成");
    }
    
    @Test
    @Story("验证合同金额计算")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("测试合同金额自动计算")
    @Description("验证合同总额自动根据条目金额计算")
    void testContractAmountCalculation() {
        // 生成测试数据
        TestDataFactory.PurchaseContractData contractData = TestDataFactory.generatePurchaseContractData(
            TestDataFactory.ContractStatus.DRAFT, 3);
        
        // 手动计算期望的总金额
        java.math.BigDecimal expectedTotal = contractData.getItems().stream()
            .map(item -> item.getAmount())
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        logStep("准备测试合同金额计算");
        logTestData("手动计算的合同总额", expectedTotal);
        logTestData("数据中的合同总额", contractData.getBasicInfo().getTotalAmount());
        
        // 执行API调用
        Response response = createPurchaseContract(contractData);
        
        // 验证响应
        verifyStatusCode(response, 201);
        
        // 验证合同总额是否正确计算
        java.math.BigDecimal actualTotal = extractFieldValue(response, "data.totalAmount");
        logVerification("验证合同总额计算: 期望=" + expectedTotal + ", 实际=" + actualTotal);
        
        if (actualTotal != null && expectedTotal != null) {
            org.junit.jupiter.api.Assertions.assertEquals(0, 
                actualTotal.compareTo(expectedTotal), 
                "合同总额应等于所有条目金额之和");
        }
        
        logStep("合同金额计算验证测试完成");
    }
    
    @Test
    @Story("并发创建合同测试")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("测试并发创建多个采购合同")
    @Description("验证系统处理并发创建合同请求的能力")
    void testConcurrentContractCreation() {
        logStep("开始并发创建合同测试");
        
        int concurrentRequests = 5;
        java.util.concurrent.ExecutorService executor = 
            java.util.concurrent.Executors.newFixedThreadPool(concurrentRequests);
        java.util.List<java.util.concurrent.Callable<Response>> tasks = new java.util.ArrayList<>();
        
        // 创建并发任务
        for (int i = 0; i < concurrentRequests; i++) {
            final int index = i;
            tasks.add(() -> {
                TestDataFactory.PurchaseContractData contractData = 
                    TestDataFactory.generatePurchaseContractData();
                contractData.getBasicInfo().setContractNumber("CONCURRENT-" + index + "-" + 
                    TestDataFactory.generateContractNumber());
                
                logTestData("并发任务 " + index + " 合同编号", 
                    contractData.getBasicInfo().getContractNumber());
                
                return createPurchaseContract(contractData);
            });
        }
        
        try {
            // 执行并发请求
            java.util.List<java.util.concurrent.Future<Response>> futures = 
                executor.invokeAll(tasks);
            
            int successCount = 0;
            int failureCount = 0;
            
            // 检查所有响应
            for (int i = 0; i < futures.size(); i++) {
                try {
                    Response response = futures.get(i).get();
                    if (response.getStatusCode() == 201) {
                        successCount++;
                        logStep("并发任务 " + i + " 成功创建合同");
                    } else {
                        failureCount++;
                        LOGGER.warn("并发任务 {} 创建合同失败: 状态码 {}", i, response.getStatusCode());
                    }
                } catch (Exception e) {
                    failureCount++;
                    LOGGER.error("并发任务 " + i + " 执行失败", e);
                }
            }
            
            // 验证结果
            logVerification("并发创建合同结果: 成功=" + successCount + ", 失败=" + failureCount);
            org.junit.jupiter.api.Assertions.assertTrue(successCount >= 3,
                "至少3个并发请求应成功");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("并发测试被中断", e);
        } finally {
            executor.shutdown();
        }
        
        logStep("并发创建合同测试完成");
    }
    
    @Test
    @Story("创建超大金额合同")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("测试创建超大金额采购合同")
    @Description("验证系统处理超大金额合同的能力")
    void testCreateLargeAmountContract() {
        // 生成超大金额合同数据
        TestDataFactory.PurchaseContractData contractData = TestDataFactory.generatePurchaseContractData();
        
        // 设置超大金额：1亿到10亿之间
        java.math.BigDecimal largeAmount = new java.math.BigDecimal(
            faker.number().randomDouble(2, 100000000, 1000000000));
        contractData.getBasicInfo().setTotalAmount(largeAmount);
        
        logStep("准备创建超大金额采购合同");
        logTestData("合同金额", largeAmount);
        
        // 执行API调用
        Response response = createPurchaseContract(contractData);
        
        // 验证响应
        verifyStatusCode(response, 201);
        
        // 验证金额正确保存
        java.math.BigDecimal returnedAmount = extractFieldValue(response, "data.totalAmount");
        logVerification("验证超大金额保存: 期望=" + largeAmount + ", 实际=" + returnedAmount);
        
        if (returnedAmount != null) {
            org.junit.jupiter.api.Assertions.assertEquals(0, 
                returnedAmount.compareTo(largeAmount),
                "超大金额应正确保存");
        }
        
        logStep("超大金额合同创建测试完成");
    }
    
    @Test
    @Story("创建长期合同")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("测试创建长期采购合同")
    @Description("验证创建长期（超过1年）合同的能力")
    void testCreateLongTermContract() {
        // 生成长期合同数据（2-3年）
        TestDataFactory.PurchaseContractData contractData = TestDataFactory.generatePurchaseContractData();
        
        java.time.LocalDate startDate = java.time.LocalDate.now().plusDays(30);
        java.time.LocalDate endDate = startDate.plusYears(faker.number().numberBetween(2, 3));
        
        contractData.getBasicInfo().setStartDate(startDate);
        contractData.getBasicInfo().setEndDate(endDate);
        
        logStep("准备创建长期采购合同");
        logTestData("合同开始日期", startDate);
        logTestData("合同结束日期", endDate);
        logTestData("合同期限", java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + " 天");
        
        // 执行API调用
        Response response = createPurchaseContract(contractData);
        
        // 验证响应
        verifyStatusCode(response, 201);
        
        // 验证日期正确保存
        String returnedStartDate = extractFieldValue(response, "data.startDate");
        String returnedEndDate = extractFieldValue(response, "data.endDate");
        
        logVerification("验证合同日期保存: 开始日期=" + returnedStartDate + ", 结束日期=" + returnedEndDate);
        
        if (returnedStartDate != null) {
            org.junit.jupiter.api.Assertions.assertEquals(startDate.toString(), returnedStartDate,
                "合同开始日期应正确保存");
        }
        
        if (returnedEndDate != null) {
            org.junit.jupiter.api.Assertions.assertEquals(endDate.toString(), returnedEndDate,
                "合同结束日期应正确保存");
        }
        
        logStep("长期合同创建测试完成");
    }
}