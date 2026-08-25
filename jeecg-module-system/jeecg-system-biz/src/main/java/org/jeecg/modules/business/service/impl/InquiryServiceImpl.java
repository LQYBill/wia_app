package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.jeecg.common.system.vo.DictModel;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.Country;
import org.jeecg.modules.business.entity.Inquiry;
import org.jeecg.modules.business.mapper.CountryMapper;
import org.jeecg.modules.business.mapper.InquiryMapper;
import org.jeecg.modules.business.service.CountryService;
import org.jeecg.modules.business.service.IClientService;
import org.jeecg.modules.business.service.IInquiryService;
import org.jeecg.modules.business.vo.InquiryImportResult;
import org.jeecg.modules.system.service.ISysUserService;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InquiryServiceImpl extends ServiceImpl<InquiryMapper, Inquiry> implements IInquiryService {
    private static final String STATUS_INQUIRY = "0";
    private static final String ATTACHMENT_MANUAL_DOWNLOAD_HINT =
            "Has attachment, please log in to the system to download it manually";

    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    private IClientService clientService;
    @Autowired
    private CountryService countryService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private JeecgBaseConfig jeecgBaseConfig;

    @Override
    @Transactional
    public void createInquiry(Inquiry inquiry) {
        normalizeCountryFields(inquiry);
        validateInquiry(inquiry);
        if (StringUtils.isBlank(inquiry.getStatus())) {
            inquiry.setStatus(STATUS_INQUIRY);
        }
        save(inquiry);
    }

    @Override
    @Transactional
    public void updateInquiry(Inquiry inquiry) {
        if (StringUtils.isBlank(inquiry.getId())) {
            throw new IllegalArgumentException("id cannot be empty");
        }
        normalizeCountryFields(inquiry);
        validateInquiry(inquiry);
        updateById(inquiry);
    }

    @Override
    public Inquiry prepareInquiryForView(Inquiry inquiry) {
        if (inquiry == null) {
            return null;
        }
        inquiry.setCountryIds(splitCountryIds(inquiry.getCountryId()));
        return inquiry;
    }

    @Override
    public String resolvePrimaryCountryId(Inquiry inquiry) {
        if (inquiry == null) {
            return null;
        }
        List<String> countryIds = splitCountryIds(inquiry.getCountryId());
        return countryIds.isEmpty() ? null : countryIds.get(0);
    }

    @Override
    public String normalizeCountryValue(String countryValue) {
        if (StringUtils.isBlank(countryValue)) {
            return countryValue;
        }
        String value = countryValue.trim();
        try {
            Country byId = countryMapper.selectById(value);
            if (byId != null) return byId.getId();
            Country byName = countryMapper.findByEnName(value);
            if (byName != null) return byName.getId();
            Country byZhName = countryMapper.findByZhName(value);
            if (byZhName != null) return byZhName.getId();
            Country byCode = countryMapper.findByCode(value);
            if (byCode != null) return byCode.getId();
        } catch (Exception ignored) {
            return value;
        }
        return value;
    }

    private void validateInquiry(Inquiry inquiry) {
        if (inquiry == null) {
            throw new IllegalArgumentException("inquiry cannot be empty");
        }
        if (StringUtils.isBlank(inquiry.getLink())) {
            throw new IllegalArgumentException("link cannot be empty");
        }
        if (StringUtils.isBlank(inquiry.getCountryId())) {
            throw new IllegalArgumentException("countryId cannot be empty");
        }
        validateCommaSeparatedReference(inquiry.getCountryId(), validCountryIds(), "countryId");
        validateCommaSeparatedReference(inquiry.getSalesId(), validSalesIds(), "salesId");
        if (inquiry.getExpectedSales() == null) {
            throw new IllegalArgumentException("expectedSales cannot be empty");
        }
    }

    private Set<String> validCountryIds() {
        return countryService.findAll().stream().map(Country::getId).collect(Collectors.toSet());
    }

    private Set<String> validSalesIds() {
        return sysUserService.listSalespersonOptions().stream().map(DictModel::getValue).collect(Collectors.toSet());
    }

    private void validateCommaSeparatedReference(String commaSeparated, Set<String> validIds, String fieldLabel) {
        if (StringUtils.isBlank(commaSeparated)) {
            return;
        }
        for (String piece : commaSeparated.split(",")) {
            String trimmed = piece.trim();
            if (StringUtils.isNotBlank(trimmed) && !validIds.contains(trimmed)) {
                throw new IllegalArgumentException(fieldLabel + " value '" + trimmed + "' does not match a known record");
            }
        }
    }

    private void normalizeCountryFields(Inquiry inquiry) {
        if (inquiry == null) {
            return;
        }
        List<String> sourceValues = new ArrayList<>();
        if (inquiry.getCountryIds() != null && !inquiry.getCountryIds().isEmpty()) {
            sourceValues.addAll(inquiry.getCountryIds());
        } else if (StringUtils.isNotBlank(inquiry.getCountryId())) {
            sourceValues.addAll(splitCountryIds(inquiry.getCountryId()));
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : sourceValues) {
            String normalizedValue = normalizeCountryValue(value);
            if (StringUtils.isNotBlank(normalizedValue)) {
                normalized.add(normalizedValue);
            }
        }
        String joined = normalized.stream().collect(Collectors.joining(","));
        inquiry.setCountryId(joined);
        inquiry.setCountryIds(new ArrayList<>(normalized));
    }

    private List<String> splitCountryIds(String countryIdsValue) {
        List<String> values = new ArrayList<>();
        if (StringUtils.isBlank(countryIdsValue)) {
            return values;
        }
        for (String item : countryIdsValue.split(",")) {
            if (StringUtils.isNotBlank(item)) {
                values.add(item.trim());
            }
        }
        return values;
    }

    @Override
    public Workbook buildExportWorkbook(List<Inquiry> exportList, String exportedByName) {
        List<Inquiry> rows = new ArrayList<>();
        List<String> linkTargets = new ArrayList<>();
        for (Inquiry item : exportList) {
            if (StringUtils.isNotBlank(item.getAttachments())) {
                item.setAttachments(ATTACHMENT_MANUAL_DOWNLOAD_HINT);
            }
            List<String[]> linkEntries = parseLinkEntries(item.getLink());
            if (linkEntries.isEmpty()) {
                item.setLink("");
                linkTargets.add(null);
            } else if (linkEntries.size() == 1) {
                item.setLink(formatLinkEntry(linkEntries.get(0)));
                linkTargets.add(hyperlinkTarget(linkEntries.get(0)[1]));
            } else {
                item.setLink(linkEntries.stream().map(InquiryServiceImpl::formatLinkEntry).collect(Collectors.joining("\n\n")));
                linkTargets.add(null);
            }
            rows.add(item);
        }

        ExportParams exportParams = new ExportParams("Inquiry Report", "Exported by: " + exportedByName, "Inquiry");
        exportParams.setType(ExcelType.XSSF);
        exportParams.setImageBasePath(jeecgBaseConfig.getPath().getUpload());
        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, Inquiry.class, rows);
        applyHyperlinksToColumn(workbook, "Link (Required)", rows.size(), linkTargets);
        applyWrapTextForMultilineCells(workbook, "Link (Required)", rows);
        addDropdownValidation(workbook, rows.size());
        return workbook;
    }

    private static List<String[]> parseLinkEntries(String rawLink) {
        List<String[]> result = new ArrayList<>();
        if (StringUtils.isBlank(rawLink)) {
            return result;
        }
        String trimmed = rawLink.trim();
        if (!trimmed.startsWith("[")) {
            result.add(new String[]{"", trimmed});
            return result;
        }
        try {
            List<Map<String, Object>> entries = new ObjectMapper().readValue(trimmed, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> entry : entries) {
                String title = entry.get("title") == null ? "" : String.valueOf(entry.get("title")).trim();
                String url = entry.get("url") == null ? "" : String.valueOf(entry.get("url")).trim();
                if (StringUtils.isNotBlank(url)) {
                    result.add(new String[]{title, url});
                }
            }
        } catch (Exception e) {
            result.add(new String[]{"", trimmed});
        }
        return result;
    }

    private static String formatLinkEntry(String[] entry) {
        return StringUtils.isBlank(entry[0]) ? entry[1] : entry[0] + ": " + entry[1];
    }

    private static String hyperlinkTarget(String url) {
        return (url.startsWith("http://") || url.startsWith("https://")) ? url : null;
    }

    private static int[] findHeaderCell(Sheet sheet, String headerText) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                if (headerText.equals(cell.getStringCellValue())) {
                    return new int[]{r, cell.getColumnIndex()};
                }
            }
        }
        return null;
    }

    private static void applyHyperlinksToColumn(Workbook workbook, String headerText, int rowCount, List<String> targets) {
        if (workbook == null || workbook.getNumberOfSheets() == 0 || rowCount == 0) {
            return;
        }
        Sheet sheet = workbook.getSheetAt(0);
        int[] header = findHeaderCell(sheet, headerText);
        if (header == null) {
            return;
        }
        int headerRowIndex = header[0];
        int colIndex = header[1];
        CreationHelper helper = workbook.getCreationHelper();
        Font hyperlinkFont = workbook.createFont();
        hyperlinkFont.setUnderline(Font.U_SINGLE);
        hyperlinkFont.setColor(IndexedColors.BLUE.getIndex());
        for (int i = 0; i < rowCount; i++) {
            String url = targets.get(i);
            if (StringUtils.isBlank(url)) {
                continue;
            }
            Row row = sheet.getRow(headerRowIndex + 1 + i);
            if (row == null) {
                continue;
            }
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                continue;
            }
            Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(url);
            cell.setHyperlink(hyperlink);
            CellStyle hyperlinkStyle = workbook.createCellStyle();
            hyperlinkStyle.cloneStyleFrom(cell.getCellStyle());
            hyperlinkStyle.setFont(hyperlinkFont);
            cell.setCellStyle(hyperlinkStyle);
        }
    }

    private static void applyWrapTextForMultilineCells(Workbook workbook, String headerText, List<Inquiry> rows) {
        if (workbook == null || workbook.getNumberOfSheets() == 0 || rows.isEmpty()) {
            return;
        }
        Sheet sheet = workbook.getSheetAt(0);
        int[] header = findHeaderCell(sheet, headerText);
        if (header == null) {
            return;
        }
        int headerRowIndex = header[0];
        int colIndex = header[1];
        for (int i = 0; i < rows.size(); i++) {
            String link = rows.get(i).getLink();
            if (link == null || !link.contains("\n")) {
                continue;
            }
            Row row = sheet.getRow(headerRowIndex + 1 + i);
            if (row == null) {
                continue;
            }
            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                continue;
            }
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.cloneStyleFrom(cell.getCellStyle());
            wrapStyle.setWrapText(true);
            cell.setCellStyle(wrapStyle);
            row.setHeight((short) -1);
        }
    }

    private void addDropdownValidation(Workbook workbook, int dataRowCount) {
        if (workbook == null || workbook.getNumberOfSheets() == 0) {
            return;
        }
        Sheet sheet = workbook.getSheetAt(0);
        int headerRowIndex = -1;
        int countryColIndex = -1;
        int salesColIndex = -1;
        int clientColIndex = -1;
        int priorityModeColIndex = -1;
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (Cell cell : row) {
                String text = cell.getStringCellValue();
                if ("Country (Required)".equals(text)) {
                    headerRowIndex = r;
                    countryColIndex = cell.getColumnIndex();
                } else if ("Sales".equals(text)) {
                    headerRowIndex = r;
                    salesColIndex = cell.getColumnIndex();
                } else if ("Client".equals(text)) {
                    headerRowIndex = r;
                    clientColIndex = cell.getColumnIndex();
                } else if ("Priority Mode".equals(text)) {
                    headerRowIndex = r;
                    priorityModeColIndex = cell.getColumnIndex();
                }
            }
            if (countryColIndex >= 0 && salesColIndex >= 0 && clientColIndex >= 0 && priorityModeColIndex >= 0) {
                break;
            }
        }
        if (headerRowIndex < 0) {
            return;
        }

        LinkedHashSet<String> orderedCountryNames = new LinkedHashSet<>();
        countryService.getPopularCountries().stream()
                .map(Country::getNameEn)
                .filter(StringUtils::isNotBlank)
                .forEach(orderedCountryNames::add);
        countryService.findAll().stream()
                .map(Country::getNameEn)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .forEach(orderedCountryNames::add);
        List<String> countryNames = new ArrayList<>(orderedCountryNames);

        List<String> salesUsernames = sysUserService.listSalespersonOptions().stream()
                .map(DictModel::getText)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        List<String> clientCodes = clientService.list().stream()
                .filter(c -> "1".equals(c.getActive()))
                .map(Client::getInternalCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        List<String> priorityModes = Arrays.asList("dropShipping", "stockMode");

        if (countryNames.isEmpty() && salesUsernames.isEmpty() && clientCodes.isEmpty() && priorityModes.isEmpty()) {
            return;
        }

        Sheet dictSheet = workbook.createSheet("字典源");
        writeDictColumn(dictSheet, 0, countryNames);
        writeDictColumn(dictSheet, 1, salesUsernames);
        writeDictColumn(dictSheet, 2, clientCodes);
        writeDictColumn(dictSheet, 3, priorityModes);
        workbook.setSheetHidden(workbook.getSheetIndex(dictSheet), true);
        int lastRow = headerRowIndex + dataRowCount + 50;
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        if (countryColIndex >= 0 && !countryNames.isEmpty()) {
            addListValidation(sheet, validationHelper, dictSheet.getSheetName(), 0, countryNames.size(), countryColIndex, headerRowIndex + 1, lastRow);
        }
        if (salesColIndex >= 0 && !salesUsernames.isEmpty()) {
            addListValidation(sheet, validationHelper, dictSheet.getSheetName(), 1, salesUsernames.size(), salesColIndex, headerRowIndex + 1, lastRow);
        }
        if (clientColIndex >= 0 && !clientCodes.isEmpty()) {
            addListValidation(sheet, validationHelper, dictSheet.getSheetName(), 2, clientCodes.size(), clientColIndex, headerRowIndex + 1, lastRow);
        }
        if (priorityModeColIndex >= 0) {
            addListValidation(sheet, validationHelper, dictSheet.getSheetName(), 3, priorityModes.size(), priorityModeColIndex, headerRowIndex + 1, lastRow);
        }
    }

    private void writeDictColumn(Sheet dictSheet, int colIndex, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            Row row = dictSheet.getRow(i);
            if (row == null) {
                row = dictSheet.createRow(i);
            }
            row.createCell(colIndex).setCellValue(values.get(i));
        }
    }

    private void addListValidation(Sheet sheet, DataValidationHelper helper, String dictSheetName, int dictColIndex,
                                    int valueCount, int targetColIndex, int firstRow, int lastRow) {
        String colLetter = CellReference.convertNumToColString(dictColIndex);
        String formula = "'" + dictSheetName + "'!$" + colLetter + "$1:$" + colLetter + "$" + valueCount;
        DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
        CellRangeAddressList addressList = new CellRangeAddressList(firstRow, lastRow, targetColIndex, targetColIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(false);
        sheet.addValidationData(validation);
    }

    @Override
    public InquiryImportResult importFromExcel(byte[] fileBytes, String forcedClientId) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(2);
        params.setHeadRows(1);
        params.setNeedSave(false);

        List<Inquiry> list = ExcelImportUtil.importExcel(new ByteArrayInputStream(fileBytes), Inquiry.class, params);
        List<String> rawClientTexts = readRawColumnValues(fileBytes, "Client", params.getTitleRows(), params.getHeadRows());

        InquiryImportResult result = new InquiryImportResult();
        Set<String> seenInBatch = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            Inquiry inquiry = list.get(i);
            inquiry.setId(null);
            boolean rawClientBlank = i >= rawClientTexts.size() || StringUtils.isBlank(rawClientTexts.get(i));
            if (StringUtils.isBlank(inquiry.getClientId()) && rawClientBlank
                    && StringUtils.isBlank(inquiry.getLink()) && StringUtils.isBlank(inquiry.getCountryId())
                    && inquiry.getExpectedSales() == null) {
                continue;
            }
            if (forcedClientId != null) {
                inquiry.setClientId(forcedClientId);
            } else if (StringUtils.isBlank(inquiry.getClientId()) && i < rawClientTexts.size()) {
                String rawClientText = rawClientTexts.get(i);
                if (StringUtils.isNotBlank(rawClientText)) {
                    inquiry.setClientId(rawClientText);
                }
            }
            if (StringUtils.isNotBlank(inquiry.getAttachments())
                    && ATTACHMENT_MANUAL_DOWNLOAD_HINT.equals(inquiry.getAttachments().trim())) {
                inquiry.setAttachments(null);
            }
            inquiry.setLink(reconstructLinkFromCell(inquiry.getLink()));
            // duplicate = same Client + Link + Country
            String duplicateKey = buildDuplicateKey(inquiry.getClientId(), inquiry.getLink(), inquiry.getCountryId());
            if (!seenInBatch.add(duplicateKey) || isDuplicateInDb(inquiry)) {
                result.setSkippedDuplicates(result.getSkippedDuplicates() + 1);
                continue;
            }
            try {
                createInquiry(inquiry);
                result.setImported(result.getImported() + 1);
            } catch (Exception rowException) {
                int excelRowNumber = params.getTitleRows() + params.getHeadRows() + i + 1;
                String rawClientText = i < rawClientTexts.size() ? rawClientTexts.get(i) : null;
                log.warn("Inquiry import row {} (Excel row {}, client: {}) failed: {}", i, excelRowNumber, rawClientText, rowException.getMessage());
                result.getRowErrors().add("row " + excelRowNumber + " (client: " + StringUtils.defaultIfBlank(rawClientText, "?") + "): " + rowException.getMessage());
            }
        }
        return result;
    }

    private static String reconstructLinkFromCell(String rawLinkCell) {
        if (StringUtils.isBlank(rawLinkCell) || !rawLinkCell.contains("\n")) {
            return rawLinkCell;
        }
        List<String> lines = Arrays.stream(rawLinkCell.split("\n"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (lines.size() <= 1) {
            return lines.isEmpty() ? rawLinkCell : lines.get(0);
        }
        return buildLinkJson(lines);
    }

    private static String[] parseFormattedLinkCell(String cellText) {
        if (StringUtils.isBlank(cellText)) {
            return new String[]{"", ""};
        }
        int sepIndex = cellText.indexOf(": ");
        if (sepIndex > 0) {
            return new String[]{cellText.substring(0, sepIndex), cellText.substring(sepIndex + 2)};
        }
        return new String[]{"", cellText.trim()};
    }

    private static String buildLinkJson(List<String> formattedLinkTexts) {
        List<Map<String, String>> entries = new ArrayList<>();
        for (String text : formattedLinkTexts) {
            String[] parsed = parseFormattedLinkCell(text);
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("title", parsed[0]);
            entry.put("url", parsed[1]);
            entries.add(entry);
        }
        try {
            return new ObjectMapper().writeValueAsString(entries);
        } catch (Exception e) {
            return formattedLinkTexts.get(0);
        }
    }

    // country/sales are comma-separated multi-value fields
    private static String normalizeForDuplicateCheck(String commaSeparated) {
        if (StringUtils.isBlank(commaSeparated)) {
            return "";
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static String buildDuplicateKey(String clientId, String link, String countryId) {
        return StringUtils.defaultString(clientId).trim() + "|" + StringUtils.defaultString(link).trim()
                + "|" + normalizeForDuplicateCheck(countryId);
    }

    private boolean isDuplicateInDb(Inquiry inquiry) {
        if (StringUtils.isBlank(inquiry.getClientId()) || StringUtils.isBlank(inquiry.getLink())) {
            return false;
        }
        List<Inquiry> candidates = list(new QueryWrapper<Inquiry>()
                .eq("client_id", inquiry.getClientId())
                .eq("link", inquiry.getLink()));
        String targetCountry = normalizeForDuplicateCheck(inquiry.getCountryId());
        return candidates.stream().anyMatch(existing -> normalizeForDuplicateCheck(existing.getCountryId()).equals(targetCountry));
    }
    private List<String> readRawColumnValues(byte[] fileBytes, String headerText, int titleRows, int headRows) {
        List<String> values = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerRowIndex = titleRows + headRows - 1;
            Row headerRow = sheet.getRow(headerRowIndex);
            int colIndex = -1;
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    if (headerText.equals(cell.getStringCellValue())) {
                        colIndex = cell.getColumnIndex();
                        break;
                    }
                }
            }
            if (colIndex < 0) {
                return values;
            }
            DataFormatter formatter = new DataFormatter();
            int dataStart = titleRows + headRows;
            for (int r = dataStart; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                Cell cell = row == null ? null : row.getCell(colIndex);
                values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
            }
        } catch (Exception e) {
            log.warn("Failed to read raw Client column for prospect-client fallback", e);
        }
        return values;
    }
}
