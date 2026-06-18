package cn.aiedge.erp.batchsn.service;

import cn.aiedge.erp.batchsn.entity.SerialNumber;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 序列号服务接口
 * 
 * @author team-member
 * @date 2026-04-27
 */
public interface SerialNumberService extends IService<SerialNumber> {
    
    /**
     * 创建序列号
     */
    SerialNumber createSerial(SerialNumber serial);
    
    /**
     * 根据ID查询序列号详情
     */
    SerialNumber getSerialById(Long id);
    
    /**
     * 根据序列号查询
     */
    SerialNumber getSerialByNo(String serialNo);
    
    /**
     * 查询序列号列表（支持多条件筛选）
     */
    List<SerialNumber> listSerials(
        String serialNo,
        String productCode,
        String status,
        String batchNo,
        int page,
        int size
    );
    
    /**
     * 更新序列号信息
     */
    SerialNumber updateSerial(Long id, SerialNumber serial);
    
    /**
     * 序列号入库操作
     */
    SerialNumber inbound(SerialNumber serial, Long warehouseId, String warehouseName, Long locationId);
    
    /**
     * 序列号出库操作
     */
    SerialNumber outbound(Long serialId, Long saleOrderId, String saleOrderNo, Long warehouseId, Long locationId);
    
    /**
     * 更新序列号状态
     */
    SerialNumber updateStatus(Long id, String status, String stage);
    
    /**
     * 序列号维修记录
     */
    SerialNumber addMaintenanceRecord(Long serialId, String remark, String operatorId);
    
    /**
     * 查询质保即将到期的序列号
     */
    List<SerialNumber> getWarrantyExpiring(int warningDays);
    
    /**
     * 查询质保已到期的序列号
     */
    List<SerialNumber> getWarrantyExpired();
    
    /**
     * 查询序列号完整流转历史
     */
    List<SerialNumber> getFullHistory(Long serialId);
    
    /**
     * 查询序列号维修记录
     */
    List<SerialNumber> getMaintenanceHistory(Long serialId);
    
    /**
     * 验证序列号是否存在
     */
    boolean serialNoExists(String serialNo);
    
    /**
     * 检查序列号质保是否即将到期
     */
    boolean isWarrantyExpiring(SerialNumber serial, int warningDays);
    
    /**
     * 批量更新序列号状态
     */
    int updateSerialStatus(List<Long> serialIds, String status, String stage, String updaterId);
    
    /**
     * 序列号报废
     */
    SerialNumber scrapSerial(Long serialId, String remark, String operatorId);
    
    /**
     * 查询序列号当前位置
     */
    String getCurrentLocation(Long serialId);
    
    /**
     * 查询序列号当前仓库
     */
    Long getCurrentWarehouseId(Long serialId);
}
