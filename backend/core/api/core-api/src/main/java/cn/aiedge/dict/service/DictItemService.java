package cn.aiedge.dict.service;

import cn.aiedge.dict.dto.DictItemDTO;
import cn.aiedge.dict.model.DictItem;
import cn.aiedge.dict.vo.DictItemVO;

import java.util.List;
import java.util.Map;

/**
 * 字典项服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DictItemService {

    /**
     * 创建字典项
     *
     * @param dictItemDTO 字典项DTO
     * @return 字典项ID
     */
    Long create(DictItemDTO dictItemDTO);

    /**
     * 批量创建字典项
     *
     * @param dictItemDTOs 字典项DTO列表
     * @return 创建数量
     */
    int batchCreate(List<DictItemDTO> dictItemDTOs);

    /**
     * 更新字典项
     *
     * @param dictItemDTO 字典项DTO
     * @return 是否成功
     */
    boolean update(DictItemDTO dictItemDTO);

    /**
     * 删除字典项
     *
     * @param id 字典项ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 获取字典项详情
     *
     * @param id 字典项ID
     * @return 字典项VO
     */
    DictItemVO getById(Long id);

    /**
     * 根据字典类型查询字典项
     *
     * @param dictTypeId 字典类型ID
     * @return 字典项列表
     */
    List<DictItemVO> getByDictTypeId(Long dictTypeId);

    /**
     * 根据字典类型编码查询字典项
     *
     * @param dictCode 字典类型编码
     * @return 字典项列表
     */
    List<DictItemVO> getByDictCode(String dictCode);

    /**
     * 查询字典项树形结构
     *
     * @param dictTypeId 字典类型ID
     * @param parentId   父ID
     * @return 树形列表
     */
    List<DictItemVO> getTree(Long dictTypeId, Long parentId);

    /**
     * 查询字典项列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    Map<String, Object> list(Map<String, Object> params);

    /**
     * 修改字典项状态
     *
     * @param id     字典项ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);

    /**
     * 根据字典类型和项值获取字典项
     *
     * @param dictCode  字典类型编码
     * @param itemValue 字典项值
     * @return 字典项VO
     */
    DictItemVO getByDictCodeAndValue(String dictCode, String itemValue);

    /**
     * 根据字典类型ID统计字典项数量
     *
     * @param dictTypeId 字典类型ID
     * @return 字典项数量
     */
    int countByDictType(Long dictTypeId);

    /**
     * 批量删除字典项
     *
     * @param ids 字典项ID列表
     * @return 是否成功
     */
    boolean removeBatchByIds(List<Long> ids);

    /**
     * 导出字典项
     *
     * @param params 查询参数
     * @return 字典项列表
     */
    List<DictItem> export(Map<String, Object> params);

    /**
     * 同步刷新字典缓存
     *
     * @param dictCode 字典类型编码
     */
    void refreshCache(String dictCode);
}
