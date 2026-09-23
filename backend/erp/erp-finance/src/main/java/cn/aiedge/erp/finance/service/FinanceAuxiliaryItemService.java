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
     * 启用/禁用辅助核算项目
     *
     * <p>列表页的「启用」开关专用：只改 enabled 一个字段，
     * 不走 {@link #update} 的全字段校验（否则列表页只传 enabled 会因缺少
     * itemCode/auxiliaryTypeId 被 @Valid 拦下）。
     */
    FinanceAuxiliaryItemDTO enable(Long id, Boolean enabled);

    /**
     * 删除辅助核算项目
     */
    void delete(Long id);
}
