package cn.aiedge.erp.printing.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「打印设置 → 打印内容」的选项口径（**唯一出处**）。
 *
 * <p>三个选项取自 ql361 实测，不改写、不新增：{@code 批号 *数量} / {@code 生产日期 *数量} /
 * {@code 批号 生产日期~到期日期 *数量}。选项的「器名 == 值 == 文案」（前端下拉、库里的值、
 * 这里查表用的键，三处逐字一致）。</p>
 *
 * <p>之所以抽成单独类：这三个文案原先同时写在控制器（下发下拉选项）与前端（拼接口径）里，
 * 任一处改了另一处不知道 —— 打印模块前后端漂移过一次（权限码 17 个全对不上），
 * 同一根因不再制造第二处。</p>
 */
public final class PrintContentOptions {

    /**
     * 选项文案 → 参与拼接的明细行字段。
     *
     * <p>{@code productionToExpiry} 是**一个**拼段（对应文案里的 {@code 生产日期~到期日期}），
     * 不是一个字段名 —— 由 {@link PrintBehaviorApplier} 特殊处理。
     * 插入顺序即前端下拉顺序，字段顺序即拼接顺序。</p>
     */
    public static final Map<String, List<String>> FIELDS;

    /** 下发给前端的下拉选项（value == label == 文案） */
    public static final List<Map<String, String>> OPTIONS;

    /** 默认值 = 第一项（ql361 rawValue=1 的实拍选中值） */
    public static final String DEFAULT;

    static {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        fields.put("批号 *数量", List.of("batchNo"));
        fields.put("生产日期 *数量", List.of("productionDate"));
        fields.put("批号 生产日期~到期日期 *数量", List.of("batchNo", "productionToExpiry"));
        FIELDS = Collections.unmodifiableMap(fields);

        List<Map<String, String>> options = new ArrayList<>(FIELDS.size());
        for (String text : FIELDS.keySet()) {
            options.add(Map.of("value", text, "label", text));
        }
        OPTIONS = Collections.unmodifiableList(options);
        DEFAULT = FIELDS.keySet().iterator().next();
    }

    private PrintContentOptions() {
    }
}
