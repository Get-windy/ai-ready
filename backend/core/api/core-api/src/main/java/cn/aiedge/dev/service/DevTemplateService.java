package cn.aiedge.dev.service;

import cn.aiedge.dev.model.DevTemplate;

import java.util.List;

/**
 * 开发模板服务接口
 */
public interface DevTemplateService {

    List<DevTemplate> getList();

    DevTemplate getById(Long id);

    DevTemplate create(DevTemplate template);

    DevTemplate update(DevTemplate template);

    boolean delete(Long id);

    boolean activate(Long id);

    boolean deactivate(Long id);
}
