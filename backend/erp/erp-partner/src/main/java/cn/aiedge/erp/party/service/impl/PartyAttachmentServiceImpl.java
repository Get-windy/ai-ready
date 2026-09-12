package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.PartyAttachment;
import cn.aiedge.erp.party.mapper.PartyAttachmentMapper;
import cn.aiedge.erp.party.service.PartyAttachmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class PartyAttachmentServiceImpl extends ServiceImpl<PartyAttachmentMapper, PartyAttachment>
        implements PartyAttachmentService {

    @Override
    public List<PartyAttachment> listByPartnerId(Long partnerId) {
        LambdaQueryWrapper<PartyAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyAttachment::getPartnerId, partnerId);
        wrapper.eq(PartyAttachment::getDeleted, 0);
        wrapper.orderByAsc(PartyAttachment::getSortOrder).orderByDesc(PartyAttachment::getId);
        return list(wrapper);
    }

    @Override
    public Map<Long, Integer> countByPartnerIds(List<Long> partnerIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (partnerIds == null || partnerIds.isEmpty()) {
            return result;
        }
        for (Map<String, Object> row : baseMapper.countByPartyIds(partnerIds)) {
            Object id = pick(row, "partnerId", "partnerid");
            Object cnt = pick(row, "cnt");
            if (id instanceof Number && cnt instanceof Number) {
                result.put(((Number) id).longValue(), ((Number) cnt).intValue());
            }
        }
        return result;
    }

    /** 列标签大小写兜底（PG 未加引号的别名会被折叠为小写） */
    private Object pick(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k)) {
                return row.get(k);
            }
        }
        return null;
    }
}
