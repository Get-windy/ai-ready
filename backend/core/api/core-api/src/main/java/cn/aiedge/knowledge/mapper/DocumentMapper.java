package cn.aiedge.knowledge.mapper;

import cn.aiedge.knowledge.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<KnowledgeDocument> {
}
