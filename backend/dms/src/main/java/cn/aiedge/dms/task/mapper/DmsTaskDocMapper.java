package cn.aiedge.dms.task.mapper;

import cn.aiedge.dms.task.entity.DmsTaskDoc;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配送单内上游单据关联 Mapper
 */
@Mapper
public interface DmsTaskDocMapper extends BaseMapper<DmsTaskDoc> {
}
