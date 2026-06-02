package cn.aiedge.erp.sale.service.integration;

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
 * 销售模块记账集成服务
 * 调用财务模块的BusinessAccountingController创建会计凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAccountingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Finance service base URL */
    private static final String FINANCE_BASE_URL = "http://localhost:8095";

    /**
     * 发货时创建应收和收入凭证
     * Dr. Accounts Receivable (应收账款)
     * Cr. Revenue (主营业务收入)
     *
     * @param shipmentId   发货单ID
     * @param shipmentNo   发货单编号
     * @param customerId   客户ID
     * @param customerName 客户名称
     * @param amount       金额
     */
    public void createReceivableOnShipment(Long shipmentId, String shipmentNo, String customerId,
                                           String customerName, BigDecimal amount) {
        log.info("创建销售应收及凭证: shipmentNo={}, customerId={}, amount={}", shipmentNo, customerId, amount);

        // 1. Create receivable via finance integration API
        Map<String, Object> receivableRequest = new HashMap<>();
        receivableRequest.put("sourceType", "SALE_SHIPMENT");
        receivableRequest.put("sourceId", shipmentId);
        receivableRequest.put("sourceNo", shipmentNo);
        receivableRequest.put("customerId", customerId);
        receivableRequest.put("customerName", customerName);
        receivableRequest.put("amount", amount);
        receivableRequest.put("dueDate", LocalDate.now().plusDays(30).toString());
        receivableRequest.put("summary", "销售发货 - " + shipmentNo);

        JsonNode receivableResult = callFinanceApi("/api/erp/finance/integration/receivable", receivableRequest);
        log.info("应收创建成功: {}", receivableResult);

        // 2. Create voucher via finance integration API
        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("sourceType", "SALE_SHIPMENT");
        voucherRequest.put("sourceId", shipmentId);
        voucherRequest.put("sourceNo", shipmentNo);
        voucherRequest.put("customerId", customerId);
        voucherRequest.put("customerName", customerName);
        voucherRequest.put("amount", amount);
        voucherRequest.put("summary", "销售出库凭证 - " + shipmentNo);
        voucherRequest.put("voucherDate", LocalDate.now().toString());

        // Accounting entries: Dr. AR, Cr. Revenue
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("summary", "销售出库");
        debitEntry.put("subjectCode", "1122");  // 应收账款
        debitEntry.put("debitAmount", amount);
        debitEntry.put("creditAmount", BigDecimal.ZERO);

        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("summary", "确认收入");
        creditEntry.put("subjectCode", "6001");  // 主营业务收入
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
