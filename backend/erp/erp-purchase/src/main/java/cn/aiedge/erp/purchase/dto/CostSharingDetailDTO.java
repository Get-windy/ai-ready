package cn.aiedge.erp.purchase.dto;

import cn.aiedge.erp.purchase.entity.CostSharing;
import cn.aiedge.erp.purchase.entity.CostSharingExpenseItem;
import cn.aiedge.erp.purchase.entity.CostSharingItem;
import lombok.Data;

import java.util.List;

/**
 * 采购费用分摊详情DTO
 *
 * 返回主表 + 费用单明细 + 采购入库单分摊明细，供表单页编辑回填。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class CostSharingDetailDTO {

    /** 主表信息 */
    private CostSharing sharing;

    /** 费用单明细 */
    private List<CostSharingExpenseItem> expenseItems;

    /** 采购入库单分摊明细 */
    private List<CostSharingItem> items;
}
