package cn.aiedge.wms.receipt.service;

import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ReceiptService {
    // 从采购订单创建收货任务
    WmsReceiptTask createFromPurchaseOrder(Long purchaseOrderId);
    boolean saveTask(WmsReceiptTask task);
    boolean updateTask(WmsReceiptTask task);
    WmsReceiptTask getTaskById(Long id);
    WmsReceiptTask getByTaskNo(String taskNo);
    Page<WmsReceiptTask> pageTask(Page<WmsReceiptTask> page, WmsReceiptTask query);
    boolean removeTask(Long id);
    // 明细
    boolean saveDetail(WmsReceiptDetail detail);
    boolean updateDetail(WmsReceiptDetail detail);
    WmsReceiptDetail getDetailById(Long id);
    List<WmsReceiptDetail> listByTaskId(Long taskId);
    // 操作
    void startReceipt(Long taskId, Long userId, String userName);
    void confirmReceipt(Long taskId, Long userId, String userName);
    void cancelReceipt(Long taskId, String reason);
}
