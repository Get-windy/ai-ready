package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.FinanceAuxiliaryItemDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 辅助核算项目Service接口
 */
public interface FinanceAuxiliaryItemService {

    /**
     * 分页查询辅助核算项目
     */
    IPage<FinanceAuxiliaryItemDTO> page(Long auxiliaryTypeId, String itemCode, String itemName, Boolean enabled, Page<FinanceAuxiliaryItemDTO> page);

    /**
     * 查询指定类型下的辅助核算项目列表
     */
    List<FinanceAuxiliaryItemDTO> listByTypeId(Long auxiliaryTypeId, Boolean enabled);

    /**
     * 根据ID查询辅助核算项目
     */
    FinanceAuxiliaryItemDTO getById(Long id);

    /**
     * 创建辅助核算项目
     */
    FinanceAuxiliaryItemDTO create(FinanceAuxiliaryItemDTO dto);

    /**
     * 更新辅助核算项目
     */
    FinanceAuxiliaryItemDTO update(Long id, FinanceAuxiliaryItemDTO dto);

    /**
     * 删除辅助核算项目
     */
    void delete(Long id);
}
