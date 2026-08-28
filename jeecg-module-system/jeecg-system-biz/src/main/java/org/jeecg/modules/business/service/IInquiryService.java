package org.jeecg.modules.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecg.modules.business.entity.Inquiry;
import org.jeecg.modules.business.vo.InquiryImportResult;

import java.util.List;

public interface IInquiryService extends IService<Inquiry> {
    void createInquiry(Inquiry inquiry);

    void updateInquiry(Inquiry inquiry);

    Inquiry prepareInquiryForView(Inquiry inquiry);

    String resolvePrimaryCountryId(Inquiry inquiry);

    String normalizeCountryValue(String countryValue);

    Workbook buildExportWorkbook(List<Inquiry> exportList, String exportedByName, boolean isEmployee);

    InquiryImportResult importFromExcel(byte[] fileBytes, String forcedClientId) throws Exception;
}
