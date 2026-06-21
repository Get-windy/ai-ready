package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.Partner;
import cn.aiedge.erp.stock.mapper.PartnerMapper;
import cn.aiedge.erp.stock.service.PartnerRoleService;
import cn.aiedge.erp.stock.service.PartnerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class PartnerServiceImpl extends ServiceImpl<PartnerMapper, Partner> implements PartnerService {

    private final PartnerRoleService partnerRoleService;

    @Override
    public IPage<Partner> getPartnerPage(String keyword, String partnerType, String status,
                                          Long categoryId, Integer pageNum, Integer pageSize) {
        Page<Partner> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<Partner>()
                .eq(Partner::getDeleted, 0);

        if (StringUtils.hasText(partnerType)) {
            wrapper.eq(Partner::getPartnerType, partnerType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Partner::getStatus, status);
        }
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Partner::getPartnerCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Partner::getPartnerCode, keyword)
                    .or().like(Partner::getPartnerName, keyword)
                    .or().like(Partner::getContactPerson, keyword)
                    .or().like(Partner::getContactPhone, keyword));
        }
        wrapper.orderByDesc(Partner::getCreateTime);
        return baseMapper.selectPartnerPage(page, wrapper);
    }

    @Override
    public Partner getPartnerDetail(Long id) {
        return baseMapper.selectPartnerDetail(id);
    }

    @Override
    public boolean createPartner(Partner partner) {
        if (partner.getStatus() == null) partner.setStatus("ENABLED");
        return save(partner);
    }

    @Override
    public boolean createPartnerWithRoles(Partner partner, List<String> roles) {
        if (partner.getStatus() == null) partner.setStatus("ENABLED");
        boolean saved = save(partner);
        if (saved && roles != null && !roles.isEmpty()) {
            for (String roleType : roles) {
                partnerRoleService.addRole(partner.getId(), roleType, roleType.equals(partner.getPartnerType()));
            }
        }
        return saved;
    }

    @Override
    public boolean updatePartner(Partner partner) {
        return updateById(partner);
    }

    @Override
    public List<Partner> search(String keyword, String partnerType) {
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<Partner>()
                .eq(Partner::getDeleted, 0)
                .eq(Partner::getStatus, "ENABLED");
        if (StringUtils.hasText(partnerType)) {
            wrapper.eq(Partner::getPartnerType, partnerType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Partner::getPartnerCode, keyword)
                    .or().like(Partner::getPartnerName, keyword));
        }
        wrapper.last("LIMIT 50");
        return list(wrapper);
    }

    @Override
    public List<Partner> getPartnerList(String partnerType, String status, Integer pageSize) {
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<Partner>()
                .eq(Partner::getDeleted, 0);
        if (StringUtils.hasText(partnerType)) {
            wrapper.eq(Partner::getPartnerType, partnerType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Partner::getStatus, status);
        } else {
            wrapper.eq(Partner::getStatus, "ENABLED");
        }
        wrapper.orderByDesc(Partner::getCreateTime);
        if (pageSize != null && pageSize > 0) {
            wrapper.last("LIMIT " + pageSize);
        } else {
            wrapper.last("LIMIT 200");
        }
        return list(wrapper);
    }

    @Override
    public Integer getNextSeq(String prefix) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codePrefix = prefix + "-" + dateStr;
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<Partner>()
                .likeRight(Partner::getPartnerCode, codePrefix)
                .eq(Partner::getDeleted, 0);
        long count = count(wrapper);
        return (int) count + 1;
    }
}
