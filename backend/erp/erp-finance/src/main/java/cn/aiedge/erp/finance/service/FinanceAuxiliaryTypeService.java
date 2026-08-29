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
     * 删除辅助核算类型
     */
    void delete(Long id);
}
