package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi;

import org.jeecg.modules.business.domain.mabangapi.OrderListRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;


public class OrderListRequestTest {

    @Test
    void testToReq(){
        ResponseEntity<String> res = OrderListRequest.toReq();
        String expect = "ae6dbd7743e05cafb8b3b17b7f11d4379a2392a8da119acf66082a3262651f96";
        System.out.println(res);
    }
}
