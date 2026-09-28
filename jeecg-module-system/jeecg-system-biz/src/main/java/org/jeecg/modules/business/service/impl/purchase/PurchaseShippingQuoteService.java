package org.jeecg.modules.business.service.impl.purchase;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.*;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.SkuQuantity;
import org.jeecg.modules.business.vo.clientPlatformOrder.section.OrdersStatisticData;
import org.jeecg.modules.business.vo.clientPurchaseOrder.PurchaseShippingQuote;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PurchaseShippingQuoteService {
    private final IClientService clients;
    private final ISkuService skus;
    private final ClientSkuMapper clientSkus;
    private final IPlatformOrderService platformOrders;
    private final ExchangeRatesMapper rates;
    private final MabangPurchaseSupplierService suppliers;
    private final PurchaseShippingQuoteMapper quotes;
    private final PurchaseOrderMapper purchases;
    private final ISecurityService security;

    public PurchaseShippingQuoteService(IClientService clients, ISkuService skus, ClientSkuMapper clientSkus,
            IPlatformOrderService platformOrders, ExchangeRatesMapper rates,
            MabangPurchaseSupplierService suppliers, PurchaseShippingQuoteMapper quotes,
            PurchaseOrderMapper purchases, ISecurityService security) {
        this.clients = clients; this.skus = skus; this.clientSkus = clientSkus;
        this.platformOrders = platformOrders; this.rates = rates; this.suppliers = suppliers; this.quotes = quotes;
        this.purchases = purchases; this.security = security;
    }

    public Client currentClient() throws UserException {
        Client client = clients.getCurrentClient();
        if (client == null) throw new UserException("Client login required");
        return client;
    }

    public Client purchaseClient(List<SkuQuantity> items) throws UserException {
        quantities(items);
        if (!security.checkIsEmployee()) return currentClient();
        Client client = clients.getClientBySku(items.get(0).getID());
        if (client == null) throw new UserException("No client associated with selected SKU");
        return client;
    }

    public static Map<String, Integer> quantities(List<SkuQuantity> items) throws UserException {
        if (items == null || items.isEmpty() || items.size() > 500) throw new UserException("Select between 1 and 500 SKUs");
        Map<String, Integer> result = new TreeMap<>();
        for (SkuQuantity item : items) {
            if (item == null || item.getID() == null || item.getID().trim().isEmpty()
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new UserException("SKU quantities must be positive integers");
            }
            if (result.put(item.getID(), item.getQuantity()) != null) throw new UserException("Duplicate SKU in purchase");
        }
        return result;
    }

    public Map<String, String> validate(Client client, Map<String, Integer> quantities) throws UserException {
        List<ClientSku> owned = clientSkus.selectList(new LambdaQueryWrapper<ClientSku>()
                .eq(ClientSku::getClientId, client.getId()).in(ClientSku::getSkuId, quantities.keySet()));
        Set<String> ownedIds = owned.stream().map(ClientSku::getSkuId).collect(Collectors.toSet());
        if (!ownedIds.containsAll(quantities.keySet())) throw new UserException("Some SKUs do not belong to this client");
        Map<String, String> codes = new TreeMap<>();
        for (Sku sku : skus.listByIds(quantities.keySet())) {
            if (sku.getErpCode() == null || sku.getErpCode().trim().isEmpty()) throw new UserException("SKU has no ERP code");
            codes.put(sku.getId(), sku.getErpCode());
        }
        if (codes.size() != quantities.size() || new HashSet<>(codes.values()).size() != codes.size()) {
            throw new UserException("Missing or ambiguous SKU ERP codes");
        }
        return codes;
    }

    public PurchaseShippingQuote preview(List<SkuQuantity> items) throws UserException {
        Client client = purchaseClient(items);
        PurchaseShippingQuote quote = calculate(client, items, null);
        saveQuote(client, quote, null);
        return quote;
    }

    /** Calculates the domestic fee without creating a quote or loading purchase prices. */
    public BigDecimal estimateDomesticShippingFee(Client client, List<SkuQuantity> items) throws UserException {
        return calculateDomesticShipping(client, items).getDomesticShippingFee();
    }

    /** Used by all non-interactive purchase paths, in the caller's database transaction. */
    public void applyToPurchase(Client client, List<SkuQuantity> items, List<OrderContentDetail> details, String orderId) throws UserException {
        if (!Boolean.TRUE.equals(client.getSmallPurchaseShippingFeeEnabled())) return;
        PurchaseShippingQuote quote = calculate(client, items, details);
        PurchaseOrder amounts = new PurchaseOrder();
        amounts.setId(orderId);
        amounts.setDomesticShippingFee(quote.getDomesticShippingFee());
        amounts.setFinalAmount(quote.getPayableAmount());
        if (purchases.updateById(amounts) != 1) throw new UserException("Unable to save purchase shipping fee");
        saveQuote(client, quote, orderId);
    }

    private PurchaseShippingQuote calculate(Client client, List<SkuQuantity> items, List<OrderContentDetail> suppliedDetails) throws UserException {
        PurchaseShippingQuote quote = calculateDomesticShipping(client, items);
        List<OrderContentDetail> details = suppliedDetails == null ? platformOrders.searchPurchaseOrderDetail(items) : suppliedDetails;
        if (details.size() != items.size()) throw new UserException("Some SKU prices could not be loaded");
        OrdersStatisticData totals = OrdersStatisticData.makeData(details, null);
        quote.setMerchandiseAmount(totals.getEstimatedTotalPrice());
        quote.setDiscountAmount(totals.getReducedAmount());
        quote.setFinalAmount(totals.finalAmount().add(quote.getDomesticShippingFee()));
        quote.setCurrency(client.getCurrency());
        if (!"EUR".equals(quote.getCurrency()) && !"USD".equals(quote.getCurrency())) throw new UserException("Unsupported purchase currency");
        BigDecimal rate = "EUR".equals(quote.getCurrency()) ? BigDecimal.ONE : rates.getLatestExchangeRate("EUR", "USD");
        if (rate == null || rate.signum() <= 0) throw new UserException("Exchange rate unavailable");
        quote.setExchangeRate(rate);
        quote.setPayableAmount(quote.getFinalAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP));
        quote.setQuoteId(UUID.randomUUID().toString());
        quote.setExpiresAt(System.currentTimeMillis() + 180_000);
        return quote;
    }

    private PurchaseShippingQuote calculateDomesticShipping(Client client, List<SkuQuantity> items) throws UserException {
        PurchaseShippingQuote quote = new PurchaseShippingQuote();
        quote.setQuantities(quantities(items));
        quote.setErpCodes(validate(client, quote.getQuantities()));
        quote.setEnabled(Boolean.TRUE.equals(client.getSmallPurchaseShippingFeeEnabled()));
        Map<String, String> byCode = quote.isEnabled() ? suppliers.suppliers(quote.getErpCodes().values()) : Collections.emptyMap();
        Map<String, String> byId = new TreeMap<>();
        if (quote.isEnabled()) quote.getErpCodes().forEach((id, code) -> byId.put(id, byCode.get(code)));
        quote.setSuppliers(byId);
        quote.setGroups(calculateGroups(quote.getQuantities(), byId));
        quote.setDomesticShippingFee(quote.getGroups().stream().map(PurchaseShippingQuote.Group::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return quote;
    }

    private void saveQuote(Client client, PurchaseShippingQuote quote, String orderId) {
        PurchaseShippingQuoteRecord record = new PurchaseShippingQuoteRecord();
        record.setId(quote.getQuoteId()); record.setClientId(client.getId());
        record.setSnapshotJson(JSON.toJSONString(quote)); record.setExpiresAt(new Date(quote.getExpiresAt()));
        record.setCreateTime(new Date()); record.setPurchaseOrderId(orderId); quotes.insert(record);
    }

    public static List<PurchaseShippingQuote.Group> calculateGroups(Map<String, Integer> quantities, Map<String, String> suppliers) {
        Map<String, Long> grouped = new TreeMap<>();
        suppliers.forEach((id, supplier) -> grouped.merge(supplier, quantities.get(id).longValue(), Long::sum));
        List<PurchaseShippingQuote.Group> result = new ArrayList<>();
        grouped.forEach((supplier, quantity) -> {
            PurchaseShippingQuote.Group group = new PurchaseShippingQuote.Group();
            group.setSupplier(supplier); group.setQuantity(quantity); group.setThreshold(30);
            group.setStandardFee(new BigDecimal("2.00"));
            group.setFee(quantity < 30 ? group.getStandardFee() : new BigDecimal("0.00"));
            result.add(group);
        });
        return result;
    }

    public PurchaseShippingQuote saved(String orderId) {
        PurchaseShippingQuoteRecord record = quotes.findByOrder(orderId);
        return record == null ? null : JSON.parseObject(record.getSnapshotJson(), PurchaseShippingQuote.class);
    }
}
