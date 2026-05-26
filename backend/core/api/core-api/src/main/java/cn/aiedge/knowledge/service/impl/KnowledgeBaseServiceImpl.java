package cn.aiedge.knowledge.service.impl;

import cn.aiedge.knowledge.entity.*;
import cn.aiedge.knowledge.mapper.*;
import cn.aiedge.knowledge.model.*;
import cn.aiedge.knowledge.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 知识库服务实现
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setTenantId(request.getTenantId());
        kb.setVectorDimension(request.getVectorDimension());
        kb.setStatus("active");
        knowledgeBaseMapper.insert(kb);
        return kb;
    }

    @Override
    public KnowledgeBase getKnowledgeBase(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public List<KnowledgeBase> listKnowledgeBases(Long tenantId) {
        return knowledgeBaseMapper.selectList(
            new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getTenantId, tenantId)
                .eq(KnowledgeBase::getStatus, "active")
        );
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(id);
        kb.setStatus("inactive");
        knowledgeBaseMapper.updateById(kb);
    }
}
