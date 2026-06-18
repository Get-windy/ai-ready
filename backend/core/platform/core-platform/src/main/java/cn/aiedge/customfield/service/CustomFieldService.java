package cn.aiedge.customfield.service;

import cn.aiedge.customfield.dto.*;
import cn.aiedge.customfield.entity.CustomField;
import cn.aiedge.customfield.entity.CustomFieldGroup;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface CustomFieldService {

    CustomField createField(CustomFieldCreateRequest request);

    CustomField updateField(Long id, CustomFieldCreateRequest request);

    CustomField getFieldById(Long id);

    List<CustomField> getFieldsByModel(String modelName);

    List<CustomField> getFieldsByModelAndGroup(String modelName, String groupCode);

    Page<CustomField> listFields(Integer page, Integer size, String modelName, String fieldType);

    void deleteField(Long id);

    void activateField(Long id);

    void deactivateField(Long id);

    List<CustomFieldGroup> getGroupsByModel(String modelName);

    CustomFieldGroup createGroup(String modelName, String groupName, String description);

    void deleteGroup(Long id);

    boolean validateFieldName(String modelName, String fieldName);

    List<CustomField> getSearchableFields(String modelName);
}