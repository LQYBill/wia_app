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

public abstract class AbstractInvoice {
    private final Client targetClient;

    private final String code;

    private final String subject;

    private ExcelWriter writer;

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

        List<Row> data = tableData();
        int lineNum;
        Row rowValue;
        // table section
        for (int i = 0; i < data.size(); i++) {
            lineNum = i + FIRST_ROW;
            rowValue = data.get(i);
            writer.writeCellValue("C" + lineNum, String.format("%06d", i + 1));
            writer.setStyle(factory.leftSideStyle(), "C" + lineNum);

            writer.writeCellValue("D" + lineNum, rowValue.getDescription());
            writer.setStyle(factory.leftSideStyle(), "D" + lineNum);

            writer.writeCellValue("E" + lineNum, rowValue.getDescription());
            writer.setStyle(factory.leftSideStyle(), "D" + lineNum);

            writer.writeCellValue("F" + lineNum, String.format("%06d", i + 1));
            writer.setStyle(factory.rightSideStyle(), "C" + lineNum);

            writer.writeCellValue("G" + lineNum, rowValue.getDescription());
            writer.setStyle(factory.rightSideStyle(), "D" + lineNum);

            configCell("H" + lineNum,);
            writer.writeCellValue(, rowValue.getDescription());
            writer.setStyle(factory.rightSideStyle(), "D" + lineNum);
        }
        writer.setStyle(factory.invoiceCodeStyle(), INVOICE_CODE_LOCATION);
        // revaluate formulae to enable automatic calculation
        FormulaEvaluator evaluator = writer.getWorkbook().getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        writer.flush();
        writer.close();
    }

    /**
     * A shortcut to configure a cell of a sheet with a value and a style by global writer.
     *
     * @param locationRef location of the cell, for example "C11"
     * @param value       the value to set
     * @param style       the style to apply
     */
    private void configCell(String locationRef, Object value, CellStyle style) {
        writer.writeCellValue(locationRef, value);
        writer.setStyle(style, locationRef);
    }

    public String entity() {
        return targetClient.getInvoiceEntity();
    }


    private void fillInformation(ExcelWriter writer, InvoiceStyleFactory factory) {
        for (Map.Entry<String, Object> entry : invoiceInformation().entrySet()) {
            writer.writeCellValue(entry.getKey(), entry.getValue());
            writer.setStyle(factory.otherStyle(), entry.getKey());
        }
    }

    /**
     * Fill content that does not belong to the table section.
     *
     * @return a map between a cell location and its contents. For example: G3 -> Toto
     */
    private Map<String, Object> invoiceInformation() {
        Map<String, Object> cellContentMap = new HashMap<>();
        cellContentMap.put(INVOICE_CODE_LOCATION, code);
        cellContentMap.put(GENERATED_DATE_LOCATION, InvoiceStyleFactory.INVOICE_CODE_DATETIME_FORMAT.format(new Date()));
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
        return cellContentMap;
    }

    /**
     * Fill cells that belong to the table section.
     *
     * @return map between location and table cell
     */
    protected abstract List<Row> tableData();
}
