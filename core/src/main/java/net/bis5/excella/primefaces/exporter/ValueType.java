package net.bis5.excella.primefaces.exporter;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

public enum ValueType {
    STRING(w -> w.createDataFormat().getFormat("General")),
    YEAR_MONTH(w -> w.createDataFormat().getFormat("yyyy/m")),
    DATE(w -> 0xe),
    DATE_TIME(w -> 0x16),
    TIME(w -> w.createDataFormat().getFormat("[h]:mm")),
    DECIMAL(w -> 0x4),
    INTEGER(w -> 0x3);

    static final Pattern timePattern = Pattern.compile("^\\d+:[0-5]\\d$");

    private final Function<Workbook, Short> dataFormat;

    ValueType(Function<Workbook, Short> dataFormat) {
        this.dataFormat = dataFormat;
    }

    public short getFormat(Workbook workbook) {
        return dataFormat.apply(workbook);
    }

    public static Map<ValueType, CellStyle> initStyles(Workbook workbook) {
        return initStyles(workbook, null);
    }

    static Map<ValueType, CellStyle> initStyles(Workbook workbook, ExCellaExporterOptions options) {
        EnumMap<ValueType, CellStyle> styles = new EnumMap<>(ValueType.class);
        for (ValueType valueType : values()) {
            CellStyle style = workbook.createCellStyle();
            Short overrideDataFormat = options == null ? null : options.getOverrideDataFormat(valueType, workbook);
            style.setDataFormat(overrideDataFormat == null ? valueType.getFormat(workbook) : overrideDataFormat);
            styles.put(valueType, style);
        }
        return styles;
    }
}
