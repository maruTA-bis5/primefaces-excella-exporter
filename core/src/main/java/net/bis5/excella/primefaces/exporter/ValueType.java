package net.bis5.excella.primefaces.exporter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

public enum ValueType {
    STRING(w -> w.createDataFormat().getFormat("General")),
    YEAR_MONTH(w -> w.createDataFormat().getFormat("yyyy/m")),
    DATE(w -> 0xe),
    DATE_TIME(w -> 0x16),
    TIME(w -> 0x14),
    DECIMAL(w -> 0x4),
    INTEGER(w -> 0x3);

    private final Function<Workbook, Short> dataFormat;

    ValueType(Function<Workbook, Short> dataFormat) {
        this.dataFormat = dataFormat;
    }

    public short getFormat(Workbook workbook) {
        return dataFormat.apply(workbook);
    }

    public static Map<ValueType, CellStyle> initStyles(Workbook workbook) {
        EnumMap<ValueType, CellStyle> styles = new EnumMap<>(ValueType.class);

        CellStyle stringStyle = workbook.createCellStyle();
        stringStyle.setDataFormat(ValueType.STRING.getFormat(workbook));
        styles.put(ValueType.STRING, stringStyle);

        CellStyle yearMonthStyle = workbook.createCellStyle();
        yearMonthStyle.setDataFormat(ValueType.YEAR_MONTH.getFormat(workbook));
        styles.put(ValueType.YEAR_MONTH, yearMonthStyle);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(ValueType.DATE.getFormat(workbook));
        styles.put(ValueType.DATE, dateStyle);

        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(ValueType.DATE_TIME.getFormat(workbook));
        styles.put(ValueType.DATE_TIME, dateTimeStyle);

        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setDataFormat(ValueType.TIME.getFormat(workbook));
        styles.put(ValueType.TIME, timeStyle);

        CellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(ValueType.DECIMAL.getFormat(workbook));
        styles.put(ValueType.DECIMAL, decimalStyle);

        CellStyle integerStyle = workbook.createCellStyle();
        integerStyle.setDataFormat(ValueType.INTEGER.getFormat(workbook));
        styles.put(ValueType.INTEGER, integerStyle);

        return styles;
    }
}
