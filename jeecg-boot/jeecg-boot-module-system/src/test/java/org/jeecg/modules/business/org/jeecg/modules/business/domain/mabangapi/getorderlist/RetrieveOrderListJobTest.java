package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRequestErrorException;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.RetrieveOrderListJob;
import org.jeecg.modules.business.service.impl.PlatformOrderMabangServiceImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;


public class RetrieveOrderListJobTest {


    @Test
    void updateDateFromMabangTest() throws OrderListRequestErrorException {
        PlatformOrderMabangServiceImpl fakeService = mock(PlatformOrderMabangServiceImpl.class);

        RetrieveOrderListJob job = new RetrieveOrderListJob();
        job.setPlatformOrderMabangService(fakeService);

        job.updateMergedOrder();
    }

}
