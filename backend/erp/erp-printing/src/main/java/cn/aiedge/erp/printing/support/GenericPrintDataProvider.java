package cn.aiedge.erp.printing.support;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用单据打印装配器：把「主表实体 + 明细实体」直接摊成标准打印数据包。
 *
 * <p>它存在的理由：绝大多数的单据装配器只是「selectById 主表 + selectList 明细」，
 * 而字段名本来就是实体属性名（这正是本 SPI 的契约）。为每个单据手写一份，
 * 除了抄一遍字段清单没有别的价值 —— 字段一改还得跟着改两处。这里把那段重复收成一个类：
 * 业务模块只需要在配置里登记一行（pageCode + 两个 mapper + 外键列）。</p>
 *
 * <p>产出：
 * <ul>
 *   <li>主表字段：实体属性名 → 值（Jackson 转换，与接口返回给前端的形态一致）；</li>
 *   <li>{@code items}：明细行列表，同样按属性名；行号为 null 时按顺序补 1..n；</li>
 *   <li>不含任何跨行汇总 —— 合计交给模板的 {@code agg: "sum"} 在渲染时算，
 *       免得「实体存的值」和「明细求和」两个口径在这里打架。</li>
 * </ul>
 *
 * <p>⚠️ 模板不要引用 {@code xxxId} 这类只有 ID 的字段：装配器不做名称回填
 * （实体上已有 xxxName 快照的直接用；确实没有的，那个字段就不该上纸）。
 * 需要额外查表补名称、或要做字段翻译的单据，仍然用手写装配器（见 SaleOrderPrintDataProvider）。</p>
 */
public class GenericPrintDataProvider implements PrintDataProvider {

    private final String pageCode;
    private final BaseMapper<?> docMapper;
    private final BaseMapper<?> itemMapper;
    /** 明细表里指向主表的**列名**（snake_case，如 stock_in_id）；没有明细的单据传 null */
    private final String itemFkColumn;
    private final ObjectMapper objectMapper;

    public GenericPrintDataProvider(String pageCode,
                                    BaseMapper<?> docMapper,
                                    BaseMapper<?> itemMapper, String itemFkColumn,
                                    ObjectMapper objectMapper) {
        this.pageCode = pageCode;
        this.docMapper = docMapper;
        this.itemMapper = itemMapper;
        this.itemFkColumn = itemFkColumn;
        this.objectMapper = objectMapper;
    }

    @Override
    public String pageCode() {
        return pageCode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> load(Long documentId) {
        Object doc = docMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>(toMap(doc));

        List<Map<String, Object>> rows = new ArrayList<>();
        if (itemMapper != null && itemFkColumn != null && !itemFkColumn.isBlank()) {
            QueryWrapper<Object> query = new QueryWrapper<>();
            query.eq(itemFkColumn, documentId);
            query.orderByAsc("line_no");
            List<Object> items = ((BaseMapper<Object>) itemMapper).selectList(query);
            int fallbackLineNo = 1;
            for (Object item : items) {
                Map<String, Object> row = new LinkedHashMap<>(toMap(item));
                // 库里 line_no 可能是空的：打印时按顺序补，免得整列空白
                if (row.containsKey("lineNo") && row.get("lineNo") == null) {
                    row.put("lineNo", fallbackLineNo);
                }
                fallbackLineNo++;
                rows.add(row);
            }
        }
        data.put("items", rows);
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        Object doc = docMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        Map<String, Object> map = toMap(doc);
        for (String key : new String[]{"orderNo", "docNo", "billNo", "no"}) {
            Object v = map.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v);
            }
        }
        // 退一步：找第一个以 No 结尾的单号型字段
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey().endsWith("No") && e.getValue() != null && !String.valueOf(e.getValue()).isBlank()) {
                return String.valueOf(e.getValue());
            }
        }
        return null;
    }

    private Map<String, Object> toMap(Object bean) {
        return objectMapper.convertValue(bean, new TypeReference<Map<String, Object>>() {
        });
    }
}
