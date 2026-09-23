package cn.aiedge.common.serial.mapper;

import cn.aiedge.common.serial.BizNumberSequence;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BizNumberSequenceMapper extends BaseMapper<BizNumberSequence> {

    /**
     * 原子递增序列号（SELECT FOR UPDATE + UPDATE）
     * 如果日期已变化，先重置为0再递增
     */
    @Select("SELECT id, biz_type, locale, current_seq, seq_date, prefix, seq_length, max_seq, update_time FROM biz_number_sequence WHERE biz_type = #{bizType} AND locale = #{locale} AND tenant_id = #{tenantId} FOR UPDATE")
    BizNumberSequence selectForUpdateWithLocale(@Param("bizType") String bizType, @Param("locale") String locale, @Param("tenantId") Long tenantId);

    /**
     * 原子递增序列号（针对中文环境，使用默认zh_CN）
     */
    @Select("SELECT id, biz_type, locale, current_seq, seq_date, prefix, seq_length, max_seq, update_time FROM biz_number_sequence WHERE biz_type = #{bizType} AND locale = 'zh_CN' AND tenant_id = #{tenantId} FOR UPDATE")
    BizNumberSequence selectForUpdate(@Param("bizType") String bizType, @Param("tenantId") Long tenantId);

    @Update("UPDATE biz_number_sequence SET current_seq = current_seq + 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int incrementSeq(@Param("id") Long id);

    @Update("UPDATE biz_number_sequence SET current_seq = 1, seq_date = #{today}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int resetAndIncrement(@Param("id") Long id, @Param("today") String today);

    /**
     * 给「本租户还没有号段行」的场景按模板补种一行（幂等），返回插入行数。
     *
     * <p><b>为什么需要它</b>：`biz_number_sequence` 的唯一键是 `(tenant_id, biz_type, locale)`，
     * 而迁移只给租户 1 种了号段（真库实测全表 72 行、`tenant_id` 全部为 1）。新租户第一次建单据时
     * 查不到行，旧实现直接抛 `IllegalArgumentException("未配置编号序列…")` —— 也就是**任何非 1 号租户
     * 都建不出员工/合同/单据**。这里改为「按同 biz_type 的既有行补种一行」，让号段随首次使用自愈。</p>
     *
     * <p><b>模板取自别的租户</b>（新租户自己一行都没有）⇒ 必须 {@code @InterceptorIgnore(tenantLine = "true")}
     * 关掉租户注入，否则模板 SELECT 会被加上 `AND tenant_id = <会话租户>`，一行都读不到、
     * 插入变成 0 行，表现为「自愈静默失效、还是抛原来的异常」。</p>
     *
     * <p><b>模板选择</b>：优先同 `locale` 的行，其次任意 locale（按 tenant_id 升序取第一条，
     * 即最早那个租户的配置）。全库都没有该 `biz_type` 的模板时不插任何行 —— 这种就是 prompt 写错了
     * bizType，应该继续抛异常而不是凭空造一个前缀。</p>
     *
     * <p><b>并发</b>：`ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING` 兜住两个线程同时首次使用的竞态；
     * 插入 0 行时调用方会重新 `SELECT … FOR UPDATE` 取值。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO biz_number_sequence "
            + "(biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time) "
            + "SELECT t.biz_type, '19700101', 0, t.max_seq, t.prefix, t.seq_length, #{tenantId}, #{locale}, CURRENT_TIMESTAMP "
            + "FROM biz_number_sequence t "
            + "WHERE t.biz_type = #{bizType} "
            + "ORDER BY (t.locale = #{locale}) DESC, t.tenant_id "
            + "LIMIT 1 "
            + "ON CONFLICT (tenant_id, biz_type, locale) DO NOTHING")
    int seedMissingSequence(@Param("bizType") String bizType,
                            @Param("locale") String locale,
                            @Param("tenantId") Long tenantId);
}
