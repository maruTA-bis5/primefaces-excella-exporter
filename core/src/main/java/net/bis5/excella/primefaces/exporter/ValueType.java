package net.bis5.excella.primefaces.exporter;

import java.util.function.Function;

import org.apache.poi.ss.usermodel.Workbook;

public enum ValueType {
    STRING(w -> 0x0),
    YEAR_MONTH(w -> w.createDataFormat().getFormat("yy/m")),
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
}
