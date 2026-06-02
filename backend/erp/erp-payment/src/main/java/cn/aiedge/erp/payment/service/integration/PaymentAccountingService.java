package cn.aiedge.erp.payment.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 收付款模块记账集成服务
 * 调用财务模块的BusinessAccountingController创建收付款凭证并核销应收/应付
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAccountingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Finance service base URL */
    private static final String FINANCE_BASE_URL = "http://localhost:8095";

    /**
     * 付款时创建付款凭证并核销应付账款
     * Dr. Accounts Payable (应付账款)
     * Cr. Bank Deposit (银行存款)
     *
     * @param paymentId     付款单ID
     * @param paymentNo     付款单编号
     * @param supplierId    供应商ID
     * @param supplierName  供应商名称
     * @param amount        付款金额
     * @param payableId     应付款ID (用于核销)
     */
    public void postPaymentVoucher(Long paymentId, String paymentNo, String supplierId,
                                   String supplierName, BigDecimal amount, Long payableId) {
        log.info("创建付款凭证: paymentNo={}, amount={}", paymentNo, amount);

        // 1. Create payment voucher
        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("sourceType", "PAYMENT");
        voucherRequest.put("sourceId", paymentId);
        voucherRequest.put("sourceNo", paymentNo);
        voucherRequest.put("supplierId", supplierId);
        voucherRequest.put("supplierName", supplierName);
        voucherRequest.put("amount", amount);
        voucherRequest.put("summary", "付款 - " + paymentNo);
        voucherRequest.put("voucherDate", LocalDate.now().toString());

        // Accounting entries: Dr. AP, Cr. Bank Deposit
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("summary", "支付应付账款");
        debitEntry.put("subjectCode", "2202");  // 应付账款
        debitEntry.put("debitAmount", amount);
        debitEntry.put("creditAmount", BigDecimal.ZERO);

        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("summary", "银行存款");
        creditEntry.put("subjectCode", "1002");  // 银行存款
        creditEntry.put("debitAmount", BigDecimal.ZERO);
        creditEntry.put("creditAmount", amount);

        voucherRequest.put("items", new Map[]{debitEntry, creditEntry});

        JsonNode voucherResult = callFinanceApi("/api/erp/finance/integration/voucher", voucherRequest);
        log.info("付款凭证创建成功: {}", voucherResult);

        // 2. Write off the AP (PUT request)
        if (payableId != null) {
            Map<String, Object> writeOffRequest = new HashMap<>();
            writeOffRequest.put("amount", amount);
            callFinanceApiPut("/api/erp/finance/payable/" + payableId + "/write-off", writeOffRequest);
            log.info("应付核销成功: payableId={}", payableId);
        }
    }

    /**
     * 收款时创建收款凭证并核销应收账款
     * Dr. Bank Deposit (银行存款)
     * Cr. Accounts Receivable (应收账款)
     *
     * @param receiptId     收款单ID
     * @param receiptNo     收款单编号
     * @param customerId    客户ID
     * @param customerName  客户名称
     * @param amount        收款金额
     * @param receivableId  应收款ID (用于核销)
     */
    public void postReceiptVoucher(Long receiptId, String receiptNo, String customerId,
                                   String customerName, BigDecimal amount, Long receivableId) {
        log.info("创建收款凭证: receiptNo={}, amount={}", receiptNo, amount);

        // 1. Create receipt voucher
        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("sourceType", "RECEIPT");
        voucherRequest.put("sourceId", receiptId);
        voucherRequest.put("sourceNo", receiptNo);
        voucherRequest.put("customerId", customerId);
        voucherRequest.put("customerName", customerName);
        voucherRequest.put("amount", amount);
        voucherRequest.put("summary", "收款 - " + receiptNo);
        voucherRequest.put("voucherDate", LocalDate.now().toString());

        // Accounting entries: Dr. Bank Deposit, Cr. AR
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("summary", "银行存款");
        debitEntry.put("subjectCode", "1002");  // 银行存款
        debitEntry.put("debitAmount", amount);
        debitEntry.put("creditAmount", BigDecimal.ZERO);

        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("summary", "收回账款");
        creditEntry.put("subjectCode", "1122");  // 应收账款
        creditEntry.put("debitAmount", BigDecimal.ZERO);
        creditEntry.put("creditAmount", amount);

        voucherRequest.put("items", new Map[]{debitEntry, creditEntry});

        JsonNode voucherResult = callFinanceApi("/api/erp/finance/integration/voucher", voucherRequest);
        log.info("收款凭证创建成功: {}", voucherResult);

        // 2. Write off the AR (PUT request)
        if (receivableId != null) {
            Map<String, Object> writeOffRequest = new HashMap<>();
            writeOffRequest.put("amount", amount);
            callFinanceApiPut("/api/erp/finance/receivable/" + receivableId + "/write-off", writeOffRequest);
            log.info("应收核销成功: receivableId={}", receivableId);
        }
    }

    /**
     * 调用财务模块API (POST)
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

    /**
     * 调用财务模块API (PUT)
     */
    private JsonNode callFinanceApiPut(String path, Object request) {
        String url = FINANCE_BASE_URL + path;
        try {
            HttpEntity<Object> entity = new HttpEntity<>(request);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PUT, entity, JsonNode.class);
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
