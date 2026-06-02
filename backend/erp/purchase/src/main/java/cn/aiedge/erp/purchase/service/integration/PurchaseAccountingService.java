package cn.aiedge.erp.purchase.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购模块记账集成服务
 * 调用财务模块的BusinessAccountingController创建会计凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseAccountingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Finance service base URL */
    private static final String FINANCE_BASE_URL = "http://localhost:8095";

    /**
     * 收货时创建应付和库存凭证
     * Dr. Inventory (存货)
     * Cr. Accounts Payable (应付账款)
     *
     * @param receiptId   收货单ID
     * @param receiptNo   收货单编号
     * @param supplierId  供应商ID
     * @param supplierName 供应商名称
     * @param amount      金额
     */
    public void createPayableOnReceipt(Long receiptId, String receiptNo, String supplierId,
                                       String supplierName, BigDecimal amount) {
        log.info("创建采购应付及凭证: receiptNo={}, supplierId={}, amount={}", receiptNo, supplierId, amount);

        // 1. Create payable via finance integration API
        Map<String, Object> payableRequest = new HashMap<>();
        payableRequest.put("sourceType", "PURCHASE_RECEIPT");
        payableRequest.put("sourceId", receiptId);
        payableRequest.put("sourceNo", receiptNo);
        payableRequest.put("supplierId", supplierId);
        payableRequest.put("supplierName", supplierName);
        payableRequest.put("amount", amount);
        payableRequest.put("dueDate", LocalDate.now().plusDays(30).toString());
        payableRequest.put("summary", "采购收货 - " + receiptNo);

        JsonNode payableResult = callFinanceApi("/api/erp/finance/integration/payable", payableRequest);
        log.info("应付创建成功: {}", payableResult);

        // 2. Create voucher via finance integration API
        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("sourceType", "PURCHASE_RECEIPT");
        voucherRequest.put("sourceId", receiptId);
        voucherRequest.put("sourceNo", receiptNo);
        voucherRequest.put("supplierId", supplierId);
        voucherRequest.put("supplierName", supplierName);
        voucherRequest.put("amount", amount);
        voucherRequest.put("summary", "采购入库凭证 - " + receiptNo);
        voucherRequest.put("voucherDate", LocalDate.now().toString());

        // Accounting entries: Dr. Inventory, Cr. AP
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("summary", "采购入库");
        debitEntry.put("subjectCode", "1403");  // 原材料
        debitEntry.put("debitAmount", amount);
        debitEntry.put("creditAmount", BigDecimal.ZERO);

        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("summary", "采购入库");
        creditEntry.put("subjectCode", "2202");  // 应付账款
        creditEntry.put("debitAmount", BigDecimal.ZERO);
        creditEntry.put("creditAmount", amount);

        voucherRequest.put("items", new Map[]{debitEntry, creditEntry});

        JsonNode voucherResult = callFinanceApi("/api/erp/finance/integration/voucher", voucherRequest);
        log.info("凭证创建成功: {}", voucherResult);
    }

    /**
     * 调用财务模块API
     */
    private JsonNode callFinanceApi(String path, Object request) {
        String url = FINANCE_BASE_URL + path;
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, request, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.has("code") && body.get("code").asInt() == 200) {
                return body.get("data");
            }
            String errMsg = body != null ? body.path("message").asText("Unknown error") : "No response";
            throw new RuntimeException("Finance API error: " + errMsg + " (path=" + path + ")");
        } catch (Exception e) {
            log.error("调用财务模块API失败: path={}", path, e);
            throw new RuntimeException("调用财务模块API失败: " + e.getMessage(), e);
        }
    }
}
