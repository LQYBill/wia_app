# 采购境内运费

## 部署顺序

1. 已由操作者添加的 `client.small_purchase_shipping_fee_enabled` 应为可映射 Boolean 的字段（推荐 TINYINT(1)，默认 0）；`purchase_order.domestic_shipping_fee` 推荐 DECIMAL(12,2)，默认 0。历史 NULL 在代码中视为关闭或无运费。不要重复添加这两个字段。
2. 执行 `db/增量SQL/20260917_purchase_shipping_quote.sql` 创建报价表。此脚本未连接或执行到数据库。
3. 部署后端，确认现有 Redis 可用。
4. 前端接入报价及 quoteId 后，再为指定客户开启开关。收费客户的旧自助采购页面提交会得到 SHIPPING_QUOTE_REQUIRED，不能省略报价偷偷收费。

本仓库没有前端代码，以下为前端接口约定。没有启动应用连接生产库或调用真实马帮。

## 规则与快照

- 开关启用后，客户自助 SKU 采购、后台员工采购、平台订单采购、Excel 导入采购和综合发票采购都会计算境内运费；只有客户自助确认付款使用三分钟报价锁定。
- 一次提交内按马帮默认供应商名称合并 SKU 件数；1–29 件收 2 EUR，30 件及以上免费。没有采购的供应商不收费。
- 当前马帮 DTO 只提供供应商名称；使用 trim 后的名称精确分组，不做模糊匹配。稳定供应商 ID 尚未接入。
- 供应商 Redis 缓存 180 秒；新增 SKU 才补查；每批最多 50 个；单次 HTTP 使用现有 3 秒连接/10 秒读取超时，不使用后台请求的五次重试。每次报价开始新批次前检查 15 秒预算，正在进行的一次 HTTP 仍可能额外占用超时时间。
- 查询部分缺失、无供应商、临时供货商、同 SKU 返回冲突供应商、API 故障均拒绝报价，不能按免费处理。
- 报价有效 180 秒；有效期内接受当时的供应商和汇率，不在提交时再次远程查询。提交核对归属、数量、ERP 编码、客户开关、币种和商品金额。过期或变化必须重新报价并再次确认。
- 数据库对报价行加 FOR UPDATE 锁并在同一事务中生成采购单、增加运费、绑定报价。已使用报价拒绝再次提交（避免重复订单及重复扣余额）。quoteId 不是可重复扣款的凭据。
- `purchase_shipping_quote.snapshot_json` 保存 SKU 数量、ERP 编码、供应商分组、每组数量/门槛/费用、商品金额、折扣、币种和汇率；`purchase_order_id` 非空时是历史订单快照，禁止清理。以一张报价/快照表代替分别建立临时报价表和费用明细表；`groups` 中也保留 0 费用组。
- 在实际向马帮创建采购前比较所提交 SKU 的供应商和数量，变化则返回失败、交由人工处理，不自动改客户已确认费用。原有采购查询排除赠品的行为不变。
- 不与旧 shipping_fees_waiver 叠加。新报价只采用新规则；修正旧统计类重复累加费用的问题。

## 币种约定

沿用现有商品定价：商品金额、折扣及 `domestic_shipping_fee` 均以 EUR 保存。
报价的 `finalAmount` 为 EUR 应付金额，`payableAmount` 为 `currency` 对应的付款金额，保留两位小数。
通过新报价创建的采购单 `final_amount` 保存 `payableAmount`，让现有按采购单币种扣余额的流程扣除正确金额。
发票另列 China domestic shipping fee，并使用已保存的币种和汇率；不因客户后续改币种或汇率更新改变历史报价。
目前支持 EUR 和 USD。其他币种明确拒绝新报价。

## API

所有路径均需加部署的 context-path，并带现有登录 token。

### 预览

`POST /business/purchaseOrder/client/quote`

请求体（本地 SKU ID，ERP 编码由后端查询，不信任前端）：

```json
[{"id":"sku-id-1","quantity":10},{"id":"sku-id-2","quantity":19}]
```

返回标准 Result 包装，`result` 为 PurchaseShippingQuote：

- `quoteId`、`expiresAt`（epoch 毫秒）、`enabled`
- `merchandiseAmount`、`discountAmount`、`domesticShippingFee`、`finalAmount`（EUR）
- `currency`、`exchangeRate`、`payableAmount`（付款币种）
- `groups`：supplier、quantity、threshold、standardFee、fee
- `quantities`、`erpCodes`、`suppliers`：确认及订单追溯快照

数量改变防抖 300–500ms，再取报价；以请求序号忽略过期响应。请求进行中或失败时禁止付款提交，不显示为免费。页面可显示供应商分组编号代替真实供应商名称。

### 提交

如页面使用 `/business/purchaseOrder/client/add`，请求增加 quoteId：

```json
{"skuQuantity":[{"id":"sku-id-1","quantity":10},{"id":"sku-id-2","quantity":19}],"platformOrderIDList":[],"invoiceEntityId":"entity-id","quoteId":"server-quote-id"}
```

如使用现有 `makeManualSkuPurchaseInvoice` 接口，在原 ERP 编码到数量的对象增加 quoteId：

```json
{"ERP-SKU-1":10,"ERP-SKU-2":19,"invoiceEntityId":"entity-id","quoteId":"server-quote-id"}
```

保留原有成功返回结构与开票/余额流程。不要把上述两个提交接口各调用一次；沿用页面原先的一个入口。

处理 `Result.success=false`，message 前缀为 SHIPPING_QUOTE_REQUIRED / INVALID / EXPIRED / CHANGED 时重新预览并让客户确认；USED 表示已经提交，刷新订单列表，不能自动换新 quoteId 再下单。

### 查询历史费用

`GET /business/purchaseOrder/client/shippingQuote?purchaseId=...`

仅允许客户查看自己的订单。旧订单 result 为 null。采购单列表增加 domesticShippingFee；客户编辑 DTO 及客户简要信息增加 smallPurchaseShippingFeeEnabled。收费开关仅允许员工设置。

## 验证

```text
mvn -pl jeecg-module-system/jeecg-system-biz -am test -DskipTests=false -Dtest=PurchaseShippingQuoteServiceTest,MabangPurchaseSupplierServiceTest,PurchaseShippingSubmissionTest,PurchaseInvoiceShippingTest -Dsurefire.failIfNoSpecifiedTests=false
```

测试用 mock，不连接马帮/Redis/数据库，不发邮件。上线前还需在测试环境验证 SQL 建表、两个并发相同报价提交、实际马帮响应、真实 Excel 模板及前端确认流程。取消订单保留收费快照；部分退款或实际供应商变更走人工费用调整，不在这里自动补退。
