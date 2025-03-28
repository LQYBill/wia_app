package org.jeecg.modules.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.istack.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
        Response<List<AbstractLogisticExpenseDetail>, String> excelToObjectResponse = new Response<>();
        Response<List<LogisticExpenseDetail>, String> importResponse = new Response<>();
        Class<?> entityClass = LogisticExpenseDetail.class;
        switch (logisticCompanyEnum) {
            case DISIFANG:

                break;
            case CNE:
                log.info("Importing CNE expense detail excel");
                entityClass = CNEExpenseDetail.class;
                excelToObjectResponse = CNEExcelToObject(file);

                break;
            case CHUKOUYI:

                break;
            case ANTU:
                log.info("Importing AnTu expense detail excel");
                entityClass = AnTuExpenseDetail.class;
                excelToObjectResponse = antuExcelToObject(file);
                if(excelToObjectResponse.getError() != null) {
                    break;
                }
                List<AnTuExpenseDetail> antuExpenseDetails = excelToObjectResponse.getData().stream().map(AnTuExpenseDetail.class::cast).collect(Collectors.toList());
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
                entityClass = YDHExpenseDetail.class;
                break;
            case UBI:

                break;
            case JIEHANG:

                break;
            case WANBANG:
                log.info("Importing WanBang expense detail excel");
                entityClass = WanBangExpenseDetail.class;
                break;
            case WIA:

                break;
            case CHENMINGKUNXIAOBAO:

                break;
            case CAINIAO:
                log.info("Importing CaiNiao expense detail excel");
                entityClass = CaiNiaoExpenseDetail.class;
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
    public Response<List<AbstractLogisticExpenseDetail>, String> CNEExcelToObject(MultipartFile file) {
        Response<List<AbstractLogisticExpenseDetail>, String> responses = new Response<>();
        List<AbstractLogisticExpenseDetail> detailList = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()){
            Workbook workbook = WorkbookFactory.create(inputStream);
            detailList.addAll(CNEExpenseDetailToObject(workbook));
            detailList.addAll(CNEExtraExpenseDetailToObject(workbook));
            detailList.addAll(CNERefundDetailToObject(workbook));
            responses.setData(detailList);
        } catch (Exception e) {
            responses.setError(e.getMessage());
            log.error(e.getMessage(), e);
        }
        return responses;
    }

    private Collection<? extends AbstractLogisticExpenseDetail> CNERefundDetailToObject(Workbook workbook) {
        List<AbstractLogisticExpenseDetail> details = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(3);
        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();
        for (int rowIndex = firstRow + 1; rowIndex < lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            CNERefundDetail detail = new CNERefundDetail();
            for( int cellIndex = 1; cellIndex < 7; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                switch (cellIndex) {
                    case 1:
                        detail.setTrackingNumber(cell.getStringCellValue());
                        break;
                    case 2:
                        detail.setTotalFee(BigDecimal.valueOf(cell.getNumericCellValue()));
                        break;
                    case 3:
                        detail.setCurrency(cell.getStringCellValue());
                        break;
                    case 4:
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

    private Collection<? extends AbstractLogisticExpenseDetail> CNEExtraExpenseDetailToObject(Workbook workbook) {
        List<AbstractLogisticExpenseDetail> details = new ArrayList<>();
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
                        detail.setFeeDate(cell.getStringCellValue());
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

    private Collection<? extends AbstractLogisticExpenseDetail> CNEExpenseDetailToObject(Workbook workbook) {
        List<AbstractLogisticExpenseDetail> details = new ArrayList<>();
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
            for( int cellIndex = 1; cellIndex < 11; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                switch (cellIndex) {
                    case 1:
                        detail.setBusinessDate(cell.getStringCellValue());
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
                    case 5:
                        detail.setLogisticChannelName(cell.getStringCellValue());
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
        System.out.println("list size: " + details.size());
        System.out.println("objects : " + details);
        return details;
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
