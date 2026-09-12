package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.dto.PartyContactRow;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.aiedge.erp.party.mapper.PartyQueryParam;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            // 基础资料占位「请输入物流公司名称/编号/备注」→ 编号/名称/简称/助记码/电话/备注 模糊匹配
            wrapper.and(w -> w.like(Party::getPartyCode, keyword)
                    .or().like(Party::getPartyName, keyword)
                    .or().like(Party::getShortName, keyword)
                    .or().like(Party::getMnemonicCode, keyword)
                    .or().like(Party::getPhone, keyword)
                    .or().like(Party::getRemark, keyword));
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
        if (createTimeStart != null) {
            wrapper.ge(Party::getCreateTime, createTimeStart.atStartOfDay());
        }
        if (createTimeEnd != null) {
            wrapper.le(Party::getCreateTime, createTimeEnd.plusDays(1).atStartOfDay());
        }

        wrapper.orderByDesc(Party::getCreateTime);
        return this.page(new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20), wrapper);
    }

    /** 往来单位类型 → 多重身份标识（与 party_type 一一对应） */
    private static final Map<Integer, String> ROLE_BY_PARTY_TYPE = Map.of(
            1, "CUSTOMER",
            2, "SUPPLIER",
            3, "LOGISTICS",
            4, "OTHER");

    @Override
    public IPage<Party> getPartyPageByRole(String keyword, Integer partyType, Integer status,
                                           Long categoryId, String settleType,
                                           LocalDate createTimeStart, LocalDate createTimeEnd,
                                           Boolean showAsCustomer, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Party::getPartyCode, keyword)
                    .or().like(Party::getPartyName, keyword)
                    .or().like(Party::getShortName, keyword)
                    .or().like(Party::getMnemonicCode, keyword)
                    .or().like(Party::getPhone, keyword)
                    .or().like(Party::getRemark, keyword));
        }
        if (partyType != null) {
            String role = ROLE_BY_PARTY_TYPE.get(partyType);
            if (Boolean.TRUE.equals(showAsCustomer) && role != null) {
                // 合并多重身份：本类型 ∪ roles 中含该身份的单位（对标「显示客户中的供应商」）
                wrapper.and(w -> w.eq(Party::getPartyType, partyType)
                        .or().like(Party::getRoles, role));
            } else {
                wrapper.eq(Party::getPartyType, partyType);
            }
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
        if (createTimeStart != null) {
            wrapper.ge(Party::getCreateTime, createTimeStart.atStartOfDay());
        }
        if (createTimeEnd != null) {
            wrapper.le(Party::getCreateTime, createTimeEnd.plusDays(1).atStartOfDay());
        }

        wrapper.orderByAsc(Party::getPartyCode);
        return this.page(new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20), wrapper);
    }

    @Override
    public IPage<Party> getCustomerPage(PartyQueryParam query, Integer pageNum, Integer pageSize) {
        PartyQueryParam q = query != null ? query : new PartyQueryParam();
        return this.baseMapper.selectCustomerPage(
                new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20), q);
    }

    @Override
    public IPage<Party> getMemberPage(PartyQueryParam query, Integer pageNum, Integer pageSize) {
        PartyQueryParam q = query != null ? query : new PartyQueryParam();
        return this.baseMapper.selectMemberPage(
                new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20), q);
    }

    @Override
    public IPage<PartyContactRow> getContactPage(PartyQueryParam query, Integer pageNum, Integer pageSize) {
        PartyQueryParam q = query != null ? query : new PartyQueryParam();
        return this.baseMapper.selectContactPage(
                new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20), q);
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
        wrapper.last("LIMIT 500");
        List<Party> candidates = this.baseMapper.selectList(wrapper);

        // 编号有两种形态：
        //   1) 前缀 + 序号（如 GYS494557）——历史形态，序号全局递增
        //   2) 前缀-YYYYMMDD-序号（如 WLDW-20260911-001）——前端 generateCode 形态，序号按日重置
        // 旧实现只按「前缀后的整串」parseInt，形态 2 必然解析失败而恒返回 1，导致编号重复。
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Pattern datedPattern = Pattern.compile("^" + Pattern.quote(prefix) + "-(\\d{8})-(\\d+)$");
        Pattern plainPattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
        int max = 0;
        for (Party p : candidates) {
            String code = p.getPartyCode();
            if (code == null) continue;
            Matcher dated = datedPattern.matcher(code);
            if (dated.matches()) {
                if (today.equals(dated.group(1))) {
                    max = Math.max(max, parseIntSafe(dated.group(2)));
                }
                continue;
            }
            Matcher plain = plainPattern.matcher(code);
            if (plain.matches()) {
                max = Math.max(max, parseIntSafe(plain.group(1)));
            }
        }
        return max + 1;
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
