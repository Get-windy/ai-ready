package cn.aiedge.erp.product.kit.service;

import cn.aiedge.erp.product.kit.entity.KitAssembly;
import cn.aiedge.erp.product.kit.entity.KitAssemblyItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface KitAssemblyService extends IService<KitAssembly> {

    KitAssembly getByAssemblyNo(String assemblyNo);

    Page<KitAssembly> pageList(String keyword, Long kitId, Long warehouseId, Integer status, int pageNum, int pageSize);

    String generateAssemblyNo();

    KitAssembly createAssembly(KitAssembly assembly);

    KitAssembly createFromKit(Long kitId, BigDecimal quantity, Long warehouseId);

    KitAssembly updateAssembly(Long assemblyId, KitAssembly assembly);

    KitAssembly submitForApproval(Long assemblyId);

    KitAssembly approve(Long assemblyId, Long approverId, String note);

    KitAssembly reject(Long assemblyId, String reason);

    KitAssembly execute(Long assemblyId, Long executorId);

    KitAssemblyItem executeItem(Long itemId, BigDecimal actualQuantity, String batchNo);

    KitAssembly complete(Long assemblyId);

    KitAssembly cancel(Long assemblyId, String reason);

    void calculateTotals(Long assemblyId);

    List<KitAssemblyItem> getItems(Long assemblyId);

    void updateStock(Long assemblyId);
}