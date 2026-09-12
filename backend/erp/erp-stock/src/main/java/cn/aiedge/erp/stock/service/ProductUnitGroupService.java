package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.ProductUnitGroupDTO;
import cn.aiedge.erp.stock.dto.ProductUnitGroupVO;
import cn.aiedge.erp.stock.entity.ProductUnitGroup;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商品单位组Service接口
 */
public interface ProductUnitGroupService extends IService<ProductUnitGroup> {

    /** 分页查询单位组（含组内单位明细） */
    IPage<ProductUnitGroupVO> getPage(Long tenantId, String keyword, Integer status, int pageNum, int pageSize);

    /** 新增单位组（含成员），返回新ID */
    Long createGroup(Long tenantId, ProductUnitGroupDTO dto);

    /** 修改单位组（含成员全量覆盖） */
    void updateGroup(Long tenantId, Long id, ProductUnitGroupDTO dto);

    /** 删除单位组（同时逻辑删除成员） */
    void deleteGroup(Long tenantId, Long id);

    /** 启用/停用单位组（对标行内「停用 / 启用」） */
    void updateStatus(Long tenantId, Long id, Integer status);

    /** 单位组详情（含成员） */
    ProductUnitGroupVO getDetail(Long tenantId, Long id);
}
