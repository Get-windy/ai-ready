package cn.aiedge.erp.fixedasset.service.integration;

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
 * 固定资产模块记账集成服务
 * 调用财务模块的BusinessAccountingController创建折旧凭证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedAssetAccountingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Finance service base URL */
    private static final String FINANCE_BASE_URL = "http://localhost:8095";

    /**
     * 计提折旧时创建凭证
     * Dr. Expense (管理费用/制造费用等 - 折旧费)
     * Cr. Accumulated Depreciation (累计折旧)
     *
     * @param assetId      资产ID
     * @param assetCode    资产编号
     * @param assetName    资产名称
     * @param amount       折旧金额
     * @param fiscalYear   会计年度
     * @param fiscalPeriod 会计期间
     * @return 凭证编号
     */
    public String postDepreciationVoucher(Long assetId, String assetCode, String assetName,
                                          BigDecimal amount, Integer fiscalYear, Integer fiscalPeriod) {
        log.info("创建折旧凭证: assetCode={}, amount={}, period={}-{}",
                assetCode, amount, fiscalYear, fiscalPeriod);

        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("sourceType", "FIXED_ASSET_DEPRECIATION");
        voucherRequest.put("sourceId", assetId);
        voucherRequest.put("sourceNo", assetCode);
        voucherRequest.put("amount", amount);
        voucherRequest.put("summary", "计提折旧 - " + assetName + "(" + assetCode + ")");
        voucherRequest.put("voucherDate", LocalDate.now().toString());

        // Accounting entries: Dr. Expense (折旧费), Cr. Accumulated Depreciation
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("summary", "计提折旧费用");
        debitEntry.put("subjectCode", "6604");  // 折旧费
        debitEntry.put("debitAmount", amount);
        debitEntry.put("creditAmount", BigDecimal.ZERO);

        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("summary", "累计折旧");
        creditEntry.put("subjectCode", "1602");  // 累计折旧
        creditEntry.put("debitAmount", BigDecimal.ZERO);
        creditEntry.put("creditAmount", amount);

        voucherRequest.put("items", new Map[]{debitEntry, creditEntry});

        JsonNode result = callFinanceApi("/api/erp/finance/integration/voucher", voucherRequest);
        JsonNode voucherNo = result.get("voucherNo");
        String voucherNoStr = voucherNo != null ? voucherNo.asText() : "";
        log.info("折旧凭证创建成功: voucherNo={}", voucherNoStr);
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
            throw new RuntimeException("Finance API error: " + errMsg + " (path=" + path + ")");
        } catch (Exception e) {
            log.error("调用财务模块API失败: path={}", path, e);
            throw new RuntimeException("调用财务模块API失败: " + e.getMessage(), e);
        }
    }
}
