package cn.aiedge.dms.vehicle.mapper;

import cn.aiedge.dms.vehicle.dto.VendorOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 往来单位只读查询（维保厂商选择器 / 厂商名校验）
 *
 * <p>红线：厂商与《资料 → 往来单位》是**同一主数据**（`biz_party`），DMS 侧只读引用，
 * 不新建厂商表、不写往来单位数据；取数口径与《往来单位》一致（未删除 + 已启用）。</p>
 *
 * @author AI-Ready Team
 */
@Mapper
public interface PartnerLookupMapper {

    /**
     * 厂商候选：供应商(2) + 其他往来单位(4)，按名称/编码/助记码模糊
     */
    @Select("<script>"
            + "SELECT p.id AS id, p.party_code AS code, p.party_name AS name, p.party_type AS partyType "
            + "FROM biz_party p "
            + "WHERE p.deleted = 0 AND p.status = 1 AND p.party_type IN (2, 4) "
            + "<if test=\"keyword != null\">"
            + "  AND (p.party_name LIKE CONCAT('%', #{keyword}, '%') "
            + "       OR p.party_code LIKE CONCAT('%', #{keyword}, '%') "
            + "       OR p.mnemonic_code LIKE CONCAT('%', #{keyword}, '%')) "
            + "</if>"
            + "ORDER BY p.party_name ASC LIMIT #{limit}"
            + "</script>")
    List<VendorOptionVO> searchVendors(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 按ID取厂商（用于「选择器带 ID 保存」时校验并回填名称快照）
     */
    @Select("SELECT p.id AS id, p.party_code AS code, p.party_name AS name, p.party_type AS partyType "
            + "FROM biz_party p "
            + "WHERE p.id = #{id} AND p.deleted = 0 AND p.party_type IN (2, 4)")
    VendorOptionVO findVendor(@Param("id") Long id);
}
