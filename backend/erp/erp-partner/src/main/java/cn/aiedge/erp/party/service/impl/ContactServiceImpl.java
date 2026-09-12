package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.Contact;
import cn.aiedge.erp.party.mapper.ContactMapper;
import cn.aiedge.erp.party.service.IContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 联系人（独立主数据）服务实现
 */
@Service
public class ContactServiceImpl extends ServiceImpl<ContactMapper, Contact> implements IContactService {
}
