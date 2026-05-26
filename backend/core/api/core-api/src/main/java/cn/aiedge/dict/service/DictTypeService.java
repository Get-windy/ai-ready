package cn.aiedge.dict.service;

import cn.aiedge.dict.dto.DictTypeDTO;
import cn.aiedge.dict.model.DictType;
import cn.aiedge.dict.vo.DictTypeVO;

import java.util.List;
import java.util.Map;

/**
 * 字典类型服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DictTypeService {

    /**
     * 创建字典类型
     *
     * @param dictTypeDTO 字典类型DTO
     * @return 字典类型ID
     */
    Long create(DictTypeDTO dictTypeDTO);

    /**
     * 更新字典类型
     *
     * @param dictTypeDTO 字典类型DTO
     * @return 是否成功
     */
    boolean update(DictTypeDTO dictTypeDTO);

    /**
     * 删除字典类型
     *
     * @param id 字典类型ID
     * @return 是否成功
     */
    boolean delete(Long id);

    /**
     * 获取字典类型详情
     *
     * @param id 字典类型ID
     * @return 字典类型VO
     */
    DictTypeVO getById(Long id);

    /**
     * 根据编码获取字典类型
     *
     * @param dictCode 字典编码
     * @return 字典类型VO
     */
    DictTypeVO getByDictCode(String dictCode);

    /**
     * 查询字典类型列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    Map<String, Object> list(Map<String, Object> params);

    /**
     * 查询字典类型树形结构
     *
     * @param parentId 父ID
     * @return 树形列表
     */
    List<DictTypeVO> getTree(Long parentId);

    /**
     * 查询所有启用的字典类型
     *
     * @return 字典类型列表
     */
    List<DictTypeVO> getEnabled();

    /**
     * 修改字典类型状态
     *
     * @param id     字典类型ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);

    /**
     * 导出字典类型
     *
     * @param params 查询参数
     * @return 字典类型列表
     */
    List<DictType> export(Map<String, Object> params);

    /**
     * 清理字典类型缓存
     *
     * @param dictCode 字典编码
     */
    void clearCache(String dictCode);

    /**
     * 清理所有字典缓存
     */
    void clearAllCache();
}
