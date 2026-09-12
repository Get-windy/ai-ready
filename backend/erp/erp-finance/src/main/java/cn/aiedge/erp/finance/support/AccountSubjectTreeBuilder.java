package cn.aiedge.erp.finance.support;

import cn.aiedge.erp.finance.dto.AccountSubjectDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会计科目树构建（共享工具）。
 *
 * <p>供「会计科目」页与「费用类型」页（费用类会计科目视图）共用：
 * 由扁平科目列表按 parentId 建树，父节点不在集合内的节点自动升为顶层，
 * 同级按科目编号升序。视图过滤（如仅损益类借方）后，父科目若被过滤掉，
 * 其子科目仍会作为顶层返回，保证不漏行。</p>
 */
public final class AccountSubjectTreeBuilder {

    private AccountSubjectTreeBuilder() {
    }

    public static List<AccountSubjectDTO> build(List<AccountSubjectDTO> flat) {
        if (flat == null || flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, AccountSubjectDTO> byId = new LinkedHashMap<>();
        for (AccountSubjectDTO dto : flat) {
            dto.setChildren(new ArrayList<>());
            byId.put(dto.getId(), dto);
        }
        List<AccountSubjectDTO> roots = new ArrayList<>();
        for (AccountSubjectDTO dto : flat) {
            AccountSubjectDTO parent = dto.getParentId() == null ? null : byId.get(dto.getParentId());
            if (parent == null) {
                roots.add(dto);
            } else {
                parent.getChildren().add(dto);
            }
        }
        Comparator<AccountSubjectDTO> byCode = Comparator.comparing(
                AccountSubjectDTO::getSubjectCode, Comparator.nullsLast(Comparator.naturalOrder()));
        roots.sort(byCode);
        byId.values().forEach(d -> d.getChildren().sort(byCode));
        return roots;
    }
}
