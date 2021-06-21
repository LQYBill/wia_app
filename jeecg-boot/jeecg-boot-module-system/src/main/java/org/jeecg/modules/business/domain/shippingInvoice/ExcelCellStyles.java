package org.jeecg.modules.business.domain.shippingInvoice;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.*;

/**
 * This class contains some predefined excel cell style.
 */
class ExcelCellStyles {
    /**
     * Constant style used for invoiceCode cell.
     *
     * @param workbook the workbook to apply this style
     * @return the style
     */
    public static CellStyle invoiceCode(Workbook workbook) {
        return makeCellStyle(workbook,
                makeFont(workbook, 0, 22, "Arial", true),
                BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE,
                HorizontalAlignment.LEFT, VerticalAlignment.CENTER
        );
    }

    /**
     * Constant style used for generated date.
     *
     * @param workbook the workbook to apply this style
     * @return the style
     */
    public static CellStyle generatedDate(Workbook workbook) {
        return makeCellStyle(workbook,
                makeFont(workbook, 0, 12, "Arial", true),
                BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE,
                HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM
        );
    }


    /**
     * Constant style used for customer information cell.
     *
     * @param workbook the workbook to apply this style
     * @return the style
     */
    public static CellStyle customerInfo(Workbook workbook) {
        return makeCellStyle(workbook,
                makeFont(workbook, 0, 11, "Arial", false),
                BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE,
                HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM
        );
    }

    /**
     * Constant style used for subject cell.
     *
     * @param workbook the workbook to apply this style
     * @return the style
     */
    public static CellStyle subject(Workbook workbook) {
        CellStyle cellStyle = makeCellStyle(workbook,
                makeFont(workbook, 0, 11, "Arial", true),
                BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN,
                HorizontalAlignment.LEFT, VerticalAlignment.CENTER
        );
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return cellStyle;
    }

    public static CellStyle dataCell(Workbook workbook) {
        return makeCellStyle(workbook,
                makeFont(workbook, 0, 12, "Arial", false),
                BorderStyle.NONE, BorderStyle.THIN, BorderStyle.NONE, BorderStyle.THIN,
                HorizontalAlignment.LEFT, VerticalAlignment.CENTER
        );
    }

    /**
     * Constant style used for generated date.
     *
     * @param workbook the workbook to apply this style
     * @return the style
     */
    public static CellStyle rightAlignmentDate(Workbook workbook) {
        return makeCellStyle(workbook,
                makeFont(workbook, 0, 12, "Arial", false),
                BorderStyle.NONE, BorderStyle.THIN, BorderStyle.NONE, BorderStyle.THIN,
                HorizontalAlignment.RIGHT, VerticalAlignment.CENTER
        );
    }


    /**
     * Constant style used for total amount cell.
     *
     * @param workbook the workbook to apply this style
     * @return the style
     */
    public static CellStyle totalDue(Workbook workbook) {
        CellStyle cellStyle = makeCellStyle(workbook,
                makeFont(workbook, 0, 14, "Arial", true),
                BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN,
                HorizontalAlignment.RIGHT, VerticalAlignment.CENTER
        );
        cellStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.0000"));
        return cellStyle;
    }


    /**
     * A shortcut to generate a font.
     *
     * @param workbook the workbook to apply this style
     * @param color    the color of the font
     * @param fontSize the font size
     * @param fontName the font name
     */
    private static Font makeFont(Workbook workbook, int color, int fontSize, String fontName, boolean isBold) {
        Font font = workbook.createFont();
        font.setColor((short) color);
        font.setFontHeightInPoints((short) fontSize);
        font.setFontName(fontName);
        font.setBold(isBold);
        return font;
    }


    /**
     * A shortcut to generate a cell style.
     *
     * @param workbook            the workbook to apply this style
     * @param font                the font of cell
     * @param top                 top border of the cell
     * @param right               right border of the cell
     * @param bottom              bottom border of the cell
     * @param left                left border of the cell
     * @param horizontalAlignment horizontal alignment
     * @param verticalAlignment   vertical alignment
     * @return a cell style
     */
    private static CellStyle makeCellStyle(Workbook workbook, Font font,
                                           BorderStyle top, BorderStyle right,
                                           BorderStyle bottom, BorderStyle left,
                                           HorizontalAlignment horizontalAlignment,
                                           VerticalAlignment verticalAlignment) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setBorderTop(top);
        cellStyle.setBorderRight(right);
        cellStyle.setBorderBottom(bottom);
        cellStyle.setBorderLeft(left);
        cellStyle.setAlignment(horizontalAlignment);
        cellStyle.setVerticalAlignment(verticalAlignment);
        return cellStyle;
    }


}
