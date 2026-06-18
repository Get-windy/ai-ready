package cn.aiedge.dev.service.impl;

import cn.aiedge.dev.mapper.DevTemplateMapper;
import cn.aiedge.dev.model.DevTemplate;
import cn.aiedge.dev.service.DevTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DevTemplateServiceImpl implements DevTemplateService {

    private final DevTemplateMapper devTemplateMapper;

    @Override
    public List<DevTemplate> getList() {
        LambdaQueryWrapper<DevTemplate> wrapper = new LambdaQueryWrapper<DevTemplate>()
                .orderByAsc(DevTemplate::getId);
        return devTemplateMapper.selectList(wrapper);
    }

    @Override
    public DevTemplate getById(Long id) {
        return devTemplateMapper.selectById(id);
    }

    @Override
    public DevTemplate create(DevTemplate template) {
        LocalDateTime now = LocalDateTime.now();
        template.setCreateTime(now);
        template.setUpdateTime(now);
        if (template.getEnabled() == null) template.setEnabled(true);
        devTemplateMapper.insert(template);
        log.info("创建模板: id={}, name={}", template.getId(), template.getName());
        return template;
    }

    @Override
    public DevTemplate update(DevTemplate template) {
        DevTemplate existing = devTemplateMapper.selectById(template.getId());
        if (existing == null) return null;
        template.setCreateTime(existing.getCreateTime());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreatedBy(existing.getCreatedBy());
        devTemplateMapper.updateById(template);
        log.info("更新模板: id={}, name={}", template.getId(), template.getName());
        return template;
    }

    @Override
    public boolean delete(Long id) {
        return devTemplateMapper.deleteById(id) > 0;
    }

    @Override
    public boolean activate(Long id) {
        DevTemplate t = devTemplateMapper.selectById(id);
        if (t == null) return false;
        t.setEnabled(true);
        t.setUpdateTime(LocalDateTime.now());
        devTemplateMapper.updateById(t);
        log.info("激活模板: id={}", id);
        return true;
    }

    @Override
    public boolean deactivate(Long id) {
        DevTemplate t = devTemplateMapper.selectById(id);
        if (t == null) return false;
        t.setEnabled(false);
        t.setUpdateTime(LocalDateTime.now());
        devTemplateMapper.updateById(t);
        log.info("停用模板: id={}", id);
        return true;
    }
}
