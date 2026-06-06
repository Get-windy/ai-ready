package cn.aiedge.position.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.position.dto.CategoryQueryRequest;
import cn.aiedge.position.dto.CategoryVO;
import cn.aiedge.position.entity.PositionCategory;
import cn.aiedge.position.mapper.PositionCategoryMapper;
import cn.aiedge.position.mapper.PositionMapper;
import cn.aiedge.position.service.PositionCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位分类服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionCategoryServiceImpl extends ServiceImpl<PositionCategoryMapper, PositionCategory> 
        implements PositionCategoryService {

    private final PositionMapper positionMapper;

    @Override
    public PageResult<CategoryVO> pageList(CategoryQueryRequest request) {
        LambdaQueryWrapper<PositionCategory> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getCategoryName())) {
            wrapper.like(PositionCategory::getCategoryName, request.getCategoryName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(PositionCategory::getStatus, request.getStatus());
        }

        wrapper.orderByAsc(PositionCategory::getSort);

        Page<PositionCategory> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<PositionCategory> result = this.page(page, wrapper);

        List<CategoryVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<CategoryVO> listAll() {
        LambdaQueryWrapper<PositionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PositionCategory::getStatus, 1).orderByAsc(PositionCategory::getSort);

        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getTree() {
        LambdaQueryWrapper<PositionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PositionCategory::getSort);
        
        List<PositionCategory> all = this.list(wrapper);
        
        // 构建树结构
        Map<Long, List<PositionCategory>> groupByParent = all.stream()
            .collect(Collectors.groupingBy(PositionCategory::getParentId));
        
        return buildTree(0L, groupByParent);
    }

    @Override
    public CategoryVO getDetail(Long id) {
        PositionCategory category = this.getById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }
        return convertToVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategoryVO request) {
        // 检查编码唯一性
        LambdaQueryWrapper<PositionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PositionCategory::getCategoryCode, request.getCategoryCode());
        if (this.count(wrapper) > 0) {
            throw BusinessException.badRequest("分类编码已存在");
        }
        
        PositionCategory category = new PositionCategory();
        BeanUtils.copyProperties(request, category);
        category.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        
        // 设置祖级列表
        if (category.getParentId() > 0) {
            PositionCategory parent = this.getById(category.getParentId());
            if (parent == null) {
                throw BusinessException.badRequest("父分类不存在");
            }
            category.setAncestors(parent.getAncestors() + "," + parent.getId());
        } else {
            category.setAncestors("0");
        }
        
        this.save(category);
        log.info("创建岗位分类成功: id={}, name={}", category.getId(), category.getCategoryName());
        
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryVO request) {
        PositionCategory category = this.getById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }

        // 检查编码唯一性（排除自身）
        if (StringUtils.hasText(request.getCategoryCode())) {
            LambdaQueryWrapper<PositionCategory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PositionCategory::getCategoryCode, request.getCategoryCode())
                   .ne(PositionCategory::getId, id);
            if (this.count(wrapper) > 0) {
                throw BusinessException.badRequest("分类编码已存在");
            }
        }

        BeanUtils.copyProperties(request, category);
        this.updateById(category);
        log.info("更新岗位分类成功: id={}", category.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        PositionCategory category = this.getById(id);
        if (category == null) {
            throw BusinessException.notFound("分类不存在");
        }

        category.setStatus(status);
        this.updateById(category);
        log.info("更新岗位分类状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否有子分类
        LambdaQueryWrapper<PositionCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(PositionCategory::getParentId, id);
        if (this.count(categoryWrapper) > 0) {
            throw BusinessException.badRequest("存在子分类，无法删除");
        }
        
        // 检查是否有岗位关联
        LambdaQueryWrapper<cn.aiedge.position.entity.Position> positionWrapper = new LambdaQueryWrapper<>();
        positionWrapper.eq(cn.aiedge.position.entity.Position::getCategoryId, id);
        if (positionMapper.selectCount(positionWrapper) > 0) {
            throw BusinessException.badRequest("分类下存在岗位，无法删除");
        }
        
        this.removeById(id);
        log.info("删除岗位分类成功: id={}", id);
    }

    @Override
    public List<CategoryVO> getChildren(Long parentId) {
        LambdaQueryWrapper<PositionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PositionCategory::getParentId, parentId)
               .orderByAsc(PositionCategory::getSort);
        
        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /**
     * 构建树结构
     */
    private List<CategoryVO> buildTree(Long parentId, Map<Long, List<PositionCategory>> groupByParent) {
        List<CategoryVO> result = new ArrayList<>();
        
        List<PositionCategory> children = groupByParent.get(parentId);
        if (children == null) {
            return result;
        }
        
        for (PositionCategory category : children) {
            CategoryVO vo = convertToVO(category);
            vo.setChildren(buildTree(category.getId(), groupByParent));
            result.add(vo);
        }
        
        return result;
    }

    /**
     * 转换为VO
     */
    private CategoryVO convertToVO(PositionCategory category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
