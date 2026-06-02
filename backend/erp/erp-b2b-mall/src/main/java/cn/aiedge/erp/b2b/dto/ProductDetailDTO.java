package cn.aiedge.erp.b2b.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailDTO extends ProductListDTO {
    private String description;
    private String categoryName;
    private List<String> images;
    private List<String> specs;
}
