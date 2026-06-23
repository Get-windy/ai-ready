package cn.aiedge.erp.expense.service.integration;

import cn.aiedge.common.exception.BusinessException;
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
 * 费用模块记账集成服务
 * 调用财务模块的BusinessAccountingController创建费用凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseAccountingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Finance service base URL */
    private static final String FINANCE_BASE_URL = "http://localhost:8095";

    /**
     * 费用报销审批通过后创建凭证
     * Dr. Expense (管理费用/销售费用等)
     * Cr. Other Payable (其他应付款)
     *
     * @param applicationId   费用申请ID
     * @param applicationCode 费用申请编号
     * @param amount          金额
     * @param summary         摘要
     * @return 凭证编号
     */
    public String createExpenseVoucher(Long applicationId, String applicationCode,
                                       BigDecimal amount, String summary) {
        log.info("创建费用凭证: applicationCode={}, amount={}", applicationCode, amount);

        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("sourceType", "EXPENSE");
        voucherRequest.put("sourceId", applicationId);
        voucherRequest.put("sourceNo", applicationCode);
        voucherRequest.put("amount", amount);
        voucherRequest.put("summary", summary != null ? summary : "费用报销 - " + applicationCode);
        voucherRequest.put("voucherDate", LocalDate.now().toString());

        // Accounting entries: Dr. Expense, Cr. Other Payable
        // Use a default expense subject code (6602 - 管理费用) - subject can be refined per expense type
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("summary", "费用报销");
        debitEntry.put("subjectCode", "6602");  // 管理费用
        debitEntry.put("debitAmount", amount);
        debitEntry.put("creditAmount", BigDecimal.ZERO);

        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("summary", "应付报销款");
        creditEntry.put("subjectCode", "2241");  // 其他应付款
        creditEntry.put("debitAmount", BigDecimal.ZERO);
        creditEntry.put("creditAmount", amount);

        voucherRequest.put("items", new Map[]{debitEntry, creditEntry});

        JsonNode result = callFinanceApi("/api/erp/finance/integration/voucher", voucherRequest);
        JsonNode voucherNo = result.get("voucherNo");
        String voucherNoStr = voucherNo != null ? voucherNo.asText() : "";
        log.info("费用凭证创建成功: voucherNo={}", voucherNoStr);
        return voucherNoStr;
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
            throw BusinessException.badRequest("Finance API error: " + errMsg + " (path=" + path + ")");
        } catch (Exception e) {
            log.error("调用财务模块API失败: path={}", path, e);
            throw BusinessException.badRequest("调用财务模块API失败: " + e.getMessage());
        }
    }
}
