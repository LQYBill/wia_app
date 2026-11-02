# 威狮轨迹同步任务

任务类：`org.jeecg.modules.business.domain.job.WSJob`，与 `YDJob` 一样由项目的 Quartz 在线任务管理运行。

## 配置与启用

按当前要求，key 和 secret 固定在 `WSTrackClient` 的 `KEY` / `SECRET` 常量中，无需配置环境变量。凭据变更时需修改代码并重新部署。测试使用独立的虚构凭据。

`WEISHI_CUSTOMER_ID`（或 `weishi.customer-id`）可选，默认使用 key，已通过实际查询验证。

`weishi.track-url` 默认是文档及本次实测的测试地址 `http://120.77.151.112:16832/ws/get/track/info`；使用正式环境时应配置对方提供的正式地址。

部署后，执行同目录 `weishi_track_job.sql` 创建每小时执行一次的任务记录（可重复导入，初始为停止状态）。在“定时任务”管理中恢复该任务，系统会通过现有 Quartz 服务注册调度触发器。也可在管理页面直接新增上述任务类。仅导入 SQL 不会创建 Quartz 触发器；这里的脚本不会在代码构建时自动访问数据库。

## 范围与参数

默认通过 `platform_order.logistic_channel_name = logistic_channel.zh_name` 关联物流渠道，再通过 `logistic_channel.logistic_company_id = logistic_company.id` 确认所属物流公司。物流公司名称包含“威狮”的所有渠道，其有 `internal_tracking_number` 的订单均会拉取（不要求渠道名称包含“威狮”），默认仅查询最近 30 天（含今天）的发货日期，也包含范围内已签收订单，以便同步并中性化历史轨迹。按运单号去重并使用游标分页，逐运单请求（接口批量上限未确认），默认每次请求后间隔 200 ms。

使用 `EXISTS` 按公司关联筛选，避免多个匹配渠道造成重复结果；新增归属威狮公司的渠道会自动纳入。`transporters` 可进一步缩小该公司下的渠道范围，不能把其他公司的渠道纳入。

可选 Quartz 参数：

```json
{
  "startDate": "2026-09-01",
  "endDate": "2026-09-30",
  "pageSize": 200,
  "requestIntervalMs": 200,
  "transporters": ["实际威狮路线名称"],
  "neutralKeywords": ["额外的地名或机构名"]
}
```

日期按 `shipping_time` 筛选，含结束日当天。省略日期默认最近 30 天（今天往前 29 天至今天，包含首尾日期）。只填结束日期时取截至该日的 30 天；只填开始日期时结束日期默认为今天。显式传入首尾日期则按指定范围。参数错误会阻止运行，不会回退成意外的全量任务。

## 映射与文字规则

- `parcel.bill_code` ← 请求使用的系统内部运单号；`third_bill_code` ← 响应的 `trackingNumber`（末端派送号，允许与内部号不同；空时回退到内部号）；`order_no` ← `referenceNumber`；目的国和产品代码分别映射到 `country` / `product_code`。参考编号不能当作轨迹查询号码。
- `trackDetails` 写入 `parcel_trace`，UTC 日期转换为真正的时间点，不使用服务器本地时区解释无偏移的 UTC 字符串。
- 中文常见节点转中性英文，例如“仓库已收货”→ `Received at logistics facility`；“清关完成”→ `Customs clearance completed`。
- 英文中的中国名称、常见地名拼音、机场代码替换成 `Origin location`，`China Post` 替换为 `Postal carrier`。未知中文描述回退为 `Shipment update`，未知中文地点回退为 `Logistics facility`。其他外语和中性英文保持原样。此规则不是任意语言地名识别器，新增拼写可通过 `neutralKeywords` 补充。
- 中性化后的描述同时写入 `description` / `description_en`；时间和真实的签收、退回、失败等状态保留。仅顶层 `track_status=8` 对应的节点可继承签收状态，不会把所有历史节点标成签收。
- `trace_code` 使用原始事件时间、描述、地点和上游状态码的 MD5 前 10 位指纹，适配实表的 10 字符限制；它是本地去重标识，不代表承运商状态码。
- 包裹及其轨迹在独立事务中更新。现有轨迹先中性化，再按轨迹指纹及实表唯一键合并新节点；同一时间同一状态的不同描述以 ` | ` 合并保存，适配已有唯一索引且不丢弃不同的中性文字；重复执行不会新增同一节点。未在本次结果出现的旧历史保留。空结果、无详情、日期非法和查询号码/响应为空不会清空历史。
- 某个订单查询/保存失败会继续其余订单，最终记录计数并将本轮标记为失败，以便监控；不会记录完整接口响应或签名。

默认执行频率为每小时，可在任务管理中调整。`@DisallowConcurrentExecution` 防止同一 Quartz 任务重叠；应只注册一条 WSJob，不要另建同类任务并同时手动执行。









实表描述和地点字段最大为 256 字符；超长的中性文本保留前 255 字符并以省略号结尾，避免整单回滚。


任务启动时记录本次日期范围及符合条件的去重运单总数；每页记录本页数量，每处理 25 个运单记录累计进度、剩余量、成功/空结果/失败数量及耗时，结束或中断时记录本次汇总。总数是任务启动时按同一筛选条件统计的快照。

