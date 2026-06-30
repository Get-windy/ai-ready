package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.PartyContact;
import cn.aiedge.erp.party.mapper.PartyContactMapper;
import cn.aiedge.erp.party.service.IPartyContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PartyContactServiceImpl extends ServiceImpl<PartyContactMapper, PartyContact> implements IPartyContactService {
}
