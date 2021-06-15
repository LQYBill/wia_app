package org.jeecg.modules.business.org.jeecg.modules.business.domain.mabangapi.getorderlist;

import org.jeecg.modules.business.domain.mabangapi.getorderlist.OrderListRequestErrorException;
import org.jeecg.modules.business.domain.mabangapi.getorderlist.RetrieveOrderListJob;
import org.jeecg.modules.business.service.impl.PlatformOrderServiceImpl;
import org.jeecg.modules.business.service.impl.purchase.PlatformOrderContentServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;


public class RetrieveOrderListJobTest {


    @Test
    void updateDateFromMabangTest() throws OrderListRequestErrorException {
        PlatformOrderContentServiceImpl fakeContentService = mock(PlatformOrderContentServiceImpl.class);
        PlatformOrderServiceImpl fakeService = mock(PlatformOrderServiceImpl.class);

        RetrieveOrderListJob job = new RetrieveOrderListJob();
        job.setPlatformOrderContentService(fakeContentService);
        job.setPlatformOrderService(fakeService);

        job.updateMergedOrder();
    }

}
