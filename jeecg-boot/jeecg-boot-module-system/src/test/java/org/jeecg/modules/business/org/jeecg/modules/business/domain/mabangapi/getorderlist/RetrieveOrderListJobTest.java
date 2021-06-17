package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRequestErrorException;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.RetrieveOrderListJob;
import org.jeecg.modules.business.service.impl.PlatformOrderMabangServiceImpl;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;


public class RetrieveOrderListJobTest {
    private final PlatformOrderMabangServiceImpl fakeService = mock(PlatformOrderMabangServiceImpl.class);



    @Test
    void updateNewOrderTest() throws OrderListRequestErrorException {

        RetrieveOrderListJob job = new RetrieveOrderListJob();
        job.setPlatformOrderMabangService(fakeService);

        job.updateNewOrder();
    }

    @Test
    void updateDateFromMabangTest() throws OrderListRequestErrorException {

        RetrieveOrderListJob job = new RetrieveOrderListJob();
        job.setPlatformOrderMabangService(fakeService);

        job.updateMergedOrder();
    }

}
