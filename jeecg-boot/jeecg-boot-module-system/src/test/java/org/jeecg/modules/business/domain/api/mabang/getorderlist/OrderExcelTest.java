package org.jeecg.modules.business.domain.api.mabang.getorderlist;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OrderExcelTest {

    List<Order> fakeData(){
        Order order1 = new Order();
        order1.setShopErpCode("erpcode1111");
        order1.setLogisticChannelName("channal name 联盟-南极快线");
        order1.setPlatformOrderId("orderID88888888");
        order1.setPlatformOrderNumber("orderNumbabababababa");
        order1.setErpOrderId("erpOrderId 97641");
        order1.setTrackingNumber("trackingNumber1odhoqhd");
        order1.setOrderTime("ordertime1998-4-2");
        order1.setShippingTime("shippingtime2020-89-31");
        order1.setRecipient("Qiuyi");
        order1.setCountry("France");
        order1.setPostcode("73-000");
        order1.setStatus("status3");
        order1.setIsUnion("0");


        Order order2 = new Order();
        BeanUtils.copyProperties(order1,order2);

        OrderItem item1 = new OrderItem();
        item1.setQuantity(4);
        item1.setErpCode("erp1231421jd1");
        item1.setOriginOrderId("Original dqodoqihdq");
        OrderItem item2 = new OrderItem();
        BeanUtils.copyProperties(item1,item2);
        List<OrderItem> items1 = Lists.asList(item1, new OrderItem[]{item2, item1, item2});
        order1.setOrderItems(items1);
        List<OrderItem> items2 = Lists.asList(item1, new OrderItem[]{item1, item1, item2});
        order2.setOrderItems(items2);
        List<Order> orders = new ArrayList<>();
        orders.add(order1);
        orders.add(order2);
        return orders;
    }


    @Test
    void testExport() throws IOException {

        Path out = Paths.get("src", "test", "resources", "orderExcel.xlsx");
        if (Files.notExists(out)) {
            Files.createFile(out);
        }

        OrderExcel excel = new OrderExcel(fakeData());

        excel.export(out);

    }
}
