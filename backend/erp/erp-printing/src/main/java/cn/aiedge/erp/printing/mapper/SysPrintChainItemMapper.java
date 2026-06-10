package cn.aiedge.erp.printing.mapper;

import cn.aiedge.erp.printing.entity.v2.SysPrintChainItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysPrintChainItemMapper extends BaseMapper<SysPrintChainItem> {

    List<SysPrintChainItem> selectByChainIdOrdered(Long chainId);
}
