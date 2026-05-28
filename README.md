# 旅游神器（Travel Gadget）

全国旅游城市 **决策助手**：选择出发地与目的地，获取 **高铁 / 飞机 / 火车** 大交通建议 + 每日行程规划（不代订票）。

技术栈：Spring Boot 3.4、Spring AI Alibaba Graph、DashScope Embedding + Redis Stack RAG、DeepSeek 生成、智谱联网搜索。

## 工作流

```text
START → RAG（城市攻略+全国交通） → 联网搜索 → DeepSeek 行程 JSON → 保存 MySQL → END
```

## 支持范围

- **目的地**（12 城）：潮汕、北京、上海、成都、西安、杭州、桂林、三亚、广州、重庆、南京、厦门（见 `cities.yml`）
- **出发地**（15 城）：北京、上海、广州、深圳等（见 `cities.yml`）
- **RAG**：`src/main/resources/rag/cities/*.md` + `national-transport-guide.md`

扩展城市：在 `cities.yml` 增加目的地，并新增 `rag/cities/{id}.md` 攻略文件，重启应用重建向量索引。

## 前置环境

1. **MySQL**：执行 `src/main/resources/db/schema.sql`（新库）；已有库执行 `db/migration-v2-national.sql`
2. **Redis Stack**：`docker run -d -p 6379:6379 redis/redis-stack-server`
3. **API Key**（任选其一）：
   - 复制项目根目录 `application-local.yml.example` 为 `application-local.yml`，填写 Key 与数据库密码
   - 设置环境变量：`DEEPSEEK_API_KEY`、`DASHSCOPE_API_KEY`、`ZHIPU_API_KEY`、`MYSQL_PASSWORD`
   - 启动后在页面 **API 配置** 填写（写入 `data/api-settings.json`，优先生效）

## 启动

```bash
cd "Travel Gadget"
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认端口 **8081**。

## 页面

| 地址 | 说明 |
|------|------|
| http://localhost:8081/ | 选择出发地/目的地，生成行程 |
| http://localhost:8081/settings | API Key 配置 |
| http://localhost:8081/trips | 我的行程 |
| http://localhost:8081/trip/{id} | 详情 + 对话改行程 |

## API 示例

```http
GET http://localhost:8081/api/trip/presets

POST http://localhost:8081/api/trip/generate
{
  "destinationId": "beijing",
  "departureCityId": "guangzhou",
  "days": 3,
  "travelers": "情侣",
  "budgetTier": "舒适",
  "theme": "历史文化",
  "customRequire": "优先高铁"
}
```

生成结果 JSON 含 `transportSuggestions`（大交通班次/时长/票价区间建议）。

### 导出与分享

```http
GET  http://localhost:8081/api/trip/{id}/export      # 下载 Markdown
POST http://localhost:8081/api/trip/{id}/share      # 生成分享链接 /s/{token}
GET  http://localhost:8081/api/share/{token}        # 只读 JSON
```

### RAG 运维（无需重启）

```http
GET  http://localhost:8081/api/system/rag/status
POST http://localhost:8081/api/system/rag/rebuild
```

配置页「API 配置」底部也可一键重建 RAG 索引。
