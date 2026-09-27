package cn.aiedge.erp.budget.print;

import cn.aiedge.erp.budget.model.AnnualBudget;
import cn.aiedge.erp.budget.model.BudgetItem;
import cn.aiedge.erp.budget.repository.AnnualBudgetRepository;
import cn.aiedge.erp.budget.repository.BudgetItemRepository;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 年度预算单（预算编制单）打印数据装配器 —— **手写**，因为预算模块是 JPA，用不上通用装配器。
 *
 * <p>通用装配器 {@code GenericPrintDataProvider} 走 MyBatis-Plus 的 {@code BaseMapper}；
 * 而预算模块的 {@code AnnualBudget} / {@code BudgetItem} 是 JPA {@code @Entity} +
 * {@code JpaRepository}（表 {@code annual_budget} / {@code budget_item}），两套持久化栈不通。
 * 这正好合业界那条「取数逻辑放模型层、模板只管画」的路子：装配器直接问仓储要数据。</p>
 *
 * <p>字段名**就是模型属性名**（Jackson 转换），模板照着写；明细行的行号取 {@code sortOrder}
 * （预算明细没有 line_no 列）。</p>
 */
@Component
@RequiredArgsConstructor
public class AnnualBudgetPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="finance-budget-plan">} 一致；不能含 `/`（它是路径段） */
    public static final String PAGE_CODE = "finance-budget-plan";

    private final AnnualBudgetRepository annualBudgetRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        AnnualBudget doc = annualBudgetRepository.findById(documentId).orElse(null);
        if (doc == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>(toMap(doc));

        List<Map<String, Object>> rows = new ArrayList<>();
        int fallbackLineNo = 1;
        for (BudgetItem item : budgetItemRepository.findByBudgetIdOrderBySortOrderAsc(documentId)) {
            Map<String, Object> row = new LinkedHashMap<>(toMap(item));
            // 行号：预算明细用 sortOrder，为空时按顺序补
            row.put("lineNo", item.getSortOrder() != null ? item.getSortOrder() : fallbackLineNo);
            fallbackLineNo++;
            rows.add(row);
        }
        data.put("items", rows);
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        return annualBudgetRepository.findById(documentId).map(AnnualBudget::getBudgetNo).orElse(null);
    }

    private Map<String, Object> toMap(Object bean) {
        return objectMapper.convertValue(bean, new TypeReference<Map<String, Object>>() {
        });
    }
}
