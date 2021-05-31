package org.jeecg.modules.business.domain;

import net.sf.saxon.functions.NamespaceUri_1;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class InvoiceStyleFactory {
    static DateFormat INVOICE_CODE_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final Workbook workbook;

    private CellStyle tableCell;
    private CellStyle otherStyle;

    public InvoiceStyleFactory(Workbook workbook) {
        this.workbook = workbook;
    }

    public CellStyle invoiceCodeStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.NONE);
        style.setBorderLeft(BorderStyle.NONE);
        style.setBorderRight(BorderStyle.NONE);
        style.setBorderTop(BorderStyle.NONE);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // font
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 20);
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public CellStyle tableCellStyle() {
        if (tableCell != null)
            return tableCell;
        // border
        tableCell = workbook.createCellStyle();
        tableCell.setBorderBottom(BorderStyle.NONE);
        tableCell.setBorderLeft(BorderStyle.THIN);
        tableCell.setBorderRight(BorderStyle.THIN);
        tableCell.setBorderTop(BorderStyle.NONE);
        tableCell.setAlignment(HorizontalAlignment.CENTER);
        tableCell.setVerticalAlignment(VerticalAlignment.CENTER);
        // font
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        tableCell.setFont(font);
        return tableCell;
    }

    public CellStyle otherStyle() {
        if (otherStyle != null)
            return otherStyle;
        // border
        otherStyle = workbook.createCellStyle();
        otherStyle.setBorderBottom(BorderStyle.NONE);
        otherStyle.setBorderLeft(BorderStyle.NONE);
        otherStyle.setBorderRight(BorderStyle.NONE);
        otherStyle.setBorderTop(BorderStyle.NONE);

        // font
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        otherStyle.setFont(font);
        return otherStyle;
    }
}
