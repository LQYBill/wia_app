package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.Order;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderItem;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.RetrieveOrderListJob;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class RetrieveOrderListJobTest {

    @Test
    void parseDataTest() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String item1string = "{\"unitWeight\":\"5.0000\",\"platformQuantity\":\"0\",\"sellPrice\":\"0.0000\"," +
                "\"isCombo\":\"2\",\"productUnit\":\"\",\"hasGoods\":\"1\"," +
                "\"title\":\"AT 肥皂卡片 定制 Nature\",\"platformFee\":\"0.0000\"," +
                "\"noLiquidCosmetic\":\"0\",\"isRepeatItem\":\"0\",\"itemRemark\":\"\"," +
                "\"originOrderId\":\"0\",\"quantity\":\"1\",\"orderItemId\":\"30235012\"," +
                "\"pictureUrl\":\"https://stock-cos.mabangerp.com/900358/%E5%BE%AE%E4%BF%A1%E5%9B%BE%E7%89%87_20210302190905_1614683414.jpg\"," +
                "\"platformFeeOrigin\":\"0.0000\",\"costPrice\":\"0.0700\"," +
                "\"sellPriceOrigin\":\"0.0000\",\"storageSku\":\"\",\"transactionId\":\"\"," +
                "\"stockSku\":\"PJ0300CR00-AT\",\"itemId\":\"\",\"stockGrid\":\"G2-08-01\"," +
                "\"platformSku\":\"\",\"stockWarehouseId\":\"1009618\",\"stockWarehouseName\":\"SZBA 宝安仓\"," +
                "\"specifics\":\"\",\"status\":\"2\"}";
        OrderItem expectItem1 = new OrderItem();
        expectItem1.setErpCode("PJ0300CR00-AT");
        expectItem1.setQuantity(1);


        String item2string = "{\"unitWeight\":\"132.0000\",\"platformQuantity\":\"1\"," +
                "\"sellPrice\":\"97.8339\",\"isCombo\":\"2\",\"productUnit\":\"\"," +
                "\"hasGoods\":\"2\",\"title\":\"AT 米乳皂\",\"platformFee\":\"0.0000\"," +
                "\"noLiquidCosmetic\":\"0\",\"isRepeatItem\":\"0\",\"itemRemark\":\"\"," +
                "\"originOrderId\":\"21048812\",\"quantity\":\"1\",\"orderItemId\":\"30234971\"," +
                "\"pictureUrl\":\"https://cos-java-picture.mabangerp.com/58/2/1416390_1614338022579.jpg\"," +
                "\"platformFeeOrigin\":\"0.0000\",\"costPrice\":\"2.2921\",\"sellPriceOrigin\":\"12.9000\"," +
                "\"storageSku\":\"\",\"transactionId\":\"10031807168678\",\"stockSku\":\"SH05000W01-AT\"," +
                "\"itemId\":\"39412922089638\",\"stockGrid\":\"A1-07-02\",\"platformSku\":\"SH05000W01-AT\"," +
                "\"stockWarehouseId\":\"1009618\",\"stockWarehouseName\":\"SZBA 宝安仓\"," +
                "\"specifics\":\"1 soin (1 à 2 mois d'utilisation)\",\"status\":\"2\"}";
        OrderItem expectItem2 = new OrderItem();
        expectItem2.setErpCode("SH05000W01-AT");
        expectItem2.setQuantity(1);

        String orderString = "{\"paidTime\":\"2021-06-04 09:23:37\"," +
                "\"paypalEmail\":\"\"," +
                "\"refundFeeCurrencyId\":\"\"," +
                "\"isWms\":\"2\"," +
                "\"countryNameEN\":\"France\"," +
                "\"paypalFee\":\"0.0000\"," +
                "\"expressTime\":\"\"," +
                "\"itemTotal\":\"97.8339\"," +
                "\"doorcode\":\"\"," +
                "\"isNewOrder\":\"1\"," +
                "\"hasTort\":\"2\"," +
                "\"myLogisticsChannelName\":\"联邮通优先挂号-带电\"," +
                "\"city\":\"Avignon\"," +
                "\"fbaWeightBasedFee\":\"0.0000\"," +
                "\"isUnion\":\"2\"," +
                "\"orderWeight\":\"137.0000\"," +
                "\"fbaEndDateTime\":\"\"," +
                "\"transportTime\":\"2021-06-04 16:24:21\"," +
                "\"shippingWeight\":\"\"," +
                "\"countryCode\":\"FR\"," +
                "\"street1\":\"16 Route de Montfavet\"," +
                "\"sellerMessage\":\"\"," +
                "\"shippingTotalOrigin\":\"0.0000\"," +
                "\"street2\":\"\"," +
                "\"platformOrderId\":\"3880250540198\"," +
                "\"email\":\"ecaterinapopa90@gmail.com\"," +
                "\"createDate\":\"2021-06-04 16:11:45\"," +
                "\"fbaFlag\":\"1\",\"isSplit\":\"2\"," +
                "\"paypalFeeOrigin\":\"0.0000\"," +
                "\"platformFeeOrigin\":\"0.0000\"," +
                "\"VendorID\":\"\"," +
                "\"buyerName\":\"Ecaterina Popa\"," +
                "\"subsidyAmount\":\"0.0000\"," +
                "\"isReturned\":\"2\"," +
                "\"insuranceFee\":\"0.0000\"," +
                "\"isSyncLogisticsDescr\":\"获取成功\"," +
                "\"hasBattery\":\"2\"," +
                "\"CarrierCode\":\"\"," +
                "\"currencyRate\":\"7.5840240\"," +
                "\"isResend\":\"2\"," +
                "\"myLogisticsChannelId\":1034731," +
                "\"trackNumber2\":\"892115403456648\"," +
                "\"trackNumber1\":\"303669516527\"," +
                "\"phone2\":\"\"," +
                "\"platformFee\":\"0.0000\"," +
                "\"refundFeeOrigin\":\"0.0000\"," +
                "\"salesRecordNumber\":\"14887\"," +
                "\"phone1\":" +
                "\"07 66 86 34 99\"," +
                "\"hasPowder\":\"2\"," +
                "\"fbaCommission\":\"0.0000\"," +
                "\"abnnumber\":\"\"," +
                "\"ShippingChargeback\":\"0.0000\"," +
                "\"orderCost\":\"2.3621\"," +
                "\"shopId\":697339451," +
                "\"shippingCost\":\"0.0000\"," +
                "\"isSyncLogistics\":\"3\"," +
                "\"allianceFeeOrigin\":\"0.0000\"," +
                "\"subsidyAmountOrigin\":\"0.0000\"," +
                "\"companyId\":\"900358\"," +
                "\"voucherPrice\":\"0.0000\"," +
                "\"shippingFee\":\"0.0000\"," +
                "\"district\":\"\"," +
                "\"postCode\":\"84000\"," +
                "\"buyerUserId\":\"Ecaterina Popa\"," +
                "\"isVirtual\":\"2\"," +
                "\"canSend\":\"1\"," +
                "\"promotionAmount\":\"0.0000\"," +
                "\"allianceFee\":\"0.0000\"," +
                "\"orderStatus\":\"2\"," +
                "\"orderFee\":\"97.8339\"," +
                "\"shopName\":\"AT\"," +
                "\"remark\":\"\"," +
                "\"hasGoods\":\"2\"," +
                "\"fbaStartDateTime\":\"\"," +
                "\"hasMagnetic\":\"2\"," +
                "\"packageFee\":\"0.0000\"," +
                "\"itemTotalCost\":\"2.3621\"," +
                "\"insuranceFeeOrigin\":\"0.0000\"," +
                "\"CODCharge\":\"\"," +
                "\"fbaPerOrderFulfillmentFee\":\"0.0000\"," +
                "\"countryNameCN\":\"法国\"," +
                "\"platformId\":\"Shopify\"," +
                "\"shippingService\":\"Livraison suivie\"," +
                "\"closeDate\":\"\"," +
                "\"fbaPerUnitFulfillmentFee\":\"0.0000\"," +
                "\"myLogisticsId\":\"1005487\"," +
                "\"shippingPreCost\":\"0.0000\"," +
                "\"paypalId\":\"20648094662822\"," +
                "\"voucherPriceOrigin\":\"0.0000\"," +
                "\"orderType\":\"\"," +
                "\"itemTotalOrigin\":\"19.9900\"," +
                "\"quickPickTime\":\"\"," +
                "\"payType\":\"Credit Card\"," +
                "\"province\":\"\"," +
                "\"erpOrderId\":\"21048789\"," +
                "\"currencyId\":\"EUR\"," +
                "\"myLogisticsName\":\"\"," +
                "\"operTime\":\"2021-06-04 16:11:24\"," +
                "\"trackNumber\":\"\"}";

        Order expectedOrder = new Order();
        expectedOrder.setCountry("France");
        expectedOrder.setLogisticChannelName("联邮通优先挂号-带电");
        expectedOrder.setOrderTime(format.parse("2021-06-04 09:23:37"));
        expectedOrder.setShippingTime(null);
        expectedOrder.setShopErpCode("697339451");
        expectedOrder.setPlatformOrderId("3880250540198");
        expectedOrder.setPlatformOrderNumber("14887");
        expectedOrder.setRecipient("Ecaterina Popa");
        expectedOrder.setPostcode("84000");
        expectedOrder.setStatus("2");

        JSONObject item1JSON = JSON.parseObject(item1string);

        JSONObject item2JSON = JSON.parseObject(item2string);

        JSONArray orderItem = new JSONArray();
        orderItem.add(item1JSON);
        orderItem.add(item2JSON);
        JSONObject order = JSON.parseObject(orderString);
        order.put("orderItem", orderItem);

        JSONArray data = new JSONArray();
        data.add(order);


        Map<Order, List<OrderItem>> res = RetrieveOrderListJob.parseData(data);
        Order resultOrder = (Order) res.keySet().toArray()[0];
        expectedOrder.setId(resultOrder.getId());

        expectItem1.setPlatformOrderId(resultOrder.getId());
        expectItem2.setPlatformOrderId(resultOrder.getId());

        assertEquals(expectedOrder,resultOrder);
        assertEquals(res.get(resultOrder).size(), 2);
        assertTrue(res.get(resultOrder).contains(expectItem1));
        assertTrue(res.get(resultOrder).contains(expectItem2));








    }

    @Test
    void updateDateFromMabangTest() {
        RetrieveOrderListJob job = new RetrieveOrderListJob();
        job.updateNewOrder();
    }

}
