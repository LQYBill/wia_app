package org.jeecg.modules.business.domain.invoice;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.jeecg.modules.business.entity.Client;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractInvoice<E, F, G, H, I> {
    protected final Client targetClient;

    private final String code;

    private final String subject;

    protected ExcelWriter writer;

    public AbstractInvoice(Client targetClient,
                           String code, String subject) {
        this.targetClient = targetClient;
        this.code = code;
        this.subject = subject;
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
    private final static int LAST_ROW = 39;

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
        // table section
        for (int i = 0; i < data.size(); i++) {
            lineNum = i + FIRST_ROW;
            rowValue = data.get(i);

            configCell("C", lineNum, String.format("%06d", i + 1), factory.leftSideStyle());
            configCell("D", lineNum, rowValue.getCol1(), factory.leftSideStyle());
            configCell("E", lineNum, rowValue.getCol2(), factory.rightSideStyle());
            configCell("F", lineNum, rowValue.getCol3(), factory.rightSideStyle());
            configCell("G", lineNum, rowValue.getCol4(), factory.rightSideStyle());
            configCell("H", lineNum, rowValue.getCol5(), factory.rightSideStyle());
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
