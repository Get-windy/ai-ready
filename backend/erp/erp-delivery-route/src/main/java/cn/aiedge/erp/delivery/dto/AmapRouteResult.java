package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmapRouteResult {

    private String status;

    private String info;

    private String infocode;

    private Integer count;

    private List<AmapRoutePath> route;
}