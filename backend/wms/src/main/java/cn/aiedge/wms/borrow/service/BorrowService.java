package cn.aiedge.wms.borrow.service;

import cn.aiedge.wms.borrow.dto.BorrowReturnRequest;
import cn.aiedge.wms.borrow.dto.WmsBorrowOrderVO;
import cn.aiedge.wms.entity.WmsBorrowOrder;
import cn.aiedge.wms.entity.WmsBorrowReturn;
import cn.aiedge.wms.entity.WmsBorrowReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface BorrowService {

    /** 单据状态：草稿 */
    int STATUS_DRAFT = 0;
    /** 单据状态：待审批 */
    int STATUS_PENDING_APPROVAL = 1;
    /** 单据状态：已审批 */
    int STATUS_APPROVED = 2;
    /** 单据状态：部分归还 */
    int STATUS_PARTIAL_RETURNED = 3;
    /** 单据状态：已归还 */
    int STATUS_RETURNED = 4;
    /** 单据状态：已取消 */
    int STATUS_CANCELLED = 5;

    /** 方向：借进 */
    int DIRECTION_IN = 1;
    /** 方向：借出 */
    int DIRECTION_OUT = 2;

    Page<WmsBorrowOrder> pageOrder(Page<WmsBorrowOrder> page, WmsBorrowOrder query);

    /** 查询单据（含明细） */
    WmsBorrowOrderVO getOrderDetail(Long id);

    /** 新建单据（含明细），自动生成单号 */
    WmsBorrowOrder createOrder(WmsBorrowOrderVO order);

    /** 更新单据（仅草稿，明细整体替换） */
    void updateOrder(WmsBorrowOrderVO order);

    /** 删除单据（仅草稿/已取消） */
    void removeOrder(Long id);

    /** 提交审批：草稿 -> 待审批 */
    void submit(Long id);

    /** 审批通过：待审批 -> 已审批；借进库存增加、借出库存扣减 */
    void approve(Long id, Long operatorId, String operatorName);

    /** 取消：草稿/待审批 -> 已取消 */
    void cancel(Long id);

    /** 归还（支持部分归还，累计 returned_quantity），库存反向回冲 */
    WmsBorrowReturn returnOrder(BorrowReturnRequest request);

    /** 归还记录分页 */
    Page<WmsBorrowReturn> pageReturn(Page<WmsBorrowReturn> page, Long orderId);

    /** 归还记录明细 */
    List<WmsBorrowReturnItem> listReturnItems(Long returnId);

    /** 生成单号：JJ（借进）/JC（借出）+ yyyyMMdd + 3位流水 */
    String generateOrderNo(Integer direction);

    /** 借进借出商品台账聚合查询（按 商品×往来单位 分组） */
    java.util.List<java.util.Map<String, Object>> aggregateByProduct(Integer direction, String partnerName,
                                                                     String productName, String dateStart, String dateEnd);
}
