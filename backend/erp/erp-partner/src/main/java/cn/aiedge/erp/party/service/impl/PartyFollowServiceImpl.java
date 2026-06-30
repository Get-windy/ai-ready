package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.PartyFollow;
import cn.aiedge.erp.party.mapper.PartyFollowMapper;
import cn.aiedge.erp.party.service.PartyFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class PartyFollowServiceImpl extends ServiceImpl<PartyFollowMapper, PartyFollow> implements PartyFollowService {
}
