package org.carey.travelgadget.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.config.DeepSeekProperties;
import org.carey.travelgadget.domain.dto.GeneratedItineraryPayload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmTripService {

    private final DynamicChatClientFactory chatClientFactory;
    private final DeepSeekProperties deepSeekProperties;
    private final ObjectMapper objectMapper;

    public GeneratedItineraryPayload generateItinerary(int days,
                                                       String travelers,
                                                       String budgetTier,
                                                       String theme,
                                                       String customRequire,
                                                       String departureCity,
                                                       String destinationName,
                                                       String arrivalLabel,
                                                       String cities,
                                                       String localTransportNote,
                                                       String departureDate,
                                                       String transportPreferenceLabel,
                                                       String ragContext,
                                                       String webContext) {
        String dateLine = departureDate != null && !departureDate.isBlank()
                ? departureDate
                : "未指定（按近期平日估算，节假日需自行考虑）";
        String prompt = """
                你是全国自由行决策助手。用户计划：
                - 出发地：%s
                - 目的地：%s（覆盖：%s）
                - 计划出发日：%s
                - 大交通偏好：%s
                - 抵达枢纽：%s
                - 行程天数：%d 天
                - 同行人：%s
                - 预算档：%s
                - 主题偏好：%s
                - 自定义要求：%s
                - 当地交通说明：%s
                
                【本地知识库参考】
                %s
                
                【联网实时参考】
                %s
                
                请生成结构化行程 JSON（不要 markdown 代码块），要求：
                1. **transportSuggestions** 必须包含 2～4 条大交通建议；**优先满足用户「大交通偏好」**，并给出 1 条标为「推荐」的方案
                2. 结合计划出发日考虑季节、节假日人流、天气；D1 扣除大交通耗时后再排景点
                3. 每日主要 POI 不超过 2 个，结合当地交通方式（地铁/打车/步行）
                4. 门票、开放时间、天气等不确定信息写入 warnings，提示用户出行前核对官方信息
                5. budget 给出人均每日与全程区间（人民币），注明是否含往返大交通
                
                JSON 格式：
                {
                  "title": "行程标题",
                  "summary": "一句话摘要",
                  "departureCity": "%s",
                  "destinationName": "%s",
                  "arrivalHub": "枢纽代码或站名",
                  "transportNote": "抵达后的市内交通说明",
                  "transportSuggestions": [
                    {
                      "mode": "高铁",
                      "route": "出发站-到达站",
                      "duration": "约X小时",
                      "scheduleHint": "班次说明",
                      "priceHint": "二等座约¥X-Y",
                      "priority": "推荐",
                      "note": "补充说明"
                    }
                  ],
                  "days": [
                    {
                      "dayIndex": 1,
                      "city": "城市名",
                      "theme": "当日主题",
                      "slots": [
                        {
                          "period": "上午",
                          "poiName": "景点名",
                          "activity": "活动描述",
                          "durationMinutes": 120,
                          "transport": "交通方式",
                          "tips": "提示"
                        }
                      ],
                      "transportBetween": "当日交通",
                      "accommodationArea": "建议住宿区域"
                    }
                  ],
                  "foodRecommendations": ["特色美食"],
                  "warnings": ["需用户自行核对的信息"],
                  "budget": {
                    "tier": "%s",
                    "perPersonPerDayMin": 200,
                    "perPersonPerDayMax": 400,
                    "totalMin": 600,
                    "totalMax": 1200,
                    "note": "费用说明"
                  }
                }
                """.formatted(
                departureCity,
                destinationName,
                cities,
                dateLine,
                transportPreferenceLabel != null && !transportPreferenceLabel.isBlank()
                        ? transportPreferenceLabel : "未指定",
                arrivalLabel,
                days,
                travelers,
                budgetTier,
                theme != null && !theme.isBlank() ? theme : "综合",
                customRequire != null && !customRequire.isBlank() ? customRequire : "无",
                localTransportNote != null && !localTransportNote.isBlank() ? localTransportNote : "无",
                ragContext != null && !ragContext.isBlank() ? ragContext : "无",
                webContext != null && !webContext.isBlank() ? webContext : "无",
                departureCity,
                destinationName,
                budgetTier
        );

        log.info("调用 DeepSeek 生成行程，目的地={}，出发地={}", destinationName, departureCity);
        String response = chatClientFactory.getClient().prompt()
                .user(prompt)
                .call()
                .content();
        return parseItineraryJson(response);
    }

    public GeneratedItineraryPayload refineItinerary(GeneratedItineraryPayload current,
                                                       int tripDays,
                                                       String destinationName,
                                                       String departureCity,
                                                       String userMessage,
                                                       String ragContext) {
        String currentJson;
        try {
            currentJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(current);
        } catch (Exception e) {
            throw new IllegalStateException("序列化当前行程失败", e);
        }

        String prompt = """
                你是自由行行程决策助手。用户在已有行程基础上提出修改，请输出**完整**更新后的行程 JSON（不要 markdown）。
                
                【约束】
                - 出发地仍为：%s；目的地仍为：%s
                - 保持原行程天数不变（%d 天）
                - 保留 transportSuggestions 大交通建议（可按用户要求调整）
                - 每日主要 POI 不宜过多
                
                【用户修改要求】
                %s
                
                【本地知识参考】
                %s
                
                【当前行程 JSON】
                %s
                
                返回与生成时相同结构的 JSON。
                """.formatted(
                departureCity != null ? departureCity : "未知",
                destinationName != null ? destinationName : "未知",
                tripDays,
                userMessage,
                ragContext != null && !ragContext.isBlank() ? ragContext : "无",
                currentJson
        );

        log.info("调用 DeepSeek 调整行程");
        String response = chatClientFactory.getClient().prompt()
                .user(prompt)
                .call()
                .content();
        return parseItineraryJson(response);
    }

    private GeneratedItineraryPayload parseItineraryJson(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, GeneratedItineraryPayload.class);
        } catch (Exception e) {
            log.error("解析行程 JSON 失败: {}", response, e);
            throw new IllegalStateException("大模型返回格式异常，请重试");
        }
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
