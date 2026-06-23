package cn.aiedge.erp.partner.mapper;

import cn.aiedge.erp.partner.entity.Partner;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PartnerMapper extends BaseMapper<Partner> {
    IPage<Partner> selectPartnerPage(Page<Partner> page, @Param("ew") com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Partner> wrapper);

    Partner selectPartnerDetail(Long id);
}