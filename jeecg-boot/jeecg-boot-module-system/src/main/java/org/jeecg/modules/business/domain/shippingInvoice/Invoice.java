package org.jeecg.modules.business.domain.shippingInvoice;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.jeecg.modules.business.entity.Client;
import org.jeecg.modules.business.entity.PlatformOrder;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represent the invoice file needed in business process, since the generation of this file
 * need complex data, instance of class can only be created by it's factory.
 */
public class Invoice {
    private final String invoiceCode;

    private final Client customer;

    private final Date start;

    private final Date end;

    private final List<PlatformOrder> platformOrders;

    private final double exchangeRate;

    private final Date createTime = new Date();

    private BigDecimal receiveAmount;

    private final List<TableRow> rowList;

    /* constants of cell location */
    private final static String INVOICE_CODE_LOCATION = "G3";
    private final static String GENERATED_DATE_LOCATION = "G4";
    private final static String ENTITY_NAME_LOCATION = "F7";
    private final static String INVOICE_ADR_LINE1 = "F9";
    private final static String INVOICE_ADR_LINE2 = "F10";
    private final static String PHONE_LOCATION = "F12";
    private final static String MAIL_LOCATION = "F13";
    private final static String SUBJECT_LOCATION = "C16";
    private final static String[] NUM_COL = {"C", "D", "E", "F", "G", "H"};
    private final static int FIRST_ROW = 20;
    private final static int LAST_ROW = 39;
    private final static String DOLLAR_TOTAL_DUE = "H43";

    /**
     * @param invoiceCode    the code of this invoice
     * @param customer       the target customer
     * @param start          start of date range of the package
     * @param end            end of date range of the package
     * @param platformOrders a list of package whose fee must have been updated
     * @param exchangeRate   exchange rate between EU and US
     */
    Invoice(String invoiceCode, Client customer, Date start, Date end, List<PlatformOrder> platformOrders, double exchangeRate) {
        this.invoiceCode = invoiceCode;
        this.customer = customer;
        this.start = start;
        this.end = end;
        this.platformOrders = platformOrders;
        this.exchangeRate = exchangeRate;
        this.rowList = generateRows();
    }

    public Client getCustomer() {
        return customer;
    }

    private static class TableRow {
        private final Map<String, String> map = new HashMap<>();

        public void put(String col, String value) {
            map.put(col, value);
        }

        public String get(String col) {
            return map.get(col);
        }
    }

    /**
     * Generates row content based on package data, content of row is determined by business process.
     *
     * @return a list of generated row
     */
    private List<TableRow> generateRows() {

        Map<String, List<PlatformOrder>> countryPackageMap = platformOrders.stream()
                .collect(
                        Collectors.groupingBy(PlatformOrder::getCountry)
                );

        ArrayList<TableRow> rows = new ArrayList<>();
        int ref = 1;
        receiveAmount = BigDecimal.ZERO;
        for (Map.Entry<String, List<PlatformOrder>> entry : countryPackageMap.entrySet()) {
            String country = entry.getKey();
            List<PlatformOrder> packages = entry.getValue();
            BigDecimal countryAmount = packages.stream().map(PlatformOrder::totalFee).reduce(BigDecimal.ZERO, BigDecimal::add);

            TableRow r = new TableRow();
            r.put(NUM_COL[0], String.format("%05d", ref));
            r.put(NUM_COL[1], String.format("Total cost for %s", country));
            r.put(NUM_COL[2], "");
            r.put(NUM_COL[3], "" + packages.size());
            r.put(NUM_COL[4], "");
            r.put(NUM_COL[5], String.format("%.2f", countryAmount));
            rows.add(r);

            ref++;
            receiveAmount = receiveAmount.add(countryAmount);
        }

        return rows;

    }

    /**
     * Write invoice content to the template indicated by path.
     *
     * @param out the path indicating the file to write to
     */
    public void toFile(Path out, boolean useExchangeRate) {
        ExcelWriter writer = ExcelUtil.getWriter(out.toFile(), "FACTURE");

        /* on each writing to cell, its style will be erased
         * as consequence, style must be applied after content writing
         * */
        setContent(writer, useExchangeRate);
        setStyle(writer);

        writer.flush(out.toFile());
        writer.close();
    }

    /**
     * Set content of the invoice file, this methods only put prepared data to its respective location,
     * but not produce them.
     *
     * @param writer the writer indicating the invoice file
     */
    private void setContent(ExcelWriter writer, boolean useExchangeRate) {
        // invoice code
        writer.writeCellValue(INVOICE_CODE_LOCATION, invoiceCode);
        // invoice generation date
        String invoiceDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/d/yyyy"));
        writer.writeCellValue(GENERATED_DATE_LOCATION, invoiceDate);

        // customer person info
        writer.writeCellValue(ENTITY_NAME_LOCATION, customer.getInvoiceEntity());
        String address = customer.get();
        if (address.contains(":")) {
            writer.writeCellValue(INVOICE_ADR_LINE1, address.split(":")[0]);
            writer.writeCellValue(INVOICE_ADR_LINE2, address.split(":")[1]);
        } else {
            writer.writeCellValue(INVOICE_ADR_LINE1, address);
        }
        writer.writeCellValue(PHONE_LOCATION, customer.getPhone());
        writer.writeCellValue(MAIL_LOCATION, customer.getEmail());

        // subject
        String strStart = new SimpleDateFormat("dd/MM/yyyy").format(start);
        String strEnd = new SimpleDateFormat("dd/MM/yyyy").format(end);
        writer.writeCellValue(
                SUBJECT_LOCATION,
                "Subject : Shipping fees from " + strStart + " to " + strEnd
        );

        // table rows
        for (int i = 0; i < rowList.size(); i++) {
            int numLine = FIRST_ROW + i;
            TableRow curRow = rowList.get(i);
            writer.writeCellValue(NUM_COL[0] + numLine, curRow.get(NUM_COL[0]));
            writer.writeCellValue(NUM_COL[1] + numLine, curRow.get(NUM_COL[1]));
            writer.writeCellValue(NUM_COL[2] + numLine, curRow.get(NUM_COL[2]));
            writer.writeCellValue(NUM_COL[3] + numLine, Integer.parseInt(curRow.get(NUM_COL[3])));
            writer.writeCellValue(NUM_COL[4] + numLine, curRow.get(NUM_COL[4]));
            writer.writeCellValue(NUM_COL[5] + numLine, Double.parseDouble(curRow.get(NUM_COL[5])));
        }

        if (useExchangeRate) {
            writer.writeCellValue(DOLLAR_TOTAL_DUE, "=H42*" + exchangeRate);
        }

        // revaluate formulae to enable automatic calculation
        FormulaEvaluator evaluator = writer.getWorkbook().getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
    }


    /**
     * Set the style to the excel file, indicated by the writer
     *
     * @param writer the writer that indicate the excel file
     */
    private void setStyle(ExcelWriter writer) {
        Workbook workbook = writer.getWorkbook();

        // Invoice code
        applyStyle(ExcelCellStyles.invoiceCode(workbook), writer.getCell(INVOICE_CODE_LOCATION));

        // Generation date style
        applyStyle(ExcelCellStyles.generatedDate(workbook), writer.getCell(GENERATED_DATE_LOCATION));

        // name, address, telephone, mail
        Arrays.asList(ENTITY_NAME_LOCATION, INVOICE_ADR_LINE1, INVOICE_ADR_LINE2, PHONE_LOCATION, MAIL_LOCATION).
                forEach(location -> applyStyle(ExcelCellStyles.customerInfo(workbook), writer.getCell(location))
                );
        // Subject
        applyStyle(ExcelCellStyles.subject(workbook), writer.getCell(SUBJECT_LOCATION));

        // data style
        for (int i = FIRST_ROW; i <= LAST_ROW; i++) {
            for (String col : NUM_COL) {
                applyStyle(ExcelCellStyles.dataCell(workbook), writer.getCell(col + i));
            }
            for (String col : new String[]{"E", "F", "G", "H"}) {
                applyStyle(ExcelCellStyles.rightAlignmentDate(workbook), writer.getCell(col + i));
            }
        }

        applyStyle(ExcelCellStyles.totalDue(workbook), writer.getCell(DOLLAR_TOTAL_DUE));
    }

    /**
     * Shortcut to apply cell style, if cell is not null.
     *
     * @param cellStyle the cell style to apply.
     * @param cell      the cell to be applied.
     */
    private void applyStyle(CellStyle cellStyle, Cell cell) {
        if (cell != null) {
            cell.setCellStyle(cellStyle);
        }
    }

    public double getReceiveAmount() {
        return receiveAmount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Get the invoice code.
     *
     * @return The invoice code
     */
    public String getInvoiceCode() {
        return invoiceCode;
    }

    /**
     * Get packages that are invoiced.
     *
     * @return packages that are invoiced
     */
    public List<Package> getPlatformOrders() {
        return Collections.unmodifiableList(platformOrders);
    }

}
