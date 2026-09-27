package cn.aiedge.erp.printing.support;

import cn.aiedge.erp.printing.entity.SetPrintConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 把「打印设置」作用到打印数据上（服务端唯一实现）。
 *
 * <p>口径与前端 {@code PrintDialog/printBehavior.ts} 的 {@code applyPrintBehavior} 逐条对齐，
 * 但**规则只留这一份**：打印数据现在由 {@code PrintDataProvider} 在服务端装配，
 * 前端那份只服务于尚未迁移的「页面自己给数据」兼容模式，属于过渡遗留。</p>
 *
 * <p>生效项：</p>
 * <ul>
 *   <li><b>单据打印小数位数</b>（{@code decimalEnabled=1} 时）——数量列按 {@code qtyDecimal}、
 *       单价列按 {@code priceDecimal} 定长格式化；<b>金额类字段永不格式化</b>（另有一套口径）。</li>
 *   <li><b>打印内容</b> —— 明细行注入 {@code batchEffectiveText}（拼接口径见
 *       {@link PrintContentOptions}），顶层注入 {@code printContentText} 供表头引用。</li>
 * </ul>
 *
 * <p>⚠️ 原地加工传入的对象。调用方传进来的必须是**本次请求新装配**的打印数据
 * （{@code PrintDataProvider#load} 每次现建），不要拿缓存对象或实体本身传进来。</p>
 */
public final class PrintBehaviorApplier {

    /** 数量列字段名（大小写不敏感的包含匹配；中文键一并支持） */
    private static final Pattern QTY_KEY = Pattern.compile("(qty|quantity|数量)", Pattern.CASE_INSENSITIVE);

    /** 单价列字段名（{@code unitPrice}/{@code price}/{@code salePrice}/… ；中文键一并支持） */
    private static final Pattern PRICE_KEY = Pattern.compile("(price|单价)", Pattern.CASE_INSENSITIVE);

    /**
     * 金额类字段永不格式化：这些键即使名字里同时含 price/qty 也排除
     * （如 {@code discountAmount} / {@code taxAmount} / {@code totalAmount}）。
     *
     * <p>只按「金额」语义排除，不排除 {@code total}/{@code tax} 词根 ——
     * {@code totalQty}（数量）、{@code unitPriceWithTax}（含税单价）都属数量/单价口径，应当格式化。</p>
     */
    private static final Pattern AMOUNT_KEY =
            Pattern.compile("(amount|money|subtotal|金额|合计|小计)", Pattern.CASE_INSENSITIVE);

    /** 下钻深度上限（打印数据是「单据 + 明细数组」的浅结构） */
    private static final int MAX_DEPTH = 4;

    /** 合法的小数位（与 {@code PrintConfigService.DECIMAL_MIN/MAX} 同口径：0~4） */
    private static final int MAX_DIGITS = 4;

    /** 明细行里可能的字段别名（各模块实体命名不完全一致，只收敛同义名） */
    private static final Map<String, List<String>> ROW_ALIASES = Map.of(
            "batchNo", List.of("batchNo", "batchCode", "batchNumber", "lotNo", "lotNumber", "批号"),
            "productionDate", List.of("productionDate", "produceDate", "productionTime", "mfgDate", "生产日期"),
            "expiryDate", List.of("expiryDate", "expireDate", "expirationDate", "validUntil", "validDate", "到期日期"),
            "qty", List.of("qty", "quantity", "billQty", "outQty", "inQty", "数量"));

    private PrintBehaviorApplier() {
    }

    /**
     * 按打印设置原地加工打印数据。
     *
     * @param data 本次请求新装配的打印数据（会被就地修改）
     * @param cfg  当前租户的打印设置；为 null 时原样返回（等同未启用任何加工）
     */
    public static void apply(Map<String, Object> data, SetPrintConfig cfg) {
        if (data == null || cfg == null) {
            return;
        }
        boolean decimalOn = isOn(cfg.getDecimalEnabled());
        Integer qtyDigits = decimalOn ? normalizeDigits(cfg.getQtyDecimal()) : null;
        Integer priceDigits = decimalOn ? normalizeDigits(cfg.getPriceDecimal()) : null;

        String printContent = cfg.getPrintContent() == null ? "" : cfg.getPrintContent().trim();
        List<String> contentFields = PrintContentOptions.FIELDS.get(printContent);

        if (qtyDigits == null && priceDigits == null && contentFields == null) {
            return;
        }

        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        walk(data, data, qtyDigits, priceDigits, contentFields, 0, seen);

        if (contentFields != null) {
            data.put("printContentText", printContent);
        }
    }

    @SuppressWarnings("unchecked")
    private static void walk(Object root, Object node, Integer qtyDigits, Integer priceDigits,
                             List<String> contentFields, int depth, Set<Object> seen) {
        if (depth > MAX_DEPTH || node == null) {
            return;
        }
        if (!(node instanceof Map || node instanceof List)) {
            return;
        }
        if (!seen.add(node)) {
            return;   // 环保护
        }

        if (node instanceof List<?> list) {
            for (Object item : list) {
                walk(root, item, qtyDigits, priceDigits, contentFields, depth + 1, seen);
            }
            return;
        }

        Map<String, Object> record = (Map<String, Object>) node;

        // ① 数量 / 单价列定长格式化（金额类字段不碰）
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            Object value = entry.getValue();
            if (value == null || value instanceof Map || value instanceof List) {
                continue;
            }
            String key = entry.getKey();
            if (qtyDigits != null && isQtyKey(key)) {
                entry.setValue(formatDecimal(value, qtyDigits));
            } else if (priceDigits != null && isPriceKey(key)) {
                entry.setValue(formatDecimal(value, priceDigits));
            }
        }

        // ② 「打印内容」只作用于明细行（有批次/效期字段的行）；根对象是单据头，不注入批次字段
        if (contentFields != null && record != root) {
            String text = buildBatchEffectiveText(record, contentFields);
            if (text != null) {
                record.put("batchEffectiveText", text);
            }
        }

        // ③ 继续下钻
        for (Object value : record.values()) {
            if (value instanceof Map || value instanceof List) {
                walk(root, value, qtyDigits, priceDigits, contentFields, depth + 1, seen);
            }
        }
    }

    /**
     * 拼一行明细的「批次效期」文本（字段按选项文案顺序用空格连接，末尾 {@code *数量}）。
     *
     * <p>例（选项 {@code 批号 生产日期~到期日期 *数量}）→ {@code P20240101 2024-01-01~2025-01-01 *2.00}。
     * 行内没有该选项所需任何批次/效期字段时返回 null（非批次商品不注入，避免污染渲染数据）。</p>
     */
    static String buildBatchEffectiveText(Map<String, Object> row, List<String> fields) {
        List<String> parts = new ArrayList<>();
        for (String field : fields) {
            if ("productionToExpiry".equals(field)) {
                // 两端都在 → `起~止`；只有一端 → 退化为该端（不打出半截波浪号）
                Object start = readField(row, ROW_ALIASES.get("productionDate"));
                Object end = readField(row, ROW_ALIASES.get("expiryDate"));
                if (start != null && end != null) {
                    parts.add(start + "~" + end);
                } else if (start != null) {
                    parts.add(String.valueOf(start));
                } else if (end != null) {
                    parts.add(String.valueOf(end));
                }
                continue;
            }
            Object value = readField(row, ROW_ALIASES.get(field));
            if (value != null) {
                parts.add(String.valueOf(value));
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        Object qty = readField(row, ROW_ALIASES.get("qty"));
        return String.join(" ", parts) + (qty != null ? " *" + qty : "");
    }

    /** 取行内字段：按别名顺序找第一个非空值；全空返回 null */
    private static Object readField(Map<String, Object> row, List<String> aliases) {
        if (aliases == null) {
            return null;
        }
        for (String key : aliases) {
            Object value = row.get(key);
            if (value != null && !"".equals(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按位数格式化一个数值；**非数值原样返回**（不制造 {@code -0}，也不把 {@code '-'}/{@code '/'} 变成数字）。
     * 结果与前端 {@code toFixed} 一致，是**字符串**。
     */
    static Object formatDecimal(Object value, int digits) {
        if (value == null || "".equals(value) || value instanceof Boolean) {
            return value;
        }
        BigDecimal decimal;
        try {
            decimal = new BigDecimal(String.valueOf(value).replace(",", "").trim());
        } catch (NumberFormatException e) {
            return value;
        }
        return decimal.setScale(digits, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean isQtyKey(String key) {
        return QTY_KEY.matcher(key).find() && !AMOUNT_KEY.matcher(key).find();
    }

    private static boolean isPriceKey(String key) {
        return PRICE_KEY.matcher(key).find() && !AMOUNT_KEY.matcher(key).find();
    }

    /** 0/1 开关；非 1 一律当关闭 */
    private static boolean isOn(Integer flag) {
        return flag != null && flag == 1;
    }

    private static Integer normalizeDigits(Integer value) {
        if (value == null || value < 0 || value > MAX_DIGITS) {
            return null;
        }
        return value;
    }
}
