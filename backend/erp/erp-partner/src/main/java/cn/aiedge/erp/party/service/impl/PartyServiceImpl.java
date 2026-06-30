package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

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

    @Override
    public IPage<Party> getPartyPage(String keyword, Integer partyType, Integer status,
                                     Long categoryId, String settleType, String region,
                                     String handler, String address,
                                     LocalDate createTimeStart, LocalDate createTimeEnd,
                                     LocalDate lastTradeStart, LocalDate lastTradeEnd,
                                     Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Party::getPartyCode, keyword)
                    .or().like(Party::getPartyName, keyword)
                    .or().like(Party::getShortName, keyword)
                    .or().like(Party::getPhone, keyword));
        }
        if (partyType != null) {
            wrapper.eq(Party::getPartyType, partyType);
        }
        if (status != null) {
            wrapper.eq(Party::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Party::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(settleType)) {
            wrapper.eq(Party::getSettlementType, "挂账".equals(settleType) ? 1 : 0);
        }
        if (StringUtils.hasText(address)) {
            wrapper.and(w -> w.like(Party::getRegisteredAddress, address)
                    .or().like(Party::getBusinessAddress, address));
        }
        if (createTimeStart != null) {
            wrapper.ge(Party::getCreateTime, createTimeStart.atStartOfDay());
        }
        if (createTimeEnd != null) {
            wrapper.le(Party::getCreateTime, createTimeEnd.plusDays(1).atStartOfDay());
        }
        if (lastTradeStart != null) {
            wrapper.ge(Party::getLastTradeDate, lastTradeStart);
        }
        if (lastTradeEnd != null) {
            wrapper.le(Party::getLastTradeDate, lastTradeEnd);
        }

        wrapper.orderByDesc(Party::getCreateTime);
        return this.page(new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20), wrapper);
    }

    @Override
    public List<Party> search(String keyword, Integer partyType) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getDeleted, 0);
        wrapper.eq(Party::getStatus, 1);
        if (partyType != null) {
            wrapper.eq(Party::getPartyType, partyType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Party::getPartyCode, keyword)
                    .or().like(Party::getPartyName, keyword)
                    .or().like(Party::getShortName, keyword)
                    .or().like(Party::getPhone, keyword));
        }
        wrapper.orderByDesc(Party::getCreateTime);
        wrapper.last("LIMIT 50");
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<Party> getPartyList(Integer partyType, Integer status, Integer pageSize) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getDeleted, 0);
        if (partyType != null) {
            wrapper.eq(Party::getPartyType, partyType);
        }
        if (status != null) {
            wrapper.eq(Party::getStatus, status);
        } else {
            wrapper.eq(Party::getStatus, 1);
        }
        wrapper.orderByDesc(Party::getCreateTime);
        if (pageSize != null && pageSize > 0) {
            wrapper.last("LIMIT " + pageSize);
        }
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public Integer getNextSeq(String prefix) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Party::getPartyCode, prefix);
        wrapper.eq(Party::getDeleted, 0);
        wrapper.orderByDesc(Party::getId);
        wrapper.last("LIMIT 1");
        Party last = this.baseMapper.selectOne(wrapper);
        if (last == null || last.getPartyCode() == null) {
            return 1;
        }
        String numPart = last.getPartyCode().substring(prefix.length());
        try {
            return Integer.parseInt(numPart) + 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
