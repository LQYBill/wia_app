package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.istack.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.checkerframework.checker.units.qual.C;
import org.jeecg.modules.business.entity.LogisticExpense.*;
import org.jeecg.modules.business.entity.LogisticExpenseDetail;
import org.jeecg.modules.business.mapper.LogisticExpenseDetailMapper;
import org.jeecg.modules.business.mapper.PlatformOrderMapper;
import org.jeecg.modules.business.service.ILogisticCompanyService;
import org.jeecg.modules.business.service.ILogisticExpenseDetailService;
import org.jeecg.modules.business.vo.*;
import org.jeecg.modules.business.vo.dashboard.PeriodLogisticProfit;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Description: 物流开销明细
 * @Author: jeecg-boot
 * @Date: 2021-07-22
 * @Version: V1.0
 */
@Slf4j
@Service
public class LogisticExpenseDetailServiceImpl extends ServiceImpl<LogisticExpenseDetailMapper, LogisticExpenseDetail> implements ILogisticExpenseDetailService {

    @Autowired
    private PlatformOrderMapper platformOrderMapper;
    @Autowired
    private LogisticExpenseDetailMapper logisticExpenseDetailMapper;
    @Autowired
    private ILogisticCompanyService logisticCompanyService;

    private final SimpleDateFormat CREATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


    @Override
    public PeriodLogisticProfit calculateLogisticProfitOf(Date startDate, Date endDate, List<String> country, List<String> channelName) {

        List<PlatformOrderLogisticExpenseDetail> allOrders = logisticExpenseDetailMapper.findBetween(startDate, endDate, country, channelName);

        Predicate<PlatformOrderLogisticExpenseDetail> nonInvoiced = order -> order.getShippingInvoiceNumber() == null;
        Predicate<PlatformOrderLogisticExpenseDetail> invoiced = nonInvoiced.negate();

        // actual cost of invoiced orders
        List<PlatformOrderLogisticExpenseDetail> invoicedOrders = allOrders.stream().filter(invoiced).collect(Collectors.toList());
        Map<LocalDate, Pair<BigDecimal, BigDecimal>> invoicedActualCost = calculateActualCostByDay(invoicedOrders);
        Map<LocalDate, BigDecimal> invoicedActualCostWithVat = new HashMap<>();
        Map<LocalDate, BigDecimal> invoicedActualCostWithoutVat = new HashMap<>();
        invoicedActualCost.forEach((key, value) -> {
            invoicedActualCostWithVat.put(key, value.getFirst());
            invoicedActualCostWithoutVat.put(key, value.getSecond());
        });
        // amount due of invoice
        Map<LocalDate, Pair<BigDecimal, BigDecimal>> amountDue = calculateAmountDueByDate(invoicedOrders);
        Map<LocalDate, BigDecimal> amountDueWithVat = new HashMap<>();
        Map<LocalDate, BigDecimal> amountDueWithoutVat = new HashMap<>();
        amountDue.forEach((key, value) -> {
            amountDueWithVat.put(key, value.getFirst());
            amountDueWithoutVat.put(key, value.getSecond());
        });

        // actual cost of uninvoiced orders
        List<PlatformOrderLogisticExpenseDetail> nonInvoicedOrders = allOrders.stream().filter(nonInvoiced).collect(Collectors.toList());
        Map<LocalDate, Pair<BigDecimal, BigDecimal>> nonInvoicedActualCost = calculateActualCostByDay(nonInvoicedOrders);
        Map<LocalDate, BigDecimal> nonInvoicedActualCostWithVat = new HashMap<>();
        Map<LocalDate, BigDecimal> nonInvoicedActualCostWithoutVat = new HashMap<>();
        nonInvoicedActualCost.forEach((key, value) -> {
            nonInvoicedActualCostWithVat.put(key, value.getFirst());
            nonInvoicedActualCostWithoutVat.put(key, value.getSecond());
        });

        return new PeriodLogisticProfit(
                invoicedOrders.size(),
                nonInvoicedOrders.size(),
                amountDueWithVat,
                amountDueWithoutVat,
                invoicedActualCostWithVat,
                invoicedActualCostWithoutVat,
                nonInvoicedActualCostWithVat,
                nonInvoicedActualCostWithoutVat,
                BigDecimal.valueOf(7.6)
        );
    }

    private Map<LocalDate, Pair<BigDecimal, BigDecimal>> calculateAmountDueByDate(List<PlatformOrderLogisticExpenseDetail> invoicedOrders) {
        if (invoicedOrders.isEmpty()) {
            return Collections.emptyMap();
        }

        // group by day of month
        Map<LocalDate, List<PlatformOrderLogisticExpenseDetail>> dateToOrders = invoicedOrders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getShippingTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                );


        Map<LocalDate, Pair<BigDecimal, BigDecimal>> dateToAmountDue = new HashMap<>();

        dateToOrders.forEach(
                (date, ordersByDate) -> {
                    BigDecimal dueWithVat = ordersByDate.stream()
                            .flatMap(d -> Stream.of(d.getFretFee(), d.getShippingFee(), d.getVatFee()))
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal dueWithoutVat = ordersByDate.stream()
                            .flatMap(d -> Stream.of(d.getFretFee(), d.getShippingFee()))
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dateToAmountDue.put(date, Pair.of(dueWithVat, dueWithoutVat));
                }
        );

        return dateToAmountDue;
    }

    private Map<LocalDate, Pair<BigDecimal, BigDecimal>> calculateActualCostByDay(List<PlatformOrderLogisticExpenseDetail> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        // group by day of month
        Map<LocalDate, List<PlatformOrderLogisticExpenseDetail>> dateToOrders = orders.stream()
                .collect(
                        Collectors.groupingBy(
                                order -> order.getShippingTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                );


        Map<LocalDate, Pair<BigDecimal, BigDecimal>> dateToActualCost = new HashMap<>();

        dateToOrders.forEach(
                (date, ordersByDate) -> {
                    BigDecimal costWithVat = ordersByDate.stream()
                            .map(PlatformOrderLogisticExpenseDetail::getTotal_fee)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal vat = ordersByDate.stream()
                            .flatMap(d -> Stream.of(d.getVat(), d.getVat_service_fee()))
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    dateToActualCost.put(date, Pair.of(costWithVat, costWithVat.subtract(vat)));
                }
        );

        return dateToActualCost;
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByChannel(Date startDate, Date endDate, List<String> country, List<String> channelName) {
        return expenseBy(startDate, endDate, country, channelName, PlatformOrderLogisticExpenseDetail::getLogisticChannelName);
    }

    @Override
    public List<LogisticExpenseProportion> calculateLogisticExpenseProportionByCountry(Date startDate, Date endDate, List<String> country, List<String> channelName) {
        return expenseBy(startDate, endDate, country, channelName, PlatformOrderLogisticExpenseDetail::getCountry);
    }

    private List<LogisticExpenseProportion> expenseBy(
            Date startDate,
            Date endDate,
            List<String> country,
            List<String> channelName,
            @NotNull Function<PlatformOrderLogisticExpenseDetail, String> classifier
    ) {
        // find all orders of this month
        List<PlatformOrderLogisticExpenseDetail> orders = logisticExpenseDetailMapper.findBetween(startDate, endDate, country, channelName);
        // group them by the classifier
        Map<String, List<PlatformOrderLogisticExpenseDetail>> groupedOrdersExpenseDetail = orders.stream().collect(Collectors.groupingBy(classifier));

        // define the function that reduce each part to expense proportion
        Function<Map.Entry<String, List<PlatformOrderLogisticExpenseDetail>>, LogisticExpenseProportion> ordersToExpense = (entry) -> {

            String name = entry.getKey();
            List<PlatformOrderLogisticExpenseDetail> expenseDetails = entry.getValue();

            BigDecimal expense = expenseDetails
                    .stream()
                    .map(PlatformOrderLogisticExpenseDetail::getTotal_fee)
                    .filter(Objects::nonNull)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

            return new LogisticExpenseProportion(
                    null,
                    name,
                    expense);
        };

        return groupedOrdersExpenseDetail.entrySet().stream().map(ordersToExpense).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean saveBatch(Collection<LogisticExpenseDetail> expenseDetails) {
        return this.executeBatch((sqlSession) -> logisticExpenseDetailMapper.insertOrMerge(expenseDetails));
    }

    @Override
    public Response<List<LogisticExpenseDetail>, String> importExcel(MultipartFile file, LogisticCompanyEnum logisticCompanyEnum) {
        Response<List<LogisticExpenseDetail>, String> importResponse = new Response<>();
        switch (logisticCompanyEnum) {
            case DISIFANG:

                break;
            case CNE:
                log.info("Importing CNE expense detail excel");
                String companyId = logisticCompanyService.getIdByName(LogisticCompanyEnum.ANTU.getName());
                Response<List<LogisticExpenseDetail>, String> cneExcelToLogisticResponse = new Response<>();
                cneExcelToLogisticResponse = CNEExcelToObject(file, companyId);

                if(cneExcelToLogisticResponse.getError() != null) {
                    importResponse.setError(cneExcelToLogisticResponse.getError());
                    return importResponse;
                }
                importResponse.setData(cneExcelToLogisticResponse.getData());
                break;
            case CHUKOUYI:

                break;
            case ANTU:
                log.info("Importing AnTu expense detail excel");
                Response<List<AbstractLogisticExpenseDetail>, String> antuExcelToLogisticResponse = new Response<>();
                antuExcelToLogisticResponse = antuExcelToObject(file);
                if(antuExcelToLogisticResponse.getError() != null) {
                    importResponse.setError(antuExcelToLogisticResponse.getError());
                    return importResponse;
                }
                List<AnTuExpenseDetail> antuExpenseDetails = antuExcelToLogisticResponse.getData().stream().map(AnTuExpenseDetail.class::cast).collect(Collectors.toList());
                importResponse.setData(antuToLogisticExpenseDetail(antuExpenseDetails));
                break;
            case MIAOXIN:

                break;
            case YUNTU:

                break;
            case JITU:

                break;
            case WANGUOYOULIAN:

                break;
            case WANGYISUDA:

                break;
            case YIDA:
                log.info("Importing Yida expense detail excel");
                break;
            case UBI:

                break;
            case JIEHANG:

                break;
            case WANBANG:
                log.info("Importing WanBang expense detail excel");
                break;
            case WIA:

                break;
            case CHENMINGKUNXIAOBAO:

                break;
            case CAINIAO:
                log.info("Importing CaiNiao expense detail excel");
                break;
            case SHENZHENYUANPENG:

                break;
            case WEIKELU:

                break;
            case WANTONGWULIU:

                break;
        }
        return importResponse;
    }
    @Override
    public Response<List<AbstractLogisticExpenseDetail>, String> antuExcelToObject(MultipartFile file) {
        Response<List<AbstractLogisticExpenseDetail>, String> responses = new Response<>();
        try {
            ImportParams params = new ImportParams();
            params.setTitleRows(0);
            params.setNeedSave(true);
            List<AbstractLogisticExpenseDetail> list = ExcelImportUtil.importExcel(file.getInputStream(), AnTuExpenseDetail.class, params);
            System.out.println("Details size: " + list.size());
            System.out.println("Details : " + list);
            responses.setData(list);
        } catch (Exception e) {
            responses.setError(e.getMessage());
            log.error(e.getMessage(), e);
        }
        return responses;
    }
    @Override
    public Response<List<LogisticExpenseDetail>, String> CNEExcelToObject(MultipartFile file, String companyId) {
        Response<List<LogisticExpenseDetail>, String> responses = new Response<>();
        List<LogisticExpenseDetail> detailList = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()){
            Workbook workbook = WorkbookFactory.create(inputStream);
            List<CNEExpenseDetail> expenseDetails = CNEExpenseDetailToObject(workbook);
            List<CNEExtraExpenseDetail> extraExpenseDetails  = CNEExtraExpenseDetailToObject(workbook);
            List<CNERefundDetail> refundDetails = CNERefundDetailToObject(workbook);

            detailList.addAll(CNEDetailToLogisticDetail(expenseDetails, companyId));
            detailList.addAll(CNEExtraDetailToLogisticDetail(extraExpenseDetails, companyId));
            detailList.addAll(CNERefundDetailToLogisticDetail(refundDetails, companyId));
            responses.setData(detailList);
        } catch (Exception e) {
            responses.setError(e.getMessage());
            log.error(e.getMessage(), e);
        }
        return responses;
    }

    private List<CNERefundDetail> CNERefundDetailToObject(Workbook workbook) throws ParseException {
        List<CNERefundDetail> details = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(3);
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = firstRow + 1; rowIndex < lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            CNERefundDetail detail = new CNERefundDetail();
            for( int cellIndex = 1; cellIndex < 11; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                switch (cellIndex) {
                    case 1:
                        detail.setRefundDate(CREATE_TIME_FORMAT.parse(cell.getStringCellValue()));
                        break;
                    case 5:
                        detail.setTrackingNumber(cell.getStringCellValue());
                        break;
                    case 8:
                        detail.setTotalFee(BigDecimal.valueOf(cell.getNumericCellValue()));
                        break;
                    case 9:
                        detail.setCurrency(cell.getStringCellValue());
                        break;
                    case 10:
                        if(!cell.getCellType().equals(CellType.BLANK)){
                            detail.setRemark(cell.getStringCellValue());
                        }
                        break;
                }
            }
            details.add(detail);
        }
        System.out.println("Refund detail list size: " + details.size());
        System.out.println("Refund details : " + details);
        return details;
    }

    private List<CNEExtraExpenseDetail> CNEExtraExpenseDetailToObject(Workbook workbook) throws ParseException {
        List<CNEExtraExpenseDetail> details = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(2);
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = firstRow + 1; rowIndex < lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            CNEExtraExpenseDetail detail = new CNEExtraExpenseDetail();
            for( int cellIndex = 1; cellIndex < 9; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                switch (cellIndex) {
                    case 1:
                        detail.setFeeDate(CREATE_TIME_FORMAT.parse(cell.getStringCellValue()));
                        break;
                    case 2:
                        break;
                    case 3:
                        detail.setFeeType(cell.getStringCellValue());
                        break;
                    case 4:
                        detail.setRelatedExpenseField(cell.getStringCellValue());
                        break;
                    case 5:
                        detail.setRelatedExpenseValue(cell.getStringCellValue());
                        break;
                    case 6:
                        detail.setTotalFee(BigDecimal.valueOf(cell.getNumericCellValue()));
                        break;
                    case 7:
                        detail.setCurrency(cell.getStringCellValue());
                        break;
                    case 8:
                        if(!cell.getCellType().equals(CellType.BLANK)){
                            detail.setRemark(cell.getStringCellValue());
                        }
                        break;
                }
            }
            details.add(detail);
        }
        System.out.println("Extra detail list size: " + details.size());
        System.out.println("Extra details : " + details);
        return details;
    }

    private List<CNEExpenseDetail> CNEExpenseDetailToObject(Workbook workbook) throws ParseException {
        List<CNEExpenseDetail> details = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(1);
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
//            ImportParams params = new ImportParams();
//            params.setTitleRows(0);
//            params.setSheetNum(1);
//            params.setStartSheetIndex(1);
//            params.setNeedSave(true);
//            List<AbstractLogisticExpenseDetail> detailList = ExcelImportUtil.importExcel(file.getInputStream(), CNEExpenseDetail.class, params);
//            System.out.println("list size: " + detailList.size());
//            System.out.println("objects : " + detailList);
//            params.setSheetNum(2);
//            params.setStartSheetIndex(2);
//            List<CNEExtraExpenseDetail> extraDetailLIst = ExcelImportUtil.importExcel(file.getInputStream(), CNEExtraExpenseDetail.class, params);
//            System.out.println("list size: " + extraDetailLIst.size());
//            System.out.println("objects : " + extraDetailLIst);
        // last row is the summary row
        for (int rowIndex = firstRow + 1; rowIndex < lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            CNEExpenseDetail detail = new CNEExpenseDetail();
            for( int cellIndex = 1; cellIndex < 9; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                switch (cellIndex) {
                    case 1:
                        detail.setBusinessDate(CREATE_TIME_FORMAT.parse(cell.getStringCellValue()));
                        break;
                    case 2:
                        detail.setInternalTrackingNumber(cell.getStringCellValue());
                        break;
                    case 3:
                        detail.setTrackingNumber(cell.getStringCellValue());
                        break;
                    case 4:
                        detail.setPlatformOrderId(cell.getStringCellValue());
                        break;
                    case 6:
                        detail.setTargetCountryCn(cell.getStringCellValue());
                        break;
                    case 7:
                        detail.setChargingWeight(BigDecimal.valueOf(cell.getNumericCellValue()));
                        break;
                    case 8:
                        detail.setTotalFee(BigDecimal.valueOf(cell.getNumericCellValue()));
                        break;
                }
            }
            details.add(detail);
        }
        System.out.println("list size: " + details.size());
        System.out.println("objects : " + details);
        return details;
    }

    public List<LogisticExpenseDetail> CNEDetailToLogisticDetail(List<CNEExpenseDetail> details, String companyId) {
        List<LogisticExpenseDetail> logisticExpenseDetails = new ArrayList<>();
        for(CNEExpenseDetail detail : details) {
            LogisticExpenseDetail logisticExpenseDetail = new LogisticExpenseDetail();
            logisticExpenseDetail.setPlatformOrderSerialId(detail.getPlatformOrderId());
            logisticExpenseDetail.setTrackingNumber(detail.getTrackingNumber());
            logisticExpenseDetail.setTargetCountry(detail.getTargetCountryCn());
            logisticExpenseDetail.setChargingWeight(detail.getChargingWeight());
            logisticExpenseDetail.setShippingFee(detail.getTotalFee());
            logisticExpenseDetail.setTotalFee(detail.getTotalFee());
            logisticExpenseDetail.setLogisticCompanyId(companyId);

            logisticExpenseDetails.add(logisticExpenseDetail);
        }
        return logisticExpenseDetails;
    }
    public List<LogisticExpenseDetail> CNEExtraDetailToLogisticDetail(List<CNEExtraExpenseDetail> details, String companyId) {
        List<LogisticExpenseDetail> logisticExpenseDetails = new ArrayList<>();
        for(CNEExtraExpenseDetail detail : details) {
            LogisticExpenseDetail expenseDetail = new LogisticExpenseDetail();
            expenseDetail.setDate(detail.getFeeDate());
            if(detail.getRelatedExpenseField().equals("内单号"))
                expenseDetail.setVirtualTrackingNumber(detail.getRelatedExpenseValue());
            else
                throw new RuntimeException("Unknown related expense field: " + detail.getRelatedExpenseField());
            expenseDetail.setAdditionalFee(detail.getTotalFee());
            expenseDetail.setAdditionalFeeRemark(detail.getRemark());
            expenseDetail.setTotalFee(detail.getTotalFee());
            expenseDetail.setLogisticCompanyId(companyId);

            logisticExpenseDetails.add(expenseDetail);
        }
        return logisticExpenseDetails;
    }
    public List<LogisticExpenseDetail> CNERefundDetailToLogisticDetail(List<CNERefundDetail> details, String companyId) {
        List<LogisticExpenseDetail> logisticExpenseDetails = new ArrayList<>();
        for(CNERefundDetail detail : details) {
            LogisticExpenseDetail logisticExpenseDetail = new LogisticExpenseDetail();
            logisticExpenseDetail.setDate(detail.getRefundDate());
            logisticExpenseDetail.setTrackingNumber(detail.getTrackingNumber());
            logisticExpenseDetail.setCompensation(detail.getTotalFee());
            logisticExpenseDetail.setCompensationRemark(detail.getRemark());
            logisticExpenseDetail.setTotalFee(detail.getTotalFee().negate());
            logisticExpenseDetail.setLogisticCompanyId(companyId);

            logisticExpenseDetails.add(logisticExpenseDetail);
        }
        return logisticExpenseDetails;
    }
    @Override
    public List<LogisticExpenseDetail> antuToLogisticExpenseDetail(List<AnTuExpenseDetail> antuExpenseDetails) {
        List<LogisticExpenseDetail> logisticExpenseDetails = new ArrayList<>();
        String companyId = logisticCompanyService.getIdByName(LogisticCompanyEnum.ANTU.getName());
        for (AnTuExpenseDetail antuExpenseDetail : antuExpenseDetails) {
            LogisticExpenseDetail logisticExpenseDetail = new LogisticExpenseDetail();
            logisticExpenseDetail.setPlatformOrderSerialId(antuExpenseDetail.getPlatformOrderId());
            logisticExpenseDetail.setTrackingNumber(antuExpenseDetail.getTrackingNumber());
            logisticExpenseDetail.setTargetCountry(antuExpenseDetail.getTargetCountry());
            logisticExpenseDetail.setChargingWeight(antuExpenseDetail.getChargingWeight());
            logisticExpenseDetail.setShippingFee(antuExpenseDetail.getServiceFee());
            logisticExpenseDetail.setAdditionalFee(antuExpenseDetail.getAdditionalFee());
            logisticExpenseDetail.setTotalFee(antuExpenseDetail.getTotalFee());
            logisticExpenseDetail.setLogisticCompanyId(companyId);
        }
        return logisticExpenseDetails;
    }
    @Override
    public List<String> allCountries() {
        return platformOrderMapper.allCountries();
    }

    @Override
    public List<String> allChannels() {
        return platformOrderMapper.allChannels();
    }
}
