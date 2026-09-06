package cn.aiedge.wms.receipt.service;

import cn.aiedge.wms.controller.dto.WmsReceiptDetailVO;
import cn.aiedge.wms.entity.WmsReceiptTask;
import cn.aiedge.wms.entity.WmsReceiptDetail;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.util.List;

public interface ReceiptService {
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
    // 明细整体保存（先删后插，仅待处理状态可操作）
    void saveDetails(Long taskId, List<WmsReceiptDetail> details);
    // 按明细分页查询（对齐报损单 page-detail 成熟度）
    Page<WmsReceiptDetailVO> pageDetail(Page<WmsReceiptDetailVO> page, String keyword, String sourceOrderNo,
                                        Integer sourceType, Integer status, Long warehouseId,
                                        String warehouseName, String productName, String batchNo,
                                        LocalDate dateStart, LocalDate dateEnd);
    // 生成下一收货单号（RC + yyyyMMdd + 随机6位）
    String generateNo();
    // 操作
    void startReceipt(Long taskId, Long userId, String userName);
    void confirmReceipt(Long taskId, Long userId, String userName);
    void cancelReceipt(Long taskId, String reason);
}
