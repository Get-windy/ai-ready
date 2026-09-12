package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.dto.PartyContactRow;
import cn.aiedge.erp.party.entity.Party;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PartyMapper extends BaseMapper<Party> {

    @Select("SELECT COUNT(*) FROM biz_party_transaction WHERE party_id = #{partyId} AND deleted = 0")
    Long hasTransactions(@Param("partyId") Long partyId);

    /**
     * 客户列表分页（资料 → 往来单位 → 客户 → 全部客户子标签）
     * 覆盖对标 18 项查询条件：筛选条件 / 联系地址 / 新增日期 / 最近交易 / 结款方式 /
     * 客户级别 / 默认经手人 / 推广人 / 所属仓库 / 所属区域 / 客户来源 / 显示状态 /
     * 显示供应商中的客户 / 只显示开通商城账号 / 只显示无销售记录客户。
     */
    IPage<Party> selectCustomerPage(Page<Party> page, @Param("q") PartyQueryParam q);

    /**
     * 会员管理分页（会员名称 / 联系电话 / 客户 / 客户经手人 / 最近交易 /
     * 会员级别 / 会员卡状态 / 生日 / 年龄 / 当前积分）。
     */
    IPage<Party> selectMemberPage(Page<Party> page, @Param("q") PartyQueryParam q);

    /**
     * 全部联系人分页（联系人 × 归属客户联表）。
     */
    IPage<PartyContactRow> selectContactPage(Page<PartyContactRow> page, @Param("q") PartyQueryParam q);
}
