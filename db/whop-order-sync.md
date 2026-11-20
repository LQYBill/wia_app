# Whop → 马帮订单同步

当前实现：WhopOrderSyncJob 分页拉取支付列表、逐笔读取详情、先保存到 whop_order，再转换并创建马帮订单。异步提交后为 SUBMITTED，下一轮通过 order-do-create-order-check 的 isok=1 确认后才标为 SENT。

使用 Whop Current API，固定版本 2026-11-02-1。详情 line_items 提供真实 SKU、数量和金额对象，支持多 SKU、多数量。当前版列表按创建时间筛选；任务按创建窗口增量拉取，并每轮刷新最久未更新的 100 笔旧交易，捕获迟付、退款及取消。历史量大时应提高频率或轮询批量。发送前还会获取最新详情。

复合主键 (account_id,payment_id) 去重，全部页面和本轮旧交易刷新成功后才推进检查点。窗口结束为当前时间减 30 秒，起点回看 5 分钟。失败可以重跑，不重置马帮发送状态。

## 店铺及商品

店铺 TID-Whop，账号 biz_wsbeQPs30zNAhH。

| Whop SKU | 马帮 platformSku |
| --- | --- |
| PR-AURORE | 20261023JY007-TID |
| PR-LILAS | 20261023JY003-TID |
| PR-EMERAUDE | 20261023JY001-TID |
| PR-JASMINE | 20261023JY002-TID |
| PR-ROSE | 20261023JY005-TID |
| PR-OPT-BATT | 20261026JY001-TID |
| PR-MASQUE | 20261026JY002-TID |
| PR-OR | 20261023JY004-TID |
| PR-ARIEL | 20261023JY006-TID |

仅映射中的实物行进入 orderItemList，保留每行数量。每行小计 / 数量为单价，itemTotal 保留实物小计，避免单价舍入改变总额。

用户已确认：包裹保障 PR-PROTECT / 产品 prod_HGvV1nKBGSr0Q、30 天保修产品 prod_8NhZUbgCmccYR 作为 otherIncome；快递费产品 prod_1uIxsaPwKWLHB 作为 shippingFee，不生成发货商品。订单总额与明细合计正差（如税额）计入 otherIncome，负差计入 voucherPrice，保持 itemTotal + shippingFee + otherIncome - voucherPrice = Whop total。原始明细和备注保留所有收费，此规则不替代税务科目核算。

未知商品或缺少地址、明细、数量时整单 BLOCKED，不发送半单。不用账单地址代替收货地址。实际订单 needs_tracking=false，不能据此过滤实物交易。仅处理 paid/succeeded、未退款、无已有 shipment 的交易；付款时间转为马帮 GMT+8。

## 部署

1. 执行 db/增量SQL/20261103_whop_order.sql；脚本不会自动执行。
2. 在服务器配置 whop.api-key 或环境变量 WHOP_API_KEY；密钥不放源码或 Quartz 参数。版本可通过 whop.api-version 配置，但须匹配当前字段协议。
3. Quartz 类名：org.jeecg.modules.business.domain.job.WhopOrderSyncJob，建议 cron：0 0/5 * * * ?。
4. 参数示例：

```json
{"accountId":"biz_wsbeQPs30zNAhH","startTime":"2026-11-01T00:00:00Z","pullOnly":true}
```

先用 pullOnly:true 检查落库；移除或改为 false 才会创建马帮订单。首次 startTime 应按实际回溯范围设置；后续使用数据库检查点，改参数起点不会覆盖已有进度。WhopOrderPullJob 可单独只拉取。

一个账号只创建一个 JobDetail，不同时运行只拉取和完整同步任务。DisallowConcurrentExecution 只防止同一 JobDetail 重叠。集群必须使用共享 Quartz JDBC JobStore。

马帮复用现有 Request 的账号、签名与网关配置。需要开通创建、检查接口权限，并配置对应店铺商品 SKU。

## 状态和重试

- PENDING：待处理。
- BLOCKED：校验失败，原因见 last_error，刷新数据后再尝试。
- SUBMITTED：已提交或结果未知，后续只查询，不自动重建。包括网络超时或提交前进程中断；核实马帮确实无单且无排队请求后才可人工重置为 PENDING。
- SENT：马帮检查接口已确认成功。
- REJECTED：马帮拒绝，检查 mabang_response_json，修复并确认无单后再人工重试。
- REVIEW_REQUIRED：已发送订单后来退款或取消，人工处理；任务不会自动取消马帮订单。

mabang_request_json / mabang_response_json 保存最近请求与返回，push_attempts 记录创建次数。创建仅发送一次 HTTP 请求，不盲目重试。每次创建前先查稳定的 Whop 支付 ID 是否已存在。

2026-11-03 已只读查询最近 3 笔真实支付，确认多行商品、数量、金额及收货地址。没有创建真实马帮订单，未将密钥写入仓库。单元测试模拟数据库与接口。

## 马帮物流单号回传 Whop（新增独立任务）

任务类：`org.jeecg.modules.business.domain.job.WhopShipmentSyncJob`。建议 cron：`0 0/5 * * * ?`。

先执行 `db/增量SQL/20261113_whop_shipment_sync.sql` 创建独立回传记录表。参数：

若此前已建表并出现 `Illegal mix of collations (utf8mb4_general_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT)`，在任务执行间隙运行 `db/增量SQL/20261113_whop_collation_fix.sql`，统一三个 Whop 表的字符串列为 `utf8mb4_general_ci`。此脚本保留数据，但 ALTER 可能重建或锁表。仅修改数据库/表默认排序规则或重跑 CREATE TABLE IF NOT EXISTS 不会修复已有列。查询代码也显式指定了跨表比较的排序规则，可兼容已创建的不同默认规则；不需要修改业务订单大表。

真实 MySQL 8 回归测试为 `WhopShipmentSyncRepositoryMysqlTest`。设置测试库的 `WHOP_TEST_MYSQL_URL`、`WHOP_TEST_MYSQL_USER`、`WHOP_TEST_MYSQL_PASSWORD` 后运行；未配置 URL 时跳过。测试通过专用连接的临时表复现错误 1267，验证修复后的 JOIN、HAVING、成功过滤和多单号检测，不修改持久业务表。

Druid 1.2.19 无法解析 `MAX(column) COLLATE ...`，会在进入数据库前由 WallFilter 报 `token IDENTIFIER collate`。查询使用 `MAX(column COLLATE ...)` 保留排序规则兼容性；无需关闭 WallFilter 或更改其安全规则。`WhopShipmentSyncRepositoryWallTest` 使用实际 MySQL WallProvider 验证候选查询及状态写入语句。手动验证服务器兼容性时应使用带 WallFilter 的 Druid 数据源；普通 DriverManager 直连不能覆盖这一层。

```json
{"accountId":"biz_wsbeQPs30zNAhH","shopName":"TID-Whop","batchSize":100,"dryRun":true}
```

`dryRun:true` 仅查询并记录候选数量，不写同步状态、不请求 Whop；检查后改为 `false` 执行回传。`shopName` 默认 TID-Whop，`batchSize` 默认 100，允许 1–500。每个账号只配置一个物流回传 JobDetail。它可与订单拉取/建单任务分别调度。

来源为现有马帮导入的本地 `platform_order`：店铺 `shop.erp_code=TID-Whop`、`erp_status=3`（已发货）、`tracking_number` 非空。使用马帮 `trackNumber` 对应的外部跟踪号，不使用 `internal_tracking_number`。通过 `platform_order.platform_order_id = whop_order.payment_id` 关联该 Whop 账号。需要原 Whop 拉单任务已落库，且现有马帮订单同步已将该店铺发货信息更新到本地；本任务不另行重新拉取马帮订单。

回传接口为 `POST /api/v1/shipments`，传入 `account_id`、`payment_id`、`tracking_number`；Whop 自动识别承运商，无需把马帮物流渠道中文名映射成承运商代码。复用已有 Whop 服务端密钥及固定 API 版本，密钥需要 `shipment:create` 及查询物流记录所需的读取权限。

发送前调用 `GET /api/v1/shipments/{payment_id}`：已有相同跟踪号则直接确认成功；只有明确的 404 才尝试创建。每次逻辑请求使用固定 `Idempotency-Key`，超时后的下一轮先查远端结果，处理已提交成功但本地未收到响应的情况。Whop 的 POST 幂等响应缓存为 24 小时，跨此窗口仍先查询已有物流；失败的 401/403/429 会停止当前批次并保留失败信息，等待修复配置或下轮执行。

`whop_shipment_sync` 保存请求、响应、远端 shipment ID、尝试次数和最近尝试时间。`SYNCED` 且跟踪号未改变的订单不会重复提交；`FAILED` 下次继续检查。远端已有不同跟踪号、同一支付关联多个不同跟踪号或单号明显无效时记录 `CONFLICT`，不覆盖已有远端物流，不随意选择拆单包裹中的一个。修复数据后会再次检查，失败与冲突按最近尝试时间轮转，避免固定失败订单占满批次。

本任务不更改原订单建单的 `push_status`。本次开发尚未执行真实回传，也未自动启用 Quartz 配置；上线时单独执行建表和配置任务。

- [Whop 创建物流记录](https://docs.whop.com/api-reference/shipments/create-shipment)
- [Whop 按支付 ID 查询物流记录](https://docs.whop.com/api-reference/shipments/retrieve-shipment)
- [Whop POST 幂等规则](https://docs.whop.com/developer/api/idempotency)

- [Whop 当前版列表](https://docs.whop.com/api-reference/beta/payments/list-payments)
- [Whop 当前版详情](https://docs.whop.com/api-reference/beta/payments/retrieve-payment)
- [马帮创建订单](https://gwapi.mabangerp.com/web/#/api/v2?name=创建订单&cid=43&api=order-do-create-order)
- [马帮结果检查](https://gwapi.mabangerp.com/web/#/api/v2?api=order-do-create-order-check)
