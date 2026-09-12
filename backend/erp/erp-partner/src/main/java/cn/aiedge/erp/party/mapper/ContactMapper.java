package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.entity.Contact;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 联系人（独立主数据）Mapper
 */
@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
}
