package org.carey.travelgadget.domain.dto;

import lombok.Data;

@Data
public class ApiKeysUpdateRequest {
    /** 留空表示不修改该项 */
    private String deepseekApiKey;
    private String dashscopeApiKey;
    private String zhipuApiKey;
}
