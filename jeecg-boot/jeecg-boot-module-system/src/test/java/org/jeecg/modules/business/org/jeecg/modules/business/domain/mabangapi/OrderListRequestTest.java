package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.business.domain.mabangapi.DateType;
import org.jeecg.modules.business.domain.mabangapi.OrderListRequest;
import org.jeecg.modules.business.domain.mabangapi.OrderListRequestBody;
import org.jeecg.modules.business.domain.mabangapi.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;


public class OrderListRequestTest {

    @Test
    void testToReq(){
        OrderListRequestBody body = new OrderListRequestBody();
        body.setStatus(OrderStatus.Purchasing)
                .setStartDateType(DateType.UPDATE)
                .setStartDate(new Date(2020, Calendar.JULY, 1))
                .setEndDateType(DateType.UPDATE)
                .setEndDate(new Date(2020, Calendar.JULY, 2));
        ResponseEntity<String> res = OrderListRequest.sentRequest(body);
        JSONObject resBody = JSON.parseObject(res.getBody());
        assertEquals("000",resBody.get("code"));
    }
}
