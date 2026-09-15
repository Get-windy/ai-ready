package cn.aiedge.trade.channel;

import cn.aiedge.trade.dto.ExternalOrderDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 订单回调报文解析（各渠道适配器共用）
 *
 * <p>**报文契约**：按 {@link ExternalOrderDTO} 的字段名解析 JSON 回调
 * （`externalOrderId` / `externalOrderNo` / `orderAmount` / `paidAmount` / `orderTime` /
 * `buyerName` / `receiverName` / `receiverPhone`），兼容常见的等价键
 * （`orderId` / `orderNo` / `tid` / `trade_id`）。</p>
 *
 * <p>平台专有报文（如淘宝 TOP 消息、京东 JOS）需各自适配；未识别的键**忽略而非报错**，
 * 由调用方（`ExternalOrderService.receiveCallback`）对「缺外部订单号」给出明确业务错误——
 * 此前该场景直接落到 `external_order_raw.external_order_id` NOT NULL 约束上，表现为 500。</p>
 */
@Slf4j
public final class CallbackPayloadParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 外部订单号候选键（按优先级） */
    private static final String[] ORDER_ID_KEYS = {
            "externalOrderId", "externalOrderNo", "orderId", "orderNo", "tid", "trade_id"
    };
    private static final String[] ORDER_NO_KEYS = {
            "externalOrderNo", "orderNo", "tradeNo", "tid"
    };

    private CallbackPayloadParser() {
    }

    /**
     * 用回调报文补全 DTO 字段（解析失败仅告警，不抛异常）
     *
     * @param dto    待补全的 DTO（可为空则新建）
     * @param payload 原始报文（JSON）
     * @return 补全后的 DTO
     */
    public static ExternalOrderDTO apply(ExternalOrderDTO dto, String payload) {
        ExternalOrderDTO target = dto == null ? new ExternalOrderDTO() : dto;
        target.setRawJson(payload);
        JsonNode root = parse(payload);
        if (root == null) {
            return target;
        }
        if (!StringUtils.hasText(target.getExternalOrderId())) {
            target.setExternalOrderId(firstText(root, ORDER_ID_KEYS));
        }
        if (!StringUtils.hasText(target.getExternalOrderNo())) {
            target.setExternalOrderNo(firstText(root, ORDER_NO_KEYS));
        }
        if (target.getOrderAmount() == null) {
            target.setOrderAmount(decimal(root, "orderAmount", "totalFee", "payment"));
        }
        if (target.getPaidAmount() == null) {
            target.setPaidAmount(decimal(root, "paidAmount", "payment", "totalFee"));
        }
        if (target.getBuyerName() == null) {
            target.setBuyerName(firstText(root, new String[]{"buyerName", "buyerNick", "buyer_nick"}));
        }
        if (target.getReceiverName() == null) {
            target.setReceiverName(firstText(root, new String[]{"receiverName", "receiver_name"}));
        }
        if (target.getReceiverPhone() == null) {
            target.setReceiverPhone(firstText(root, new String[]{"receiverPhone", "receiver_phone", "mobile"}));
        }
        if (target.getOrderTime() == null) {
            target.setOrderTime(dateTime(root, new String[]{"orderTime", "created", "createTime"}));
        }
        return target;
    }

    /** 仅提取外部订单号（缺号判定用） */
    public static String extractOrderId(String payload) {
        JsonNode root = parse(payload);
        return root == null ? null : firstText(root, ORDER_ID_KEYS);
    }

    private static JsonNode parse(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return MAPPER.readTree(payload);
        } catch (Exception e) {
            log.debug("[渠道回调] 报文非 JSON，交由平台专有适配处理: {}", e.getMessage());
            return null;
        }
    }

    private static String firstText(JsonNode root, String[] keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull() && StringUtils.hasText(node.asText())) {
                return node.asText();
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && node.isNumber()) {
                return node.decimalValue();
            }
        }
        return null;
    }

    private static LocalDateTime dateTime(JsonNode root, String[] keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                continue;
            }
            String text = node.asText().trim().replace('T', ' ');
            if (!StringUtils.hasText(text)) {
                continue;
            }
            try {
                return LocalDateTime.parse(text.replace(' ', 'T'));
            } catch (DateTimeParseException e) {
                try {
                    return LocalDateTime.parse(text, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException ignored) {
                    // 时间格式不识别则留空，不阻塞回调入库
                }
            }
        }
        return null;
    }
}
