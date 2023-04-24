package org.jeecg.modules.business.domain.invoice;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.jeecg.modules.business.entity.Client;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractInvoice<E, F, G, H, I> {
    protected final Client targetClient;

    private final String code;

    private final String subject;

    protected ExcelWriter writer;

    private final BigDecimal exchangeRate;

    public AbstractInvoice(Client targetClient,
                           String code, String subject, BigDecimal exchangeRate) {
        this.targetClient = targetClient;
        this.code = code;
        this.subject = subject;
        this.exchangeRate = exchangeRate;
    }

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
    private final static int LAST_ROW = 42;
    private int TOTAL_ROW = LAST_ROW + 1;
    // the max number of rows in A4 page
    private final static int PAGE_ROW_MAX = 63;

    /**
     * Export content to the excel file; indicated by path out.
     *
     * @param out the excel file to export, extension: "xlsx"
     */
    public void toExcelFile(Path out) {
        this.writer = ExcelUtil.getWriter(out.toFile(), "FACTURE");
        InvoiceStyleFactory factory = new InvoiceStyleFactory(writer.getWorkbook());

        fillInformation(factory);

        fillTable(factory);

        // revaluate formulae to enable automatic calculation
        FormulaEvaluator evaluator = writer.getWorkbook().getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        writer.flush();
        writer.close();
    }

    public Client client() {
        return targetClient;
    }

    public String code() {
        return code;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    /**
     * Methods to set content of table, left child to implement.
     *
     * @return List of rows
     */
    protected abstract List<Row<E, F, G, H, I>> tableData();

    /**
     * Fill content that does not belong to the table section.
     *
     * @param factory factory of style
     */
    protected void fillInformation(InvoiceStyleFactory factory) {
        // Set each cell's content
        Map<String, Object> cellContentMap = new HashMap<>();
        cellContentMap.put(ENTITY_NAME_LOCATION, targetClient.getInvoiceEntity());
        cellContentMap.put(
                INVOICE_ADR_LINE1,
                String.format("%s %s, %s",
                        targetClient.getStreetNumber(),
                        targetClient.getStreetName(),
                        targetClient.getPostcode()
                )
        );
        cellContentMap.put(
                INVOICE_ADR_LINE2,
                String.format("%s, %s",
                        targetClient.getCity(),
                        targetClient.getCountry()
                )
        );
        cellContentMap.put(PHONE_LOCATION, targetClient.getPhone());
        cellContentMap.put(MAIL_LOCATION, targetClient.getEmail());
        // write content to cell and apply style
        for (Map.Entry<String, Object> entry : cellContentMap.entrySet()) {
            configCell(entry.getKey(), entry.getValue(), factory.otherStyle());
        }
        // config particulier style and content
        configCell(INVOICE_CODE_LOCATION, code, factory.invoiceCodeStyle());
        configCell(
                GENERATED_DATE_LOCATION,
                InvoiceStyleFactory.INVOICE_CODE_DATETIME_FORMAT.format(new Date()),
                factory.otherStyle()
        );
        configCell(SUBJECT_LOCATION, subject, factory.subjectStyle());
    }

    /**
     * Fill content of the table in invoice, extract data from {@code tableData} method.
     *
     * @param factory factory of style
     */
    protected void fillTable(InvoiceStyleFactory factory) {
        List<Row<E, F, G, H, I>> data = tableData();
        int lineNum;
        Row<E, F, G, H, I> rowValue;

        // if the number of rows of data is greater than the available space in the default template
        // we shift down the rows after the table and we clone the row in the table
        int dataRowNumber = LAST_ROW - FIRST_ROW;
        int additionalRowNum = data.size() - dataRowNumber - 1;

        Sheet sheet = factory.getWorkbook().getSheetAt(0);
        org.apache.poi.ss.usermodel.Row sourceRow = sheet.getRow(FIRST_ROW);
        if(data.size() > dataRowNumber)
        {
            TOTAL_ROW = LAST_ROW+additionalRowNum;
            int startRow = LAST_ROW+1;
            int fileLastRow = sheet.getLastRowNum();
            // shifting the footer of the file, to X rows below
            // making sure the whole footer is in the same page (13 lines) and we fill the end of page with blank data lines
            if(additionalRowNum%PAGE_ROW_MAX <= 13) {
                TOTAL_ROW = PAGE_ROW_MAX-2;
                sheet.shiftRows(startRow, fileLastRow, PAGE_ROW_MAX - LAST_ROW, true, false);
            }
            else {
                // +6 because if we use US template there's one more row
                sheet.shiftRows(startRow, fileLastRow, additionalRowNum, true, false);
            }

            // inserting new rows after row 42
            for(int i = 0; i < (data.size() - dataRowNumber - 1 <= 13 ? PAGE_ROW_MAX-LAST_ROW+1 : additionalRowNum+1); i++) {
                sheet.createRow(startRow-1 + i);
                org.apache.poi.ss.usermodel.Row newRow = sheet.getRow(startRow-1 + i);
                newRow.setHeight(sourceRow.getHeight());
                newRow.setRowStyle(null);
                newRow.setRowStyle(sourceRow.getRowStyle());

                for(int j = 0; j < 10; j++) {
                    Cell cell = newRow.createCell(j);
                    CellStyle cellStyle = factory.getWorkbook().createCellStyle();
                    CellStyle middleCellStyle = factory.getWorkbook().createCellStyle();
                    if (j == 1 || j == 9) {
                        cellStyle.setBorderLeft(BorderStyle.DOUBLE);
                        cell.setCellStyle(cellStyle);
                    }
                    if(startRow + i < PAGE_ROW_MAX-2) {
                        if (j >= 2 && j <= 7) {
                            middleCellStyle.setBorderLeft(BorderStyle.THIN);
                            middleCellStyle.setBorderRight(BorderStyle.THIN);
                            cell.setCellStyle(middleCellStyle);
                        }
                    }
                }
            }

            // moving down the images
            XSSFDrawing drawing = (XSSFDrawing) sheet.getDrawingPatriarch();
            for (Shape shape : drawing.getShapes()) {
                if (shape instanceof Picture){
                    XSSFPicture picture = (XSSFPicture)shape;
                    XSSFClientAnchor anchor = picture.getClientAnchor();

                    anchor.setRow1(data.size() - dataRowNumber - 1 <= 13 ? anchor.getRow1() + PAGE_ROW_MAX-LAST_ROW :  anchor.getRow1() + additionalRowNum);
                    anchor.setRow2(data.size() - dataRowNumber - 1 <= 13 ? anchor.getRow2() + PAGE_ROW_MAX-LAST_ROW :  anchor.getRow2() + additionalRowNum);
                }
            }
        }
        // table section
        for (int i = 0; i < data.size(); i++) {
            lineNum = i + FIRST_ROW;
            rowValue = data.get(i);
            log.info("Writing line {} with data {}", lineNum, rowValue);
            configCell("C", lineNum, String.format("%06d", i + 1), factory.leftSideStyle());
            configCell("D", lineNum, rowValue.getCol1(), factory.leftSideStyle());
            configCell("E", lineNum, rowValue.getCol2(), factory.rightSideDecimalStyle());
            configCell("F", lineNum, rowValue.getCol3(), factory.rightSideStyle());
            configCell("G", lineNum, rowValue.getCol4(), factory.rightSideDecimalStyle());
            configCell("H", lineNum, rowValue.getCol5(), factory.rightSideDecimalStyle());
        }
        // on fait le total par colonne
        if(data.size() > dataRowNumber)
        {
            org.apache.poi.ss.usermodel.Row totalRow;
            if(additionalRowNum%PAGE_ROW_MAX <= 13)
                totalRow = sheet.getRow(TOTAL_ROW-1);
            else
                totalRow = sheet.getRow(TOTAL_ROW);
            totalRow.setHeight(sourceRow.getHeight());
            totalRow.setRowStyle(null);
            totalRow.setRowStyle(sourceRow.getRowStyle());

            Font arialNormal = factory.getWorkbook().createFont();
            arialNormal.setFontName("Arial");
            arialNormal.setFontHeightInPoints((short) 12);
            arialNormal.setBold(false);

            Font arialBold = factory.getWorkbook().createFont();
            arialBold.setFontName("Arial");
            arialBold.setFontHeightInPoints((short) 14);
            arialBold.setBold(true);

            // looping throw the total row, setting border styles and setting formula
            for(int i = 0; i < 10; i++)
            {
                Cell cell = totalRow.createCell(i);
                CellStyle cellStyle = factory.getWorkbook().createCellStyle();
                if(i == 0){
                    cellStyle.setBorderLeft(BorderStyle.NONE);
                    cellStyle.setBorderTop(BorderStyle.NONE);
                    cellStyle.setBorderBottom(BorderStyle.NONE);
                    cellStyle.setBorderRight(BorderStyle.NONE);
                }
                else if(i==1 || i == 9) {
                    cellStyle.setBorderLeft(BorderStyle.DOUBLE);
                    cellStyle.setBorderTop(BorderStyle.NONE);
                    cellStyle.setBorderRight(BorderStyle.NONE);
                    cellStyle.setBorderBottom(BorderStyle.NONE);
                }
                else if(i < 8){
                    cellStyle.setBorderLeft(BorderStyle.THIN);
                    cellStyle.setBorderTop(BorderStyle.THIN);
                    cellStyle.setBorderRight(BorderStyle.NONE);
                    cellStyle.setBorderBottom(BorderStyle.THIN);
                }
                else {
                    cellStyle.setBorderRight(BorderStyle.NONE);
                    cellStyle.setBorderTop(BorderStyle.NONE);
                    cellStyle.setBorderBottom(BorderStyle.NONE);
                    cellStyle.setBorderLeft(BorderStyle.THIN);
                }
                cellStyle.setFont(arialNormal);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                cell.setCellStyle(cellStyle);
                if(i==2)
                    cell.setCellValue("Total");
                if(i==5) {
                    if (additionalRowNum % PAGE_ROW_MAX <= 13)
                        cell.setCellFormula("SUM(F" + FIRST_ROW + ":F" + (TOTAL_ROW-1) + ")");
                    else
                        cell.setCellFormula("SUM(F" + FIRST_ROW + ":F" + (TOTAL_ROW) + ")");
                }
                if(i==6) {
                    if (additionalRowNum % PAGE_ROW_MAX <= 13)
                        cell.setCellFormula("SUM(G" + FIRST_ROW + ":G" + (TOTAL_ROW - 1) + ")");
                    else
                        cell.setCellFormula("SUM(G" + FIRST_ROW + ":G" + (TOTAL_ROW) + ")");
                }
                if(i==7) {
                    if (additionalRowNum % PAGE_ROW_MAX <= 13)
                        cell.setCellFormula("SUM(H" + FIRST_ROW + ":H" + (TOTAL_ROW - 1) + ")");
                    else
                        cell.setCellFormula("SUM(H" + FIRST_ROW + ":H" + (TOTAL_ROW) + ")");
                }
            }

            //Total due
            org.apache.poi.ss.usermodel.Row totalDueRow;
            CellStyle totalDueCellStyle = factory.getWorkbook().createCellStyle();
            totalDueCellStyle.setBorderRight(BorderStyle.THIN);
            totalDueCellStyle.setBorderBottom(BorderStyle.THIN);
            totalDueCellStyle.setBorderTop(BorderStyle.THIN);
            totalDueCellStyle.setFont(arialBold);

            if(additionalRowNum%PAGE_ROW_MAX <= 13) {
                totalDueRow = sheet.getRow(PAGE_ROW_MAX + 2);
                Cell totalDueCell = totalDueRow.createCell(7);
                totalDueCell.setCellFormula("H" + (PAGE_ROW_MAX - 2) + "-G" + (PAGE_ROW_MAX - 2));
                totalDueCell.setCellStyle(totalDueCellStyle);
            }
            else {
                totalDueRow = sheet.getRow(TOTAL_ROW + 2);
                Cell totalDueCell = totalDueRow.createCell(7);
                totalDueCell.setCellFormula("H" + (TOTAL_ROW+1) + "-G" + (TOTAL_ROW+1));
                totalDueCell.setCellStyle(totalDueCellStyle);
            }
        }

        if (targetClient.getCurrency().equals("USD")) {
            org.apache.poi.ss.usermodel.Row dollarRow;
            String formula;
            if(data.size() > dataRowNumber){
                if (additionalRowNum % PAGE_ROW_MAX <= 13) {
                    dollarRow = sheet.getRow(TOTAL_ROW + 5);
                    formula = "H" + (TOTAL_ROW + 5) + " *" + exchangeRate;
                } else {
                    dollarRow = sheet.getRow(TOTAL_ROW + 3);
                    formula = "H" + (TOTAL_ROW + 3) + " *" + exchangeRate;
                }
            }
            else {
                dollarRow = sheet.getRow(TOTAL_ROW + 2);
                formula = "H" + (TOTAL_ROW + 2) + " *" + exchangeRate;
            }
            Cell dollarCell = dollarRow.createCell(7); // column H
            CellStyle cellStyle = factory.getWorkbook().createCellStyle();
            DataFormat format = factory.getWorkbook().createDataFormat();
            Font arial = factory.getWorkbook().createFont();
            arial.setFontName("Arial");
            arial.setFontHeightInPoints((short) 12);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setAlignment(HorizontalAlignment.RIGHT);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setFont(arial);
            cellStyle.setDataFormat(format.getFormat("#,##0.00")); // to get decimal format eg : "1234,56" and not "1234,5678" by default
            dollarCell.setCellStyle(cellStyle);
            dollarRow.getCell(7).setCellFormula(formula);
        }
    }


    /**
     * A shortcut to configure a cell of a sheet with a value and a style by global writer.
     *
     * @param col   column of location of the cell, for example "C"
     * @param line  line number of location of the cell, for example 11
     * @param value the value to set
     * @param style the style to apply
     */
    protected void configCell(String col, int line, Object value, CellStyle style) {
        configCell(col + line, value, style);
    }


    /**
     * A shortcut to configure a cell of a sheet with a value and a style by global writer.
     *
     * @param locationRef location of the cell, for example "C11"
     * @param value       the value to set
     * @param style       the style to apply
     */
    protected void configCell(String locationRef, Object value, CellStyle style) {
        writer.writeCellValue(locationRef, value);
        writer.setStyle(style, locationRef);
    }

}
