package cn.aiedge.agreement.service.impl;

import cn.aiedge.agreement.dto.TermOptionDTO;
import cn.aiedge.agreement.dto.TermOptionGroupVO;
import cn.aiedge.agreement.dto.TermOptionVO;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.mapper.AgreementTermOptionMapper;
import cn.aiedge.agreement.service.AgreementTermOptionService;
import cn.aiedge.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 平台条款字典实现。
 *
 * <p><b>平台只定义选项，不定义默认值</b>：本类刻意不提供任何「按默认值兜底」的方法，
 * 实体里也没有 defaultOption 字段（㉜ / §3.4.4d1）。
 * 任何"某条款没选就用 X"的写法都是把平台变成替双方做决定的一方。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementTermOptionServiceImpl implements AgreementTermOptionService {

    /** 法务审核状态的合法取值。 */
    private static final Set<String> LEGAL_STATUS = new LinkedHashSet<>(
            List.of("PENDING", "APPROVED", "REJECTED"));

    private final AgreementTermOptionMapper optionMapper;

    @Override
    public List<TermOptionGroupVO> grouped() {
        List<AgreementTermOption> all = listAll();
        Map<String, List<AgreementTermOption>> byTerm = new LinkedHashMap<>();
        for (AgreementTermOption o : all) {
            byTerm.computeIfAbsent(o.getTermCode(), k -> new ArrayList<>()).add(o);
        }
        List<TermOptionGroupVO> groups = new ArrayList<>(byTerm.size());
        for (Map.Entry<String, List<AgreementTermOption>> e : byTerm.entrySet()) {
            List<AgreementTermOption> options = e.getValue();
            TermOptionGroupVO group = new TermOptionGroupVO();
            group.setTermCode(e.getKey());
            group.setTermName(options.isEmpty() ? e.getKey() : options.get(0).getOptionLabel());
            // 组必填 = 组内任一选项标了 required（与前端 isRequiredGroup 同口径）
            group.setRequired(options.stream().anyMatch(o -> Boolean.TRUE.equals(o.getRequired())));
            List<TermOptionVO> vos = new ArrayList<>(options.size());
            for (AgreementTermOption o : options) {
                vos.add(toVO(o));
            }
            group.setOptions(vos);
            groups.add(group);
        }
        return groups;
    }

    @Override
    public List<TermOptionVO> listByTermCode(String termCode) {
        LambdaQueryWrapper<AgreementTermOption> wrapper = new LambdaQueryWrapper<>();
        if (termCode != null && !termCode.isBlank()) {
            wrapper.eq(AgreementTermOption::getTermCode, termCode.trim());
        }
        wrapper.orderByAsc(AgreementTermOption::getTermCode).orderByAsc(AgreementTermOption::getSort);
        List<TermOptionVO> list = new ArrayList<>();
        for (AgreementTermOption o : optionMapper.selectList(wrapper)) {
            list.add(toVO(o));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TermOptionDTO dto) {
        String termCode = requireText(dto.getTermCode(), "请填写条款类别");
        String label = requireText(dto.getOptionLabel(), "请填写选项名称（双方在下拉里看到的文字）");
        String semantics = requireText(dto.getSemantics(),
                "请填写含义说明：选了这一项，系统会怎么执行。这段文字是给双方看的条款说明书，也是举证要点");
        String optionCode = dto.getOptionCode() == null || dto.getOptionCode().isBlank()
                ? generateOptionCode(termCode)
                : dto.getOptionCode().trim();

        assertUnique(termCode, optionCode, null);

        AgreementTermOption option = new AgreementTermOption();
        option.setTermCode(termCode);
        option.setOptionCode(optionCode);
        option.setOptionLabel(label);
        option.setSemantics(semantics);
        option.setNeedsParam(trimToNull(dto.getNeedsParam()));
        option.setRequired(Boolean.TRUE.equals(dto.getRequired()));
        option.setLegalReviewStatus(normalizeLegalStatus(dto.getLegalReviewStatus()));
        option.setSort(dto.getSort() == null ? 0 : dto.getSort());
        option.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        optionMapper.insert(option);
        log.info("条款字典新增: termCode={}, optionCode={}, 必填={}, 法务={}",
                termCode, optionCode, option.getRequired(), option.getLegalReviewStatus());
        return option.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TermOptionDTO dto) {
        AgreementTermOption exists = id == null ? null : optionMapper.selectById(id);
        if (exists == null) {
            throw BusinessException.notFound("条款选项不存在");
        }
        // 条款类别不允许改：类别是"归到哪个下拉"的依据，改了会让已签协议看起来选了别的类别
        if (trimToNull(dto.getTermCode()) != null && !dto.getTermCode().trim().equals(exists.getTermCode())) {
            throw BusinessException.badRequest("条款类别不可修改；如需归入别的类别，请新建选项");
        }
        AgreementTermOption patch = new AgreementTermOption();
        patch.setId(id);
        if (trimToNull(dto.getOptionCode()) != null && !dto.getOptionCode().trim().equals(exists.getOptionCode())) {
            assertUnique(exists.getTermCode(), dto.getOptionCode().trim(), id);
            patch.setOptionCode(dto.getOptionCode().trim());
        }
        if (trimToNull(dto.getOptionLabel()) != null) {
            patch.setOptionLabel(dto.getOptionLabel().trim());
        }
        if (trimToNull(dto.getSemantics()) != null) {
            patch.setSemantics(dto.getSemantics().trim());
        }
        if (dto.getNeedsParam() != null) {
            patch.setNeedsParam(trimToNull(dto.getNeedsParam()));
        }
        if (dto.getRequired() != null) {
            patch.setRequired(dto.getRequired());
        }
        if (dto.getLegalReviewStatus() != null) {
            patch.setLegalReviewStatus(normalizeLegalStatus(dto.getLegalReviewStatus()));
        }
        if (dto.getSort() != null) {
            patch.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            patch.setStatus(dto.getStatus());
        }
        optionMapper.updateById(patch);
        // ⚠️ 字典变更**不影响已签协议**：历史版本的 optionLabel / semantics 冗余存在快照里，
        //    这正是"举证时能说清当时约定了什么"的前提（§3.4.4d2 规定 3）。
        log.info("条款字典已更新: id={}, 影响范围=以后新签的协议（历史快照不受影响）", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AgreementTermOption exists = id == null ? null : optionMapper.selectById(id);
        if (exists == null) {
            throw BusinessException.notFound("条款选项不存在");
        }
        optionMapper.deleteById(id);
        log.info("条款字典已删除（软删）: id={}, termCode={}, optionCode={}；历史协议快照不受影响",
                id, exists.getTermCode(), exists.getOptionCode());
    }

    // ── 内部 ──

    private List<AgreementTermOption> listAll() {
        return optionMapper.selectList(new LambdaQueryWrapper<AgreementTermOption>()
                .orderByAsc(AgreementTermOption::getTermCode)
                .orderByAsc(AgreementTermOption::getSort));
    }

    private void assertUnique(String termCode, String optionCode, Long excludeId) {
        LambdaQueryWrapper<AgreementTermOption> wrapper = new LambdaQueryWrapper<AgreementTermOption>()
                .eq(AgreementTermOption::getTermCode, termCode)
                .eq(AgreementTermOption::getOptionCode, optionCode);
        if (excludeId != null) {
            wrapper.ne(AgreementTermOption::getId, excludeId);
        }
        Long count = optionMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.badRequest("同一类别下选项编码「" + optionCode + "」已存在，请换一个");
        }
    }

    /** 选项编码留空时按 `类别_序号` 生成并避让已存在的编码。 */
    private String generateOptionCode(String termCode) {
        Long count = optionMapper.selectCount(new LambdaQueryWrapper<AgreementTermOption>()
                .eq(AgreementTermOption::getTermCode, termCode));
        long seq = (count == null ? 0L : count) + 1L;
        for (int i = 0; i < 1000; i++) {
            String candidate = termCode + "_" + String.format("%02d", seq + i);
            Long exists = optionMapper.selectCount(new LambdaQueryWrapper<AgreementTermOption>()
                    .eq(AgreementTermOption::getTermCode, termCode)
                    .eq(AgreementTermOption::getOptionCode, candidate));
            if (exists == null || exists == 0) {
                return candidate;
            }
        }
        throw BusinessException.badRequest("无法自动生成选项编码，请手工填写");
    }

    private String normalizeLegalStatus(String status) {
        String s = trimToNull(status);
        if (s == null) {
            // 前端表单没有这一项：默认「待审」而不是「已通过」—— 不能凭空替法务点头
            return "PENDING";
        }
        String upper = s.toUpperCase();
        if (!LEGAL_STATUS.contains(upper)) {
            throw BusinessException.badRequest("法务审核状态「" + status + "」不支持；可选：PENDING / APPROVED / REJECTED");
        }
        return upper;
    }

    private TermOptionVO toVO(AgreementTermOption o) {
        TermOptionVO vo = new TermOptionVO();
        vo.setId(o.getId());
        vo.setTermCode(o.getTermCode());
        vo.setTermName(o.getOptionLabel());
        vo.setOptionCode(o.getOptionCode());
        vo.setOptionLabel(o.getOptionLabel());
        vo.setSemantics(o.getSemantics());
        vo.setNeedsParam(o.getNeedsParam());
        vo.setRequired(o.getRequired());
        vo.setLegalReviewStatus(o.getLegalReviewStatus());
        vo.setSort(o.getSort());
        vo.setStatus(o.getStatus());
        return vo;
    }

    private static String requireText(String value, String message) {
        String v = trimToNull(value);
        if (v == null) {
            throw BusinessException.badRequest(message);
        }
        return v;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
