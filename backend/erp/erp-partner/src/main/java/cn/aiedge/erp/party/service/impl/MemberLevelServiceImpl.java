package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.MemberLevel;
import cn.aiedge.erp.party.mapper.MemberLevelMapper;
import cn.aiedge.erp.party.service.IMemberLevelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 会员级别服务实现
 */
@Service
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements IMemberLevelService {
}
