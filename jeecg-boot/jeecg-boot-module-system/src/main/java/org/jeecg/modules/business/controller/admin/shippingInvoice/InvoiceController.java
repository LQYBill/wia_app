package org.jeecg.modules.business.controller.admin.shippingInvoice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.business.controller.UserException;
import org.jeecg.modules.business.domain.api.mabang.getorderlist.OrderStatus;
import org.jeecg.modules.business.entity.*;
import org.jeecg.modules.business.mapper.PlatformOrderContentMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.*;
import org.jeecg.modules.business.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller for request related to shipping invoice
 */
@Api(tags = "shippingInvoice")
@RestController
@RequestMapping("/shippingInvoice")
@Slf4j
public class InvoiceController {
    @Autowired
    private IShopService shopService;
    @Autowired
    private PlatformOrderShippingInvoiceService shippingInvoiceService;
    @Autowired
    private PlatformOrderMapper platformOrderMapper;
    @Autowired
    private PlatformOrderContentMapper platformOrderContentMap;
    @Autowired
    private IShippingInvoiceService iShippingInvoiceService;
    @Autowired
    private ISavRefundService iSavRefundService;
    @Autowired
    private IExchangeRatesService iExchangeRatesService;

    @GetMapping(value = "/shopsByClient")
    public Result<List<Shop>> getShopsByClient(@RequestParam("clientID") String clientID) {
        log.info("Request for shop by client {}", clientID);
        return Result.OK(shopService.listByClient(clientID));
    }

    @PostMapping(value = "/period")
    public Result<?> getValidPeriod(@RequestBody List<String> shopIDs) {
        log.info("Request for valid period for shops: " + shopIDs.toString());
        Period period = shippingInvoiceService.getValidPeriod(shopIDs);
        if (period.isValid())
            return Result.OK(period);
        else return Result.error("No package in the selected period");
    }

    @GetMapping(value = "/preShipping/orders")
    public Result<?> getOrdersByClientAndShops(PlatformOrder platformOrder,
                                               @RequestParam("clientId") String clientId,
                                               @RequestParam(name = "shopIds[]", required = false) List<String> shopIDs,
                                               @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                               @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                               @RequestParam(name = "type") String type,
                                               HttpServletRequest req) {
        QueryWrapper<PlatformOrder> queryWrapper = QueryGenerator.initQueryWrapper(platformOrder, req.getParameterMap());
        LambdaQueryWrapper<PlatformOrder> lambdaQueryWrapper = queryWrapper.lambda();
        if(type.equals("pre-shipping"))
        {
            lambdaQueryWrapper.in(PlatformOrder::getErpStatus, Arrays.asList(OrderStatus.Pending.getCode(), OrderStatus.Preparing.getCode()));
        } else {
            lambdaQueryWrapper.in(PlatformOrder::getErpStatus, Arrays.asList(OrderStatus.Pending.getCode(), OrderStatus.Preparing.getCode(), OrderStatus.Shipped.getCode()));
        }
        lambdaQueryWrapper.isNull(PlatformOrder::getShippingInvoiceNumber);
        Page<PlatformOrder> page = new Page<>(pageNo, pageSize);
        IPage<PlatformOrder> pageList;
        log.info("Request for pre-shipping orders from client : " + clientId);
        if (shopIDs == null || shopIDs.isEmpty()) {
            lambdaQueryWrapper.inSql(PlatformOrder::getId, "SELECT po.id FROM platform_order po\n" +
                    " JOIN shop s ON po.shop_id = s.id\n" +
                    " JOIN client c ON s.owner_id = c.id WHERE c.id = '" + clientId + "'");
            pageList = platformOrderMapper.selectPage(page, lambdaQueryWrapper);
        } else {
            log.info("Specified shop IDs : " + shopIDs);
            lambdaQueryWrapper.in(PlatformOrder::getShopId, shopIDs);
            lambdaQueryWrapper.inSql(PlatformOrder::getId, "SELECT id FROM platform_order po");
            pageList = platformOrderMapper.selectPage(page, lambdaQueryWrapper);
            return Result.OK(pageList);
        }
        if (pageList.getSize() > 0) {
            return Result.OK(pageList);
        } else {
            return Result.error("No orders for selected client/shops");
        }
    }

    /**
     * Make pre-shipping invoice for specified orders
     *
     * @param param Parameters for creating a pre-shipping invoice
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/preShipping/make")
    public Result<?> makePreShippingInvoice(@RequestBody PreShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeInvoice(param);
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }

    /**
     * Make complete pre-shipping invoice (Purchase + shipping) for specified orders
     *
     * @param param Order IDs
     * @return List of SKUs whose prices are unavailable
     */
    @PostMapping(value = "/preShipping/checkSkuPrices")
    public Result<?> checkSkuPrices(@RequestBody PreShippingInvoiceParam param) {
        List<PlatformOrderContent> orderContents = platformOrderContentMap.fetchOrderContent(param.orderIds());
        Set<String> skuIds = orderContents.stream().map(PlatformOrderContent::getSkuId).collect(Collectors.toSet());
        List<String> skusWithoutPrice = platformOrderContentMap.searchSkuDetail(new ArrayList<>(skuIds))
                .stream()
                .filter(skuDetail -> skuDetail.getPrice().getPrice() == null)
                .map(SkuDetail::getErpCode)
                .collect(Collectors.toList());
        if (skusWithoutPrice.isEmpty()) {
            return Result.OK();
        }
        return Result.error("Couldn't find prices for following SKUs : " + skusWithoutPrice);
    }

    /**
     * Make complete pre-shipping invoice (Purchase + shipping) for specified orders
     *
     * @param param Parameters for creating a pre-shipping invoice
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/preShipping/makeComplete")
    public Result<?> makeCompletePreShippingInvoice(@RequestBody PreShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeCompleteInvoice(param, "pre");
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }

    /**
     * Same as makeCompletePreShippingInvoice but for post shipping
     * @param param ClientID, shopIDs[], startDate, endDate
     * @return
     */
    @PostMapping(value = "/postShipping/makeComplete")
    public Result<?> makeCompletePostShippingInvoice(@RequestBody ShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeCompleteInvoicePostShipping(param, "post");
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }

    /**
     *  Checks if all sku of order from shops between 2 dates are available
     * @param platformOrder
     * @param param clientID, shopIDs, start (date), end (date)
     * @param req
     * @return sku prices available code : 200 = success
     */
    @PostMapping(value = "/postShipping/ordersBetweenDates")
    public Result<?> getOrdersBetweenDates(PlatformOrder platformOrder, @RequestBody ShippingInvoiceParam param, HttpServletRequest req) {
        QueryWrapper<PlatformOrder> queryWrapper = QueryGenerator.initQueryWrapper(platformOrder, req.getParameterMap());
        LambdaQueryWrapper<PlatformOrder> lambdaQueryWrapper = queryWrapper.lambda();
        log.info("Requesting orders for : " + param.toString());
        if (param.shopIDs() == null || param.shopIDs().isEmpty()) {
            return Result.error("Missing shop IDs");
        } else {
            log.info("Specified shop IDs : " + param.shopIDs());
            lambdaQueryWrapper.in(PlatformOrder::getShopId, param.shopIDs());
            lambdaQueryWrapper.inSql(PlatformOrder::getId, "SELECT id FROM platform_order po WHERE po.erp_status = '3' AND po.shipping_time between '" + param.getStart() + "' AND '" + param.getEnd() + "'" );
            // on récupère les résultats de la requete
            List<PlatformOrder> orderID = platformOrderMapper.selectList(lambdaQueryWrapper);
            // on récupère seulement les ID des commandes
            List<String> orderIds = orderID.stream().map(PlatformOrder::getId).collect(Collectors.toList());
            PreShippingInvoiceParam args = new PreShippingInvoiceParam(param.clientID(), orderIds, "postShipping");
            // on check s'il y a des SKU sans prix
            return checkSkuPrices(args);
        }
    }

    /**
     * Make pre-shipping and post-shipping invoice for specified orders
     *
     * @param param Parameters for creating a pre-shipping invoice
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/allShipping/make")
    public Result<?> makeAllShippingInvoice(@RequestBody AllShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeInvoice(param);
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }
    /**
     * Make complete invoice all shipping (Purchase + shipping) for specified orders
     *
     * @param param Parameters for creating a shipping invoice
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/allShipping/makeCompleteAllInvoice")
    public Result<?> makeCompleteAllInvoice(@RequestBody AllShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeCompleteInvoiceAllShipping(param);
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }
    /**
     * Make invoice for orders indicated by param.
     *
     * @param param invoice making parameter
     * @return Result of the generation, in case of error, message will be contained,
     * in case of success, data will contain filename.
     */
    @PostMapping(value = "/make")
    public Result<?> makeInvoice(@RequestBody ShippingInvoiceParam param) {
        try {
            InvoiceMetaData metaData = shippingInvoiceService.makeInvoice(param);
            return Result.OK(metaData);
        } catch (UserException e) {
            return Result.error(e.getMessage());
        } catch (IOException | ParseException e) {
            log.error(e.getMessage());
            return Result.error("Sorry, server error, please try later");
        }
    }

    /**
     * Get binary data of the invoice file.
     *
     * @param filename name of the invoice
     * @return byte array, in case of error, an empty array will be returned.
     */
    @GetMapping(value = "/download")
    public byte[] downloadInvoice(@RequestParam("filename") String filename) {
        log.info("request for downloading shipping invoice");
        try {
            return shippingInvoiceService.getInvoiceBinary(filename);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new byte[0];
        }
    }

    @GetMapping(value = "/invoiceDetail")
    public byte[] invoiceDetail(@RequestParam("invoiceNumber") String invoiceNumber) throws IOException {
        List<FactureDetail> res = shippingInvoiceService.getInvoiceDetail(invoiceNumber);
        return shippingInvoiceService.exportToExcel(res, invoiceNumber);
    }

    @GetMapping(value = "/breakdown/byShop")
    public Result<?> getOrdersByClientAndShops() {
        List<String> errorMessages = new ArrayList<>();
        List<ShippingFeesEstimation> shippingFeesEstimation = shippingInvoiceService.getShippingFeesEstimation(errorMessages);
        if (shippingFeesEstimation.isEmpty()) {
            return Result.error("No data");
        } else {
            return Result.OK(errorMessages.toString(), shippingFeesEstimation);
        }
    }

    /**
     * Get an estimate of shipping fees for selected orders
     * @param param Parameters for creating a pre-shipping invoice
     * @return One ShippingFeesEstimation
     */
    @PostMapping(value = "/estimate")
    public Result<?> getShippingFeesEstimateByOrders(@RequestBody PreShippingInvoiceParam param) {
        List<String> errorMessages = new ArrayList<>();
        List<ShippingFeesEstimation> shippingFeesEstimations = shippingInvoiceService.getShippingFeesEstimation(param.clientID(),
                param.orderIds(), errorMessages);
        if (!errorMessages.isEmpty()){
            return Result.error(errorMessages.toString());
        } else {
            return Result.OK(errorMessages.toString(), shippingFeesEstimations);
        }
    }

    // TODO : here
    @GetMapping(value = "/checkInvoiceValidity")
    public Result<?> checkInvoiceValidity(@RequestParam("invoiceID") String invoiceID, @RequestParam("email") String email, @RequestParam("orgCode") String orgCode) {
        String invoiceNumber;
        String customerFullName;
        invoiceNumber = iShippingInvoiceService.getShippingInvoiceNumber(invoiceID);
        // if invoice exists
        if(invoiceNumber != null) {
            // if user is a customer, we check if he's the owner of the shops
            Client client = iShippingInvoiceService.getShopOwnerFromInvoiceNumber(invoiceNumber);
            customerFullName = client.fullName();
            String destEmail;
            if(orgCode.contains("A04")) {
                if(!client.getEmail().equals(email)) {
                    return Result.error("Not authorized to view this page.");
                }
                else {
                    destEmail = client.getEmail();
                }
            }
            else {
                destEmail = email;
            }
            JSONObject json = new JSONObject();
            json.put("name", customerFullName);
            json.put("email", destEmail);
            json.put("invoiceEntity", client.getInvoiceEntity());
            json.put("invoiceNumber", invoiceNumber);
            json.put("currency", client.getCurrency());
            return Result.OK(json);
        }
        return Result.error("Error 404 page not found.");
    }

    /**
     *
     * @param invoiceNumber
     * @param originalCurrency
     * @param targetCurrency
     * @return
     */
    @GetMapping(value = "/invoiceData")
    public Result<?> getInvoiceData(@RequestParam("invoiceNumber") String invoiceNumber,
                                    @RequestParam("originalCurrency") String originalCurrency,
                                    @RequestParam("targetCurrency") String targetCurrency
    ){
        InvoiceDatas invoiceDatas = new InvoiceDatas();

        ShippingInvoice invoice = iShippingInvoiceService.getShippingInvoice(invoiceNumber);
        List<PlatformOrder> platformOrderList = iShippingInvoiceService.getPlatformOrder(invoiceNumber);

        List<BigDecimal> refundList = iSavRefundService.getRefundAmount(invoiceNumber);
        Map<String, Map.Entry<Integer, BigDecimal>> feeAndQtyPerCountry = new HashMap<>(); // it maps number of order and shipping fee per country : <France,<250, 50.30>>, <UK, <10, 2.15>>
        BigDecimal serviceFee = BigDecimal.ZERO; // po.order_service_fee + poc.service_fee
        BigDecimal pickingFee = BigDecimal.ZERO;
        BigDecimal packagingMatFee = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;

        // on parcours toutes les commandes pour récupérer : country, order_service_fee, fret_fee, picking_fee
        for(PlatformOrder p : platformOrderList) {
            String country = countryNameFormatting(p.getCountry());

            BigDecimal shippingFee = p.getFretFee() == null ? BigDecimal.ZERO : p.getFretFee(); // po.fret_fee + poc.shipping_fee
            serviceFee = p.getOrderServiceFee() == null ? serviceFee : serviceFee.add(p.getOrderServiceFee()) ;
            pickingFee = p.getPickingFee() == null ? pickingFee : pickingFee.add(p.getPickingFee());
            packagingMatFee = p.getPackagingMaterialFee() == null ? packagingMatFee : packagingMatFee.add(p.getPackagingMaterialFee());
            List<PlatformOrderContent> poc = iShippingInvoiceService.getPlatformOrderContent(p.getId());
            // le contenu des commandes pour la vat, service_fee, quantity et picking_fee
            for(PlatformOrderContent pc : poc) {
                serviceFee = pc.getServiceFee() == null ? serviceFee : serviceFee.add(pc.getServiceFee());
                vat = pc.getVat() == null ? vat : vat.add(pc.getVat());
                pickingFee = pc.getPickingFee() == null ? pickingFee : pickingFee.add(pc.getPickingFee());
                shippingFee = pc.getShippingFee() == null ? shippingFee : shippingFee.add(pc.getShippingFee());
            }
            // On vérifie si on a déjà ce pays dans la map
            // si oui on additionne la "qty" et "shipping fee"
            // sinon on ajoute juste à la map
            if(!feeAndQtyPerCountry.containsKey(country)) {
                feeAndQtyPerCountry.put(country, new AbstractMap.SimpleEntry<>(1, shippingFee));
            }
            else {
                BigDecimal existingGlobalFee = feeAndQtyPerCountry.get(country).getValue();
                Integer existingOrderQuantity = feeAndQtyPerCountry.get(country).getKey();
                existingOrderQuantity ++;
                existingGlobalFee = existingGlobalFee.add(shippingFee);
                feeAndQtyPerCountry.remove(country);
                feeAndQtyPerCountry.put(country, new AbstractMap.SimpleEntry<>(existingOrderQuantity, existingGlobalFee));
            }
        }
        // on fait la somme des remboursements
        if(!refundList.isEmpty()) {
            for (BigDecimal amount : refundList) {
                refund = refund.add(amount);
            }
        }

        // si la monnaie utilisé par le client n'est pas l'euro on calcul le total dans sa monnaie
        if(!targetCurrency.equals(originalCurrency)) {
            BigDecimal exchangeRate = iExchangeRatesService.getExchangeRate(originalCurrency,targetCurrency);
            BigDecimal finalAmount = invoice.getFinalAmount().multiply(exchangeRate);
            finalAmount = finalAmount.setScale(2, RoundingMode.DOWN);
            invoiceDatas.setFinalAmount(finalAmount);
        }
        invoiceDatas.setInvoiceNumber(invoiceNumber);
        invoiceDatas.setDiscount(invoice.getDiscountAmount());
        invoiceDatas.setRefund(refund);
        invoiceDatas.setVat(vat);
        invoiceDatas.setFinalAmountEur(invoice.getFinalAmount());
        invoiceDatas.setServiceFee(serviceFee);
        invoiceDatas.setPickingFee(pickingFee);
        invoiceDatas.setPackagingMaterialFee(packagingMatFee);
        invoiceDatas.setFeeAndQtyPerCountry(feeAndQtyPerCountry);

        return Result.OK(invoiceDatas);
    }

    public String countryNameFormatting(String country) {
        Pattern p = Pattern.compile("(\\w*)", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m = p.matcher(country);
        String res = "";
        while(m.find()) {
            res = res.concat(m.group(1));
        }
        return res;
    }
}
