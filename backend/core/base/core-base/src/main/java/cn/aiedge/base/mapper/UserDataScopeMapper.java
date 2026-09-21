package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.UserDataScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作员数据权限 Mapper
 * <p>
 * ⚠️ 刻意**不声明任何自定义方法**：本仓库踩过「Mapper 自定义方法无 SQL 绑定 →
 * Invalid bound statement (not found)」的坑。全部条件查询 / 更新一律用
 * LambdaQueryWrapper / UpdateWrapper 在 Service 层完成。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface UserDataScopeMapper extends BaseMapper<UserDataScope> {
}
