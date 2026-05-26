package com.aiready.party.service.impl;

import com.aiready.party.entity.Party;
import com.aiready.party.mapper.PartyMapper;
import com.aiready.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 往来单位服务实现类
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PartyServiceImpl extends ServiceImpl<PartyMapper, Party> implements PartyService {

    @Override
    public Party getByPartyCode(String partyCode) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getPartyCode, partyCode);
        wrapper.eq(Party::getDeleted, 0);
        return this.baseMapper.selectOne(wrapper);
    }

    @Override
    public List<Party> listByPartyType(Integer partyType) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getPartyType, partyType);
        wrapper.eq(Party::getDeleted, 0);
        wrapper.orderByAsc(Party::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<Party> listByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getCategoryId, categoryId);
        wrapper.eq(Party::getDeleted, 0);
        wrapper.orderByAsc(Party::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public boolean checkPartyCodeExists(String partyCode) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getPartyCode, partyCode);
        wrapper.eq(Party::getDeleted, 0);
        return this.baseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean checkPartyCodeExists(String partyCode, Long excludeId) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getPartyCode, partyCode);
        wrapper.eq(Party::getDeleted, 0);
        wrapper.ne(Party::getId, excludeId);
        return this.baseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean updatePartyStatus(Long partyId, Integer status) {
        Party party = this.getById(partyId);
        if (party == null) {
            return false;
        }
        party.setStatus(status);
        return this.updateById(party);
    }

    @Override
    public List<Party> listByPartyLevel(String partyLevel) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getPartyLevel, partyLevel);
        wrapper.eq(Party::getDeleted, 0);
        wrapper.orderByAsc(Party::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<Party> listByStatus(Integer status) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getStatus, status);
        wrapper.eq(Party::getDeleted, 0);
        wrapper.orderByAsc(Party::getCreateTime);
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public Party getPartyDetailById(Long partyId) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getId, partyId);
        wrapper.eq(Party::getDeleted, 0);
        return this.baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean hasTransactions(Long partyId) {
        Long count = this.baseMapper.hasTransactions(partyId);
        return count != null && count > 0;
    }
}