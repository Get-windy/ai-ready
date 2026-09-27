package cn.aiedge.dms.task.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 配送查询（dispatch → 配送查询）打印数据装配器（pageCode = dispatch-query）。
 *
 * <p><b>它打的就是配送任务单本身</b>：该页行来自 {@code taskApi}，
 * 打印时用的是 `record.id`（任务主键），所以**不重复实现装配逻辑**，
 * 直接委托 {@link DmsTaskPrintDataProvider}。</p>
 */
@Component
@RequiredArgsConstructor
public class DispatchQueryPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="dispatch-query">} 一致 */
    public static final String PAGE_CODE = "dispatch-query";

    private final DmsTaskPrintDataProvider dmsTaskProvider;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        return dmsTaskProvider.load(documentId);
    }

    @Override
    public String documentNo(Long documentId) {
        return dmsTaskProvider.documentNo(documentId);
    }
}
