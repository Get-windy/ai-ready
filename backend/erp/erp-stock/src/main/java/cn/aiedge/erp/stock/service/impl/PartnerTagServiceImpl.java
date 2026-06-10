package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.PartnerTag;
import cn.aiedge.erp.stock.entity.PartnerTagRelation;
import cn.aiedge.erp.stock.mapper.PartnerTagMapper;
import cn.aiedge.erp.stock.mapper.PartnerTagRelationMapper;
import cn.aiedge.erp.stock.service.PartnerTagService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PartnerTagServiceImpl extends ServiceImpl<PartnerTagMapper, PartnerTag>
        implements PartnerTagService {

    private final PartnerTagRelationMapper relationMapper;

    @Override
    public List<Long> getTagIdsByPartner(Long partnerId) {
        return relationMapper.selectList(new QueryWrapper<PartnerTagRelation>()
                        .eq("partner_id", partnerId))
                .stream()
                .map(PartnerTagRelation::getTagId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean attachTags(Long partnerId, List<Long> tagIds) {
        // 删除旧关联
        relationMapper.delete(new QueryWrapper<PartnerTagRelation>()
                .eq("partner_id", partnerId));
        // 新增新关联
        if (tagIds != null && !tagIds.isEmpty()) {
            List<PartnerTagRelation> relations = tagIds.stream()
                    .map(tagId -> new PartnerTagRelation().setPartnerId(partnerId).setTagId(tagId))
                    .collect(Collectors.toList());
            relations.forEach(relationMapper::insert);
        }
        return true;
    }
}
