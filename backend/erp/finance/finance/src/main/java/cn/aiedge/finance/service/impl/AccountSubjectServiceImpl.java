package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.mapper.AccountSubjectMapper;
import cn.aiedge.finance.service.AccountSubjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountSubjectServiceImpl extends ServiceImpl<AccountSubjectMapper, AccountSubject> implements AccountSubjectService {
    
    @Override
    public List<AccountSubject> listAllEnabled(Long tenantId) {
        return baseMapper.listAllEnabled(tenantId);
    }
    
    @Override
    public List<AccountSubject> listByParentId(Long tenantId, Long parentId) {
        return baseMapper.listByParentId(tenantId, parentId);
    }
    
    @Override
    public List<AccountSubject> listByType(Long tenantId, Integer subjectType) {
        return baseMapper.listByType(tenantId, subjectType);
    }
    
    @Override
    public List<AccountSubject> listLeafSubjects(Long tenantId) {
        return baseMapper.listLeafSubjects(tenantId);
    }
    
    @Override
    public AccountSubject getByCode(Long tenantId, String subjectCode) {
        return baseMapper.getByCode(tenantId, subjectCode);
    }
    
    @Override
    public Page<AccountSubject> pageList(Long tenantId, String subjectCode, String subjectName, Integer subjectType, Integer level, Integer enabled, Page<AccountSubject> page) {
        LambdaQueryWrapper<AccountSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSubject::getTenantId, tenantId)
               .eq(AccountSubject::getDeleted, 0);
        if (subjectCode != null && !subjectCode.isEmpty()) {
            wrapper.like(AccountSubject::getSubjectCode, subjectCode);
        }
        if (subjectName != null && !subjectName.isEmpty()) {
            wrapper.like(AccountSubject::getSubjectName, subjectName);
        }
        if (subjectType != null) {
            wrapper.eq(AccountSubject::getSubjectType, subjectType);
        }
        if (level != null) {
            wrapper.eq(AccountSubject::getLevel, level);
        }
        if (enabled != null) {
            wrapper.eq(AccountSubject::getEnabled, enabled);
        }
        wrapper.orderByAsc(AccountSubject::getSubjectCode);
        return this.page(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createSubject(AccountSubject subject) {
        AccountSubject parent = null;
        if (subject.getParentId() != null && subject.getParentId() > 0) {
            parent = this.getById(subject.getParentId());
            if (parent == null) {
                throw new RuntimeException("父级科目不存在");
            }
            if (parent.getLeafFlag() == 1) {
                parent.setLeafFlag(0);
                this.updateById(parent);
            }
        }
        int childCount = baseMapper.countChildren(subject.getTenantId(), subject.getParentId() == null ? 0 : subject.getParentId());
        subject.setLeafFlag(1);
        subject.setEnabled(1);
        return this.save(subject);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSubject(AccountSubject subject) {
        AccountSubject existing = this.getById(subject.getId());
        if (existing == null) {
            throw new RuntimeException("科目不存在");
        }
        if (!existing.getSubjectCode().equals(subject.getSubjectCode())) {
            AccountSubject byCode = this.getByCode(subject.getTenantId(), subject.getSubjectCode());
            if (byCode != null) {
                throw new RuntimeException("科目编码已存在");
            }
        }
        return this.updateById(subject);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSubject(Long tenantId, Long subjectId) {
        int childCount = baseMapper.countChildren(tenantId, subjectId);
        if (childCount > 0) {
            throw new RuntimeException("存在下级科目，不能删除");
        }
        AccountSubject subject = this.getById(subjectId);
        if (subject == null) {
            throw new RuntimeException("科目不存在");
        }
        return this.removeById(subjectId);
    }
    
    @Override
    public boolean enableSubject(Long tenantId, Long subjectId) {
        AccountSubject subject = this.getById(subjectId);
        if (subject == null) {
            throw new RuntimeException("科目不存在");
        }
        subject.setEnabled(1);
        return this.updateById(subject);
    }
    
    @Override
    public boolean disableSubject(Long tenantId, Long subjectId) {
        AccountSubject subject = this.getById(subjectId);
        if (subject == null) {
            throw new RuntimeException("科目不存在");
        }
        subject.setEnabled(0);
        return this.updateById(subject);
    }
    
    @Override
    public List<AccountSubject> buildTree(Long tenantId) {
        List<AccountSubject> allSubjects = this.listAllEnabled(tenantId);
        Map<Long, AccountSubject> subjectMap = new HashMap<>();
        List<AccountSubject> rootSubjects = new ArrayList<>();
        for (AccountSubject subject : allSubjects) {
            subjectMap.put(subject.getId(), subject);
        }
        for (AccountSubject subject : allSubjects) {
            if (subject.getParentId() == null || subject.getParentId() == 0) {
                rootSubjects.add(subject);
            } else {
                AccountSubject parent = subjectMap.get(subject.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(subject);
                }
            }
        }
        return rootSubjects;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initStandardSubjects(Long tenantId) {
        List<AccountSubject> standardSubjects = new ArrayList<>();
        String[][] subjects = {
            {"1001", "库存现金", "1", "1", null, "1"},
            {"1002", "银行存款", "1", "1", null, "1"},
            {"1012", "其他货币资金", "1", "1", null, "1"},
            {"1101", "交易性金融资产", "1", "1", null, "1"},
            {"1121", "应收票据", "1", "1", null, "1"},
            {"1122", "应收账款", "1", "1", null, "1"},
            {"1123", "预付账款", "1", "1", null, "1"},
            {"1131", "应收股利", "1", "1", null, "1"},
            {"1132", "应收利息", "1", "1", null, "1"},
            {"1221", "其他应收款", "1", "1", null, "1"},
            {"1231", "坏账准备", "1", "1", null, "2"},
            {"1401", "材料采购", "1", "1", null, "1"},
            {"1402", "在途物资", "1", "1", null, "1"},
            {"1403", "原材料", "1", "1", null, "1"},
            {"1404", "材料成本差异", "1", "1", null, "1"},
            {"1405", "库存商品", "1", "1", null, "1"},
            {"1406", "发出商品", "1", "1", null, "1"},
            {"1407", "商品进销差价", "1", "1", null, "2"},
            {"1408", "委托加工物资", "1", "1", null, "1"},
            {"1411", "周转材料", "1", "1", null, "1"},
            {"1471", "存货跌价准备", "1", "1", null, "2"},
            {"1501", "持有至到期投资", "1", "1", null, "1"},
            {"1502", "持有至到期投资减值准备", "1", "1", null, "2"},
            {"1511", "长期股权投资", "1", "1", null, "1"},
            {"1512", "长期股权投资减值准备", "1", "1", null, "2"},
            {"1601", "固定资产", "1", "1", null, "1"},
            {"1602", "累计折旧", "1", "1", null, "2"},
            {"1603", "固定资产减值准备", "1", "1", null, "2"},
            {"1604", "在建工程", "1", "1", null, "1"},
            {"1605", "工程物资", "1", "1", null, "1"},
            {"1606", "固定资产清理", "1", "1", null, "1"},
            {"1701", "无形资产", "1", "1", null, "1"},
            {"1702", "累计摊销", "1", "1", null, "2"},
            {"1703", "无形资产减值准备", "1", "1", null, "2"},
            {"1801", "长期待摊费用", "1", "1", null, "1"},
            {"1811", "递延所得税资产", "1", "1", null, "1"},
            {"1901", "待处理财产损溢", "1", "1", null, "1"},
            {"2001", "短期借款", "2", "1", null, "2"},
            {"2002", "交易性金融负债", "2", "1", null, "2"},
            {"2101", "应付票据", "2", "1", null, "2"},
            {"2102", "应付账款", "2", "1", null, "2"},
            {"2103", "预收账款", "2", "1", null, "2"},
            {"2201", "应付职工薪酬", "2", "1", null, "2"},
            {"2202", "应交税费", "2", "1", null, "2"},
            {"2203", "应付利息", "2", "1", null, "2"},
            {"2204", "应付股利", "2", "1", null, "2"},
            {"2211", "其他应付款", "2", "1", null, "2"},
            {"2221", "长期借款", "2", "1", null, "2"},
            {"2231", "应付债券", "2", "1", null, "2"},
            {"2241", "其他非流动负债", "2", "1", null, "2"},
            {"2501", "递延所得税负债", "2", "1", null, "2"},
            {"3001", "实收资本", "3", "1", null, "2"},
            {"3002", "资本公积", "3", "1", null, "2"},
            {"3101", "盈余公积", "3", "1", null, "2"},
            {"3103", "本年利润", "3", "1", null, "2"},
            {"3104", "利润分配", "3", "1", null, "2"},
            {"4001", "生产成本", "4", "1", null, "1"},
            {"4002", "制造费用", "4", "1", null, "1"},
            {"4101", "研发支出", "4", "1", null, "1"},
            {"5001", "主营业务收入", "5", "1", null, "2"},
            {"5002", "其他业务收入", "5", "1", null, "2"},
            {"5101", "投资收益", "5", "1", null, "2"},
            {"5102", "营业外收入", "5", "1", null, "2"},
            {"5301", "主营业务成本", "5", "1", null, "1"},
            {"5302", "其他业务成本", "5", "1", null, "1"},
            {"5401", "营业税金及附加", "5", "1", null, "1"},
            {"5402", "销售费用", "5", "1", null, "1"},
            {"5403", "管理费用", "5", "1", null, "1"},
            {"5404", "财务费用", "5", "1", null, "1"},
            {"5501", "资产减值损失", "5", "1", null, "1"},
            {"5601", "营业外支出", "5", "1", null, "1"},
            {"5701", "所得税费用", "5", "1", null, "1"},
            {"5801", "以前年度损益调整", "5", "1", null, "1"}
        };
        for (String[] s : subjects) {
            AccountSubject subject = new AccountSubject();
            subject.setSubjectCode(s[0]);
            subject.setSubjectName(s[1]);
            subject.setSubjectType(Integer.parseInt(s[2]));
            subject.setLevel(Integer.parseInt(s[3]));
            subject.setBalanceDirection(Integer.parseInt(s[5]));
            subject.setLeafFlag(1);
            subject.setEnabled(1);
            subject.setAuxiliaryFlag(0);
            subject.setTenantId(tenantId);
            standardSubjects.add(subject);
        }
        return this.saveBatch(standardSubjects);
    }
}