package org.jeecg.modules.business.domain;

import org.apache.poi.ss.usermodel.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class InvoiceStyleFactory {
    static DateFormat INVOICE_CODE_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final Workbook workbook;

    private CellStyle leftSideStyle;
    private CellStyle rightSideStyle;
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

    public CellStyle leftSideStyle() {
        if (leftSideStyle != null)
            return leftSideStyle;
        // border
        leftSideStyle = workbook.createCellStyle();
        leftSideStyle.setBorderBottom(BorderStyle.NONE);
        leftSideStyle.setBorderLeft(BorderStyle.THIN);
        leftSideStyle.setBorderRight(BorderStyle.THIN);
        leftSideStyle.setBorderTop(BorderStyle.NONE);
        leftSideStyle.setAlignment(HorizontalAlignment.LEFT);
        leftSideStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // font
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        leftSideStyle.setFont(font);
        return leftSideStyle;
    }

    public CellStyle rightSideStyle() {
        if (rightSideStyle != null)
            return rightSideStyle;
        // border
        rightSideStyle = workbook.createCellStyle();
        rightSideStyle.setBorderBottom(BorderStyle.NONE);
        rightSideStyle.setBorderLeft(BorderStyle.THIN);
        rightSideStyle.setBorderRight(BorderStyle.THIN);
        rightSideStyle.setBorderTop(BorderStyle.NONE);
        rightSideStyle.setAlignment(HorizontalAlignment.RIGHT);
        rightSideStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // font
        Font font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        rightSideStyle.setFont(font);
        return rightSideStyle;
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
