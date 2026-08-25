package org.jeecg.modules.business.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InquiryImportResult {
    private int imported;
    private int skippedDuplicates;
    private List<String> rowErrors = new ArrayList<>();
}
