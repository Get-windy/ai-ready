package cn.aiedge.erp.batch.service;

import cn.aiedge.erp.batch.model.SerialNumber;
import cn.aiedge.erp.batch.model.dto.CommonDTO;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 序列号服务接口
 */
public interface SerialNumberService {
    
    /**
     * 创建序列号
     */
    SerialNumber createSerialNumber(SerialNumber serialNumber);
    
    /**
     * 批量创建序列号
     */
    List<SerialNumber> createSerialNumbers(List<SerialNumber> serialNumbers);
    
    /**
     * 更新序列号
     */
    SerialNumber updateSerialNumber(Long id, SerialNumber serialNumber);
    
    /**
     * 获取序列号
     */
    SerialNumber getSerialNumber(Long id);
    
    /**
     * 根据序列号获取
     */
    SerialNumber getSerialNumberBySerialNumber(String serialNumber);
    
    /**
     * 删除序列号
     */
    void deleteSerialNumber(Long id);
    
    /**
     * 查询序列号列表
     */
    List<SerialNumber> listSerialNumbers();
    
    /**
     * 分页查询序列号
     */
    Page<SerialNumber> pageSerialNumbers(CommonDTO.PageRequest pageRequest);
    
    /**
     * 查询批次序列号
     */
    List<SerialNumber> listSerialNumbersByBatch(Long batchId);
    
    /**
     * 查询产品序列号
     */
    List<SerialNumber> listSerialNumbersByProduct(Long productId);
    
    /**
     * 查询序列号状态
     */
    List<SerialNumber> listSerialNumbersByStatus(SerialNumber.SerialNumberStatus status);
    
    /**
     * 查询序列号类型
     */
    List<SerialNumber> listSerialNumbersByType(SerialNumber.SerialNumberType serialType);
    
    /**
     * 查询订单序列号
     */
    List<SerialNumber> listSerialNumbersByOrder(Long orderId);
    
    /**
     * 查询客户序列号
     */
    List<SerialNumber> listSerialNumbersByCustomer(Long customerId);
    
    /**
     * 更新序列号状态
     */
    SerialNumber updateSerialNumberStatus(Long id, SerialNumber.SerialNumberStatus status);
    
    /**
     * 激活序列号
     */
    SerialNumber activateSerialNumber(Long id, Long customerId);
    
    /**
     * 停用序列号
     */
    SerialNumber deactivateSerialNumber(Long id, String reason);
    
    /**
     * 序列号扫描
     */
    SerialNumber scanSerialNumber(String serialNumber, String location);
    
    /**
     * 序列号追溯
     */
    SerialNumber traceSerialNumber(String serialNumber);
    
    /**
     * 序列号转移（改变所属客户）
     */
    SerialNumber transferSerialNumber(Long id, Long newCustomerId, String remark);
    
    /**
     * 序列号报废
     */
    SerialNumber scrapSerialNumber(Long id, String reason);
    
    /**
     * 检查序列号是否存在
     */
    boolean isSerialNumberExists(String serialNumber);
    
    /**
     * 生成序列号
     */
    String generateSerialNumber(Long productId, Long batchId);
    
    /**
     * 批量生成序列号
     */
    List<String> generateSerialNumbers(Long productId, Long batchId, int count);
    
    /**
     * 查询序列号历史记录
     */
    List<Object> getSerialNumberHistory(String serialNumber);
}