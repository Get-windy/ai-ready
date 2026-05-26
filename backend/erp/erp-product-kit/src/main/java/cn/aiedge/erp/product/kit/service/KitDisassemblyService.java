package cn.aiedge.erp.product.kit.service;

import cn.aiedge.erp.product.kit.entity.KitDisassembly;
import cn.aiedge.erp.product.kit.entity.KitDisassemblyItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface KitDisassemblyService extends IService<KitDisassembly> {

    KitDisassembly getByDisassemblyNo(String disassemblyNo);

    Page<KitDisassembly> pageList(String keyword, Long kitId, Long warehouseId, Integer status, int pageNum, int pageSize);

    String generateDisassemblyNo();

    KitDisassembly createDisassembly(KitDisassembly disassembly);

    KitDisassembly createFromKit(Long kitId, BigDecimal quantity, Long warehouseId, String batchNo);

    KitDisassembly updateDisassembly(Long disassemblyId, KitDisassembly disassembly);

    KitDisassembly submitForApproval(Long disassemblyId);

    KitDisassembly approve(Long disassemblyId, Long approverId, String note);

    KitDisassembly reject(Long disassemblyId, String reason);

    KitDisassembly execute(Long disassemblyId, Long executorId);

    KitDisassemblyItem executeItem(Long itemId, BigDecimal actualQuantity, String batchNo);

    KitDisassembly complete(Long disassemblyId);

    KitDisassembly cancel(Long disassemblyId, String reason);

    void calculateTotals(Long disassemblyId);

    List<KitDisassemblyItem> getItems(Long disassemblyId);

    void updateStock(Long disassemblyId);
}