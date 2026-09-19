package cn.aiedge.module.mapper;

import cn.aiedge.module.model.SysModule;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysModuleMapper extends BaseMapper<SysModule> {

    /**
     * 读取系统「模块注册表」全量已安装模块（应用中心「功能模块」区的数据源）。
     *
     * <p><b>为什么必须 {@code @InterceptorIgnore(tenantLine = "true")}？</b>
     * `sys_module` 有 `tenant_id` 列，但注册表本身是**平台资产**：实测全部 6 行的
     * `tenant_id = 0`。多租户插件会无条件注入 `tenant_id = <会话租户>`，
     * 而租户会话的租户 id 是 1 / 2 / …，于是 `tenant_id = 0` 的注册表**一行都读不出来** ——
     * 表现是「应用中心功能模块区恒空」，且 SQL 不报错、极难定位（同类实踩见
     * 《全局默认配置被租户插件过滤》与 `SysConfigMapper` 的同类处理）。
     * 因此这里显式跳过租户行的注入；租户侧的「是否已开通」由 `sys_tenant_module`
     * 按会话租户单独查询后在本方法结果上做标记，越权面不受影响（只读、无写入口）。
     *
     * @return 未删除的模块列表（按 sort_order、id 升序）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_module WHERE deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<SysModule> selectInstalledModules();
}
