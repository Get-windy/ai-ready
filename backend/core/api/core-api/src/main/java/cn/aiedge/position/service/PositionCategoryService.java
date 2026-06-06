package cn.aiedge.position.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.position.dto.CategoryQueryRequest;
import cn.aiedge.position.dto.CategoryVO;
import cn.aiedge.position.entity.PositionCategory;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 岗位分类服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PositionCategoryService extends IService<PositionCategory> {

    /**
     * 分页查询分类
     */
    PageResult<CategoryVO> pageList(CategoryQueryRequest request);

    /**
     * 获取所有分类列表
     */
    List<CategoryVO> listAll();

    /**
     * 获取分类树
     */
    List<CategoryVO> getTree();

    /**
     * 获取分类详情
     */
    CategoryVO getDetail(Long id);

    /**
     * 创建分类
     */
    Long create(CategoryVO request);

    /**
     * 更新分类
     */
    void update(Long id, CategoryVO request);

    /**
     * 删除分类
     */
    void delete(Long id);

    /**
     * 更新分类状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取子分类
     */
    List<CategoryVO> getChildren(Long parentId);
}
