package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.dto.LinkedAccountQuery;
import cn.aiedge.erp.party.dto.LinkedAccountVO;
import cn.aiedge.erp.party.entity.LinkedAccount;
import cn.aiedge.erp.party.mapper.LinkedAccountMapper;
import cn.aiedge.erp.party.service.ILinkedAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 互联账号服务实现
 */
@Service
public class LinkedAccountServiceImpl extends ServiceImpl<LinkedAccountMapper, LinkedAccount>
        implements ILinkedAccountService {

    /**
     * 互联平台字典（全局唯一，营销/会员/商城互联统一引用本字典，禁止各自另立枚举）。
     * 保持有序：微信 / 支付宝 / 抖音 / 商城 / 其它。
     */
    private static final Map<String, String> PLATFORM_DESC = new LinkedHashMap<>() {{
        put("WECHAT", "微信");
        put("ALIPAY", "支付宝");
        put("DOUYIN", "抖音");
        put("MALL", "商城");
        put("OTHER", "其它");
    }};

    /** 关联类型字典（全局唯一） */
    private static final Map<String, String> LINK_TYPE_DESC = new LinkedHashMap<>() {{
        put("MEMBER", "会员");
        put("CUSTOMER", "客户");
        put("OTHER", "其它");
    }};

    @Override
    public IPage<LinkedAccount> pageQuery(LinkedAccountQuery query, int pageNum, int pageSize) {
        LambdaQueryWrapper<LinkedAccount> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getPartyId() != null) {
                wrapper.eq(LinkedAccount::getPartyId, query.getPartyId());
            }
            if (StringUtils.hasText(query.getPlatform())) {
                wrapper.eq(LinkedAccount::getPlatform, query.getPlatform());
            }
            if (StringUtils.hasText(query.getLinkType())) {
                wrapper.eq(LinkedAccount::getLinkType, query.getLinkType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(LinkedAccount::getStatus, query.getStatus());
            }
            // 对标查询项「互联账号」（占位：请输入互联手机号码）：手机号 / 互联用户名 任一命中
            if (StringUtils.hasText(query.getKeyword())) {
                String kw = query.getKeyword().trim();
                wrapper.and(w -> w.like(LinkedAccount::getPhone, kw)
                        .or().like(LinkedAccount::getLinkedUserName, kw));
            }
        }
        wrapper.orderByDesc(LinkedAccount::getId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public LinkedAccountVO toVO(LinkedAccount entity) {
        if (entity == null) return null;
        LinkedAccountVO vo = new LinkedAccountVO();
        vo.setId(entity.getId());
        vo.setPartyId(entity.getPartyId());
        vo.setPartyCode(entity.getPartyCode());
        vo.setPartyName(entity.getPartyName());
        vo.setPlatform(entity.getPlatform());
        vo.setPlatformDesc(entity.getPlatform() != null
                ? PLATFORM_DESC.getOrDefault(entity.getPlatform(), entity.getPlatform()) : null);
        vo.setLinkType(entity.getLinkType());
        vo.setLinkTypeDesc(entity.getLinkType() != null
                ? LINK_TYPE_DESC.getOrDefault(entity.getLinkType(), entity.getLinkType()) : null);
        vo.setLinkedUserName(entity.getLinkedUserName());
        vo.setPhone(entity.getPhone());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(entity.getStatus() != null && entity.getStatus() == 0 ? "已解绑" : "已绑定");
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    @Override
    public List<LinkedAccountVO> toVOList(List<LinkedAccount> entities) {
        List<LinkedAccountVO> list = new ArrayList<>();
        if (entities == null) return list;
        for (LinkedAccount e : entities) {
            list.add(toVO(e));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(List<Long> ids, int status) {
        if (ids == null || ids.isEmpty()) return false;
        // 用 UpdateWrapper 显式 set：@TableLogic 字段与状态列在同一 UPDATE 中更可控，
        // 且避免 updateById 对 null 字段的忽略语义影响批量场景
        UpdateWrapper<LinkedAccount> wrapper = new UpdateWrapper<>();
        wrapper.set("status", status).in("id", ids);
        return update(wrapper);
    }

    @Override
    public LinkedAccount findConflict(String platform, String linkedUserName, Long excludeId) {
        if (!StringUtils.hasText(linkedUserName)) return null;
        LambdaQueryWrapper<LinkedAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LinkedAccount::getPlatform, StringUtils.hasText(platform) ? platform : "OTHER");
        wrapper.eq(LinkedAccount::getLinkedUserName, linkedUserName.trim());
        if (excludeId != null) {
            wrapper.ne(LinkedAccount::getId, excludeId);
        }
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public Map<String, List<Map<String, String>>> dict() {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        result.put("platforms", toDictList(PLATFORM_DESC));
        result.put("linkTypes", toDictList(LINK_TYPE_DESC));
        return result;
    }

    private List<Map<String, String>> toDictList(Map<String, String> source) {
        List<Map<String, String>> list = new ArrayList<>();
        source.forEach((value, label) -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("value", value);
            item.put("label", label);
            list.add(item);
        });
        return list;
    }
}
