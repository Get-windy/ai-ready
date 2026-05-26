package cn.aiedge.position.service;

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
    void update(CategoryVO request);

    /**
     * 删除分类
     */
    void delete(Long id);

    /**
     * 获取子分类
     */
    List<CategoryVO> getChildren(Long parentId);
}
