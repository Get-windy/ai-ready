package cn.aiedge.position.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.position.dto.*;
import cn.aiedge.position.entity.Position;
import cn.aiedge.position.entity.PositionCategory;
import cn.aiedge.position.entity.UserPosition;
import cn.aiedge.position.mapper.PositionMapper;
import cn.aiedge.position.mapper.PositionCategoryMapper;
import cn.aiedge.position.mapper.UserPositionMapper;
import cn.aiedge.position.service.PositionService;
import cn.aiedge.common.result.PageResult;
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
import java.util.stream.Collectors;

/**
 * 岗位服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    private final PositionCategoryMapper categoryMapper;
    private final UserPositionMapper userPositionMapper;

    @Override
    public PageResult<PositionVO> pageList(PositionQueryRequest request) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(request.getPositionCode())) {
            wrapper.like(Position::getPositionCode, request.getPositionCode());
        }
        if (StringUtils.hasText(request.getPositionName())) {
            wrapper.like(Position::getPositionName, request.getPositionName());
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(Position::getCategoryId, request.getCategoryId());
        }
        if (request.getDeptId() != null) {
            wrapper.eq(Position::getDeptId, request.getDeptId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Position::getStatus, request.getStatus());
        }
        
        wrapper.orderByAsc(Position::getSort);
        
        Page<Position> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Position> result = this.page(page, wrapper);
        
        List<PositionVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<PositionVO> listAll() {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getStatus, 1).orderByAsc(Position::getSort);
        
        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    public PositionVO getDetail(Long id) {
        Position position = this.getById(id);
        if (position == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        return convertToVO(position);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PositionCreateRequest request) {
        // 检查编码唯一性
        if (this.getByCode(request.getPositionCode()) != null) {
            throw BusinessException.badRequest("岗位编码已存在");
        }
        
        Position position = new Position();
        BeanUtils.copyProperties(request, position);
        position.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        position.setSort(request.getSort() != null ? request.getSort() : 0);
        
        this.save(position);
        log.info("创建岗位成功: id={}, name={}", position.getId(), position.getPositionName());
        
        return position.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PositionUpdateRequest request) {
        Position position = this.getById(request.getId());
        if (position == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        
        // 检查编码唯一性（排除自身）
        Position existing = this.getByCode(request.getPositionCode());
        if (existing != null && !existing.getId().equals(request.getId())) {
            throw BusinessException.badRequest("岗位编码已存在");
        }
        
        BeanUtils.copyProperties(request, position);
        this.updateById(position);
        log.info("更新岗位成功: id={}", position.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否有用户关联
        int userCount = userPositionMapper.countUsersByPositionId(id);
        if (userCount > 0) {
            throw BusinessException.badRequest("该岗位下存在关联用户，无法删除");
        }
        
        this.removeById(id);
        log.info("删除岗位成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            this.delete(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Position position = this.getById(id);
        if (position == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        
        position.setStatus(status);
        this.updateById(position);
        log.info("更新岗位状态: id={}, status={}", id, status);
    }

    @Override
    public List<PositionVO> getByDeptId(Long deptId) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getDeptId, deptId)
               .eq(Position::getStatus, 1)
               .orderByAsc(Position::getSort);
        
        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<PositionVO> getByUserId(Long userId) {
        List<Long> positionIds = userPositionMapper.selectPositionIdsByUserId(userId);
        if (positionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return this.listByIds(positionIds).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignToUser(Long userId, List<Long> positionIds, Long primaryPositionId) {
        // 先删除旧的关联
        LambdaQueryWrapper<UserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPosition::getUserId, userId);
        userPositionMapper.delete(wrapper);
        
        // 创建新的关联
        for (Long positionId : positionIds) {
            UserPosition up = new UserPosition();
            up.setUserId(userId);
            up.setPositionId(positionId);
            up.setIsPrimary(positionId.equals(primaryPositionId) ? 1 : 0);
            userPositionMapper.insert(up);
        }
        
        log.info("分配岗位给用户: userId={}, positions={}", userId, positionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromUser(Long userId, List<Long> positionIds) {
        LambdaQueryWrapper<UserPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPosition::getUserId, userId)
               .in(UserPosition::getPositionId, positionIds);
        userPositionMapper.delete(wrapper);
        
        log.info("移除用户岗位: userId={}, positions={}", userId, positionIds);
    }

    @Override
    public List<Long> getUserIds(Long positionId) {
        return userPositionMapper.selectUserIdsByPositionId(positionId);
    }

    /**
     * 根据编码获取岗位
     */
    private Position getByCode(String positionCode) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getPositionCode, positionCode);
        return this.getOne(wrapper);
    }

    /**
     * 转换为VO
     */
    private PositionVO convertToVO(Position position) {
        PositionVO vo = new PositionVO();
        BeanUtils.copyProperties(position, vo);
        
        // 设置分类名称
        if (position.getCategoryId() != null) {
            PositionCategory category = categoryMapper.selectById(position.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
        }
        
        // 设置用户数
        vo.setUserCount(userPositionMapper.countUsersByPositionId(position.getId()));
        
        return vo;
    }
}
