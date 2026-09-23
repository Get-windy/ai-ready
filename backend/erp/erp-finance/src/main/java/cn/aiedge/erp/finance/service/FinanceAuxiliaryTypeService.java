package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 辅助核算类型Service接口
 */
public interface FinanceAuxiliaryTypeService {

    /**
     * 分页查询辅助核算类型
     */
    IPage<FinanceAuxiliaryTypeDTO> page(String typeCode, String typeName, Boolean enabled, Page<FinanceAuxiliaryTypeDTO> page);

    /**
     * 查询辅助核算类型列表
     */
    List<FinanceAuxiliaryTypeDTO> list(Boolean enabled);

    /**
     * 根据ID查询辅助核算类型
     */
    FinanceAuxiliaryTypeDTO getById(Long id);

    /**
     * 创建辅助核算类型
     */
    FinanceAuxiliaryTypeDTO create(FinanceAuxiliaryTypeDTO dto);

    /**
     * 更新辅助核算类型
     */
    FinanceAuxiliaryTypeDTO update(Long id, FinanceAuxiliaryTypeDTO dto);

    /**
     * 启用/禁用辅助核算类型
     *
     * <p>列表页的「启用」开关专用：只改 enabled 一个字段，
     * 不走 {@link #update} 的全字段校验（否则列表页只传 enabled 会因缺少
     * typeCode 被 @Valid 拦下，前端表现为开关点了没反应）。
     */
    FinanceAuxiliaryTypeDTO enable(Long id, Boolean enabled);

    /**
     * 删除辅助核算类型
     */
    void delete(Long id);
}
