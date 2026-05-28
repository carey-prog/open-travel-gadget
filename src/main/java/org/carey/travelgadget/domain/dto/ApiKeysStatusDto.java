package org.carey.travelgadget.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiKeysStatusDto {
    private boolean deepseekConfigured;
    private String deepseekMasked;
    private boolean dashscopeConfigured;
    private String dashscopeMasked;
    private boolean zhipuConfigured;
    private String zhipuMasked;
    private boolean ragEnabled;
    private boolean ragKnowledgeLoaded;
    private String settingsFile;
    private String note;
}
