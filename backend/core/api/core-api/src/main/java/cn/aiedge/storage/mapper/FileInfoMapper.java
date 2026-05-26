package cn.aiedge.storage.mapper;

import cn.aiedge.storage.model.FileInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件信息 Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}
