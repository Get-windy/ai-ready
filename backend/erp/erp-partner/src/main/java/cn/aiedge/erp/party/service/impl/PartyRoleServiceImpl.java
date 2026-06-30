package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.PartyRole;
import cn.aiedge.erp.party.mapper.PartyRoleMapper;
import cn.aiedge.erp.party.service.IPartyRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PartyRoleServiceImpl extends ServiceImpl<PartyRoleMapper, PartyRole> implements IPartyRoleService {
}
