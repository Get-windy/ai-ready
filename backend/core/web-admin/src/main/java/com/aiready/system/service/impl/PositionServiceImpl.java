package com.aiready.system.service.impl;

import com.aiready.system.entity.Department;
import com.aiready.system.entity.Position;
import com.aiready.system.mapper.PositionMapper;
import com.aiready.system.service.DepartmentService;
import com.aiready.system.service.PositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位服务实现类
 */
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    @Autowired
    private DepartmentService departmentService;

    @Override
    public Position getByPositionCode(String positionCode) {
        return this.getOne(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getPositionCode, positionCode)
                .eq(Position::getDeleted, 0)
        );
    }

    @Override
    public List<Position> listByDeptId(Long deptId) {
        return this.list(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getDeptId, deptId)
                .eq(Position::getStatus, 1)
                .eq(Position::getDeleted, 0)
                .orderByAsc(Position::getSortOrder)
        );
    }

    @Override
    public List<Position> listByType(Integer positionType) {
        return this.list(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getPositionType, positionType)
                .eq(Position::getStatus, 1)
                .eq(Position::getDeleted, 0)
                .orderByAsc(Position::getSortOrder)
        );
    }

    @Override
    public List<Position> listActivePositions() {
        return this.list(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getStatus, 1)
                .eq(Position::getDeleted, 0)
                .orderByAsc(Position::getSortOrder)
        );
    }

    @Override
    public boolean checkPositionCodeExists(String positionCode) {
        return this.count(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getPositionCode, positionCode)
                .eq(Position::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean checkPositionCodeExists(String positionCode, Long excludeId) {
        return this.count(
            new LambdaQueryWrapper<Position>()
                .eq(Position::getPositionCode, positionCode)
                .ne(Position::getId, excludeId)
                .eq(Position::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean hasEmployees(Long positionId) {
        // TODO: 需要集成员工模块后实现
        return false;
    }

    @Override
    @Transactional
    public boolean enablePosition(Long id) {
        Position position = new Position();
        position.setId(id);
        position.setStatus(1);
        return this.updateById(position);
    }

    @Override
    @Transactional
    public boolean disablePosition(Long id) {
        Position position = new Position();
        position.setId(id);
        position.setStatus(0);
        return this.updateById(position);
    }

    @Override
    @Transactional
    public boolean updateCurrentCount(Long positionId, Integer delta) {
        Position position = this.getById(positionId);
        if (position == null) {
            return false;
        }
        int newCount = (position.getCurrentCount() != null ? position.getCurrentCount() : 0) + delta;
        if (newCount < 0) {
            newCount = 0;
        }
        position.setCurrentCount(newCount);
        return this.updateById(position);
    }

    @Override
    public Position getPositionById(Long id) {
        Position position = this.getById(id);
        if (position != null && position.getDeptId() != null) {
            Department dept = departmentService.getById(position.getDeptId());
            if (dept != null) {
                position.setDeptName(dept.getDeptName());
            }
        }
        return position;
    }

    @Override
    public List<Position> getPositionsByDeptTree(Long deptId) {
        List<Position> result = new ArrayList<>();
        
        // 获取当前部门的岗位
        result.addAll(listByDeptId(deptId));
        
        // 递归获取子部门的岗位
        List<Department> childDepts = departmentService.listByParentId(deptId);
        for (Department childDept : childDepts) {
            result.addAll(getPositionsByDeptTree(childDept.getId()));
        }
        
        return result;
    }

    @Override
    @Transactional
    public boolean batchUpdateStatus(List<Long> ids, Integer status) {
        for (Long id : ids) {
            Position position = new Position();
            position.setId(id);
            position.setStatus(status);
            this.updateById(position);
        }
        return true;
    }
}
