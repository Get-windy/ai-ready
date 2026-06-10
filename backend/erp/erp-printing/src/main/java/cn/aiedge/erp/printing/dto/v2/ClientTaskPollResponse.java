package cn.aiedge.erp.printing.dto.v2;

import lombok.Data;

import java.util.List;

@Data
public class ClientTaskPollResponse {

    private boolean hasMore;
    private List<ClientTaskItem> tasks;
}
