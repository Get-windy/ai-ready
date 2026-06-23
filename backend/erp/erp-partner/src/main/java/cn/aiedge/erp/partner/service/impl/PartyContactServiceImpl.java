package cn.aiedge.erp.partner.service.impl;

import cn.aiedge.erp.partner.entity.PartyContact;
import cn.aiedge.erp.partner.mapper.PartyContactMapper;
import cn.aiedge.erp.partner.service.IPartyContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PartyContactServiceImpl extends ServiceImpl<PartyContactMapper, PartyContact> implements IPartyContactService {
}