# LC Company — Logistics API (read-only) v1.0

Read-only HTTPS JSON API giving access to the Japanese translation data shown in the sections
"Excel Traduction Japonais", "Traduction Japonais Agent" and "Excel Tableau Agent".
No endpoint can modify data, send e-mails or trigger translations.

## Base URL
`https://wqrffspljzjivbqpnfng.supabase.co/functions/v1/logistics-api`

## Authentication
Send the API key in every request:
```
x-api-key: <YOUR_KEY>
```
(`Authorization: Bearer <YOUR_KEY>` is also accepted.) Keep the key secret and server-side only.
The key can be regenerated at any time by LC Company; the old one then stops working immediately.

## Conventions
- Only `GET` is allowed (other methods → 405).
- All dates are ISO 8601 in UTC. Date filters use `YYYY-MM-DD` (UTC days).
- Lists are paginated: `limit` (default 100, max 500) and `cursor`. Repeat the call with
  `cursor=<next_cursor>` until `next_cursor` is `null`.
- Results are sorted by `created_at` then `id` ascending (stable order).
- Customer names, e-mails and addresses are never returned.

## Concepts
- **Translation line**: one personalization of one product in one order. An order with 2 products
  each having 2 engravings produces 4 lines (`item_index` × `attr_index`).
- **Session**: all lines sent on the same UTC day (`email_sent_at` date). This is what the
  Excel export groups by.
- **source**: `excel` (default) = engraving translations (Excel Traduction Japonais, Traduction
  Japonais Agent — agent corrections are already merged); `tableau` = frame/tableau translations
  (Excel Tableau Agent).

## Endpoints

### GET /health
Checks the key. → `{ "ok": true, "version": "1.0", "time": "..." }`

### GET /sessions
Lists all sent sessions (most recent first), source excel.
```json
{ "data": [ { "date": "2026-10-26", "row_count": 142, "order_count": 91, "orders": ["#95047", "..."] } ] }
```

### GET /translations
Query parameters (all optional, combinable):
| name | type | description |
|---|---|---|
| source | excel \| tableau | default excel |
| session_date | YYYY-MM-DD | lines sent that UTC day (= one session) |
| sent | true \| false | sent lines only / pending lines only |
| sent_from, sent_to | YYYY-MM-DD | send date range (inclusive) |
| created_from, created_to | YYYY-MM-DD | creation date range (inclusive) |
| order_name | string | e.g. 95721 or #95721 |
| sku | string | exact SKU |
| limit | 1-500 | page size, default 100 |
| cursor | string | value of next_cursor from previous page |

Response: `{ "data": [TranslationLine, ...], "next_cursor": "500" | null }`

### GET /orders/{order_number}
All lines of one order (e.g. `/orders/95721`). Supports `source`, `limit`, `cursor`.
404 if the order has no translation.

## TranslationLine fields
| field | type | description |
|---|---|---|
| id | string (uuid) | Unique id of the translation line |
| source | "excel" | "tableau" | excel = Excel Traduction Japonais / Traduction Japonais Agent; tableau = Excel Tableau Agent (frames) |
| order_id | string | Shopify order id |
| order_name | string | Shopify order number, e.g. "#95721" |
| line_item_id | string|null | Shopify line item id |
| item_index | integer | 0-based index of the product inside the order |
| attr_index | integer | 0-based index of the personalization inside the product (0 = first) |
| attribute_key | string|null | Personalization field name from Shopify (e.g. "Gravure au dos") |
| product_sku | string|null | Variant SKU |
| product_title | string|null | Product title |
| quantity | integer|null | Quantity |
| original_text | string | Text typed by the customer |
| translation | string|null | Final translation (Japanese, runes or hieroglyphs). Agent corrections already applied. |
| agent_override | boolean | true if the translation/fields were corrected in Traduction Japonais Agent |
| details | string|null | Extra info (options, notes) |
| has_gift_box | boolean|null | Gift box ordered (excel source only) |
| customer_country_code | string|null | ISO country code of delivery |
| sent | boolean | true once the line was validated and sent (appears in the Sent tab / an Excel session) |
| email_sent_at | string|null (ISO 8601 UTC) | Send time; its UTC date is the session date |
| created_at | string (ISO 8601) | Creation time |
| updated_at | string (ISO 8601) | Last change |
| rotate | string|null | Value for the "rotate" Excel column (couple bracelets: 情侣款为男款 for personalization 1, 情侣款为女款 for personalization 2), else null |

## Errors
`{ "error": "<code>", "message": "..." }`
| status | error | meaning |
|---|---|---|
| 400 | invalid_param | bad query parameter |
| 401 | invalid_api_key | missing or wrong key |
| 403 | api_disabled | access switched off by LC Company |
| 404 | not_found | unknown endpoint or order |
| 405 | method_not_allowed | not a GET |
| 500 | server_error | retry later |

## Examples
```bash
curl -H "x-api-key: $KEY" "https://wqrffspljzjivbqpnfng.supabase.co/functions/v1/logistics-api/sessions"
curl -H "x-api-key: $KEY" "https://wqrffspljzjivbqpnfng.supabase.co/functions/v1/logistics-api/translations?session_date=2026-10-26&limit=500"
curl -H "x-api-key: $KEY" "https://wqrffspljzjivbqpnfng.supabase.co/functions/v1/logistics-api/translations?sent=false"
curl -H "x-api-key: $KEY" "https://wqrffspljzjivbqpnfng.supabase.co/functions/v1/logistics-api/orders/95721"
```
```python
import requests
def all_lines(**params):
    url, out, cursor = "https://wqrffspljzjivbqpnfng.supabase.co/functions/v1/logistics-api/translations", [], None
    while True:
        p = {**params, "limit": 500, **({"cursor": cursor} if cursor else {})}
        r = requests.get(url, headers={"x-api-key": KEY}, params=p, timeout=60); r.raise_for_status()
        body = r.json(); out += body["data"]; cursor = body["next_cursor"]
        if not cursor: return out
rows = all_lines(session_date="2026-10-26")
```

## Building an Excel like ours
Group lines of a session by `order_name`, then `item_index`, then `attr_index`. Use `translation`
for the Japanese column and `rotate` for the rotate column.

## Good practice
Poll at most once per minute; use `sent=false` or `created_from` for incremental syncs.
