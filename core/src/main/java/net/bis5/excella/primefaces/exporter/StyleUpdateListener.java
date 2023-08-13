package net.bis5.excella.primefaces.exporter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.core.SheetParser;
import org.bbreak.excella.reports.listener.ReportProcessAdaptor;
import org.bbreak.excella.reports.model.ReportBook;
import org.bbreak.excella.reports.model.ReportSheet;
import org.bbreak.excella.reports.tag.ColRepeatParamParser;
import org.bbreak.excella.reports.tag.RowRepeatParamParser;

class StyleUpdateListener extends ReportProcessAdaptor {

    private CellAddress headerPosition;

    private CellAddress dataPosition;

    private CellAddress footerPosition;

    private int headerSize;

    private Map<ValueType, CellStyle> styles;

    private final ReportSheet reportSheet;

    private final Map<String, List<Object>> dataContainer;

    private final String dataTagName;

    private final String headersTagName;

    private final String footersTagName;

    private final int columnSize;

    private final Object[] columnDataParams;

    private final Map<String, ValueType> valueTypes;

    private final int repeatRows;

    private final Set<CellRangeAddress> headerMergedAreas;

    private final Set<CellRangeAddress> footerMergedAreas;

    @SuppressWarnings("unchecked")
    StyleUpdateListener(ReportSheet reportSheet, Map<String, List<Object>> dataContainer, String dataTagName,
            String headersTagName, String footersTagName, int columnSize, Object[] columnDataParams) {
        this.reportSheet = reportSheet;
        this.dataContainer = dataContainer;
        this.dataTagName = dataTagName;
        this.headersTagName = headersTagName;
        this.footersTagName = footersTagName;
        this.columnSize = columnSize;
        this.columnDataParams = columnDataParams;

        valueTypes = detectValueTypes(dataContainer);
        repeatRows = dataContainer.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(1);
        headerMergedAreas = Objects.requireNonNullElseGet((Set<CellRangeAddress>) reportSheet.getParam(null, ExCellaExporter.COLUMN_GROUP_MERGED_AREAS_KEY + "header"), HashSet::new);
        footerMergedAreas = Objects.requireNonNullElseGet((Set<CellRangeAddress>) reportSheet.getParam(null, ExCellaExporter.COLUMN_GROUP_MERGED_AREAS_KEY + "footer"), HashSet::new);
        reportSheet.removeParam(null, ExCellaExporter.COLUMN_GROUP_MERGED_AREAS_KEY + "header");
        reportSheet.removeParam(null, ExCellaExporter.COLUMN_GROUP_MERGED_AREAS_KEY + "footer");
    }

    private Map<String, ValueType> detectValueTypes(Map<String, List<Object>> dataContainer) {
        Map<String, ValueType> detectedValueTypes = new HashMap<>();
        for(Entry<String, List<Object>> entry : dataContainer.entrySet()) {
            String key = entry.getKey();
            List<Object> values = entry.getValue();
            ValueType type = detectValueType(values);
            detectedValueTypes.put(key, type);
        }
        return detectedValueTypes;
    }

    private Pattern timePattern = Pattern.compile("^\\d+:\\d\\d$");

    private ValueType detectValueType(List<Object> values) {
        Set<ValueType> types = values.stream()
            .map(this::detectValueType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (types.isEmpty()) {
            return null;
        }
        if (types.contains(ValueType.INTEGER) && types.contains(ValueType.DECIMAL)) {
            return ValueType.DECIMAL;
        }
        if (types.contains(ValueType.DATE) && types.contains(ValueType.DATE_TIME)) {
            return ValueType.DATE_TIME;
        }
        return types.iterator().next();
    }

    private ValueType detectValueType(Object value) {
        if (value instanceof LocalDateTime || (value instanceof Date && hasTime((Date)value)) || (value instanceof Calendar && hasTime((Calendar)value))) {
            return ValueType.DATE_TIME;
        }
        if (value instanceof LocalDate || value instanceof Date || value instanceof Calendar) {
            return ValueType.DATE;
        }
        if (value instanceof LocalTime || (value instanceof String && timePattern.matcher((String)value).matches())) {
            return ValueType.TIME;
        }
        if (value instanceof YearMonth) {
            return ValueType.YEAR_MONTH;
        }
        if (value instanceof Number) {
            if (value instanceof Long || value instanceof Integer) {
                return ValueType.INTEGER;
            } else if (value instanceof BigDecimal) {
                BigDecimal bigDecimal = (BigDecimal)value;
                if (bigDecimal.compareTo(BigDecimal.valueOf(bigDecimal.longValue())) == 0) {
                    return ValueType.INTEGER;
                } else {
                    return ValueType.DECIMAL;
                }
            } else {
                return ValueType.DECIMAL;
            }
        }
        return null;
    }

    private boolean hasTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return hasTime(cal);
    }

    private boolean hasTime(Calendar cal) {
        return cal.get(Calendar.HOUR_OF_DAY) != 0 && cal.get(Calendar.MINUTE) != 0 && cal.get(Calendar.SECOND) != 0;
    }


    @Override
    public void preBookParse(Workbook workbook, ReportBook reportBook) {
        styles = ValueType.initStyles(workbook);
    }

    private void setHeaderPosition(CellAddress address) {
        headerPosition = address;
    }

    private void setDataPosition(CellAddress address) {
        dataPosition = address;
    }

    protected int dataRowOffset(int row) {
        return Math.max(headerSize - 1, 0) + (dataPosition != null ? row + dataPosition.getRow() : row);
    }

    protected int dataColOffset(int col) {
        return dataPosition != null ? col + dataPosition.getColumn()  : col;
    }

    private void setFooterPosition(CellAddress address) {
        footerPosition = address;
    }

    @Override
    public void preParse(Sheet sheet, SheetParser sheetParser) {
        String headerTag = ColRepeatParamParser.DEFAULT_TAG + "{" + headersTagName + "}";
        String footerTag = ColRepeatParamParser.DEFAULT_TAG + "{" + footersTagName + "}";
        String dataTag = ColRepeatParamParser.DEFAULT_TAG + "{" + dataTagName + "}";
        StreamSupport.stream(sheet.spliterator(), false)
            .map(Row::spliterator)
            .flatMap(s -> StreamSupport.stream(s, false))
            .filter(c -> c.getCellType() == CellType.STRING)
            .forEach(c -> {
                if (headerTag.equals(c.getStringCellValue())) {
                    setHeaderPosition(c.getAddress());
                } else if (footerTag.equals(c.getStringCellValue())) {
                    setFooterPosition(c.getAddress());
                } else if (dataTag.equals(c.getStringCellValue())) {
                    setDataPosition(c.getAddress());
                }
            });
    }

    @Override
    public void postParse(Sheet sheet, SheetParser sheetParser, SheetData sheetData) {
        if (dataContainer.isEmpty() || !sheetData.getSheetName().equals(reportSheet.getSheetName())) {
            return;
        }
        IntStream.range(0, columnSize)
            .mapToObj(i -> "header" + i)
            .map(t -> reportSheet.getParam(RowRepeatParamParser.DEFAULT_TAG, t))
            .filter(Objects::nonNull)
            .map(Object[].class::cast)
            .mapToInt(a -> a.length)
            .max()
            .ifPresentOrElse(s -> headerSize = s, () -> headerSize = 1);
        for (Entry<String, ValueType> entry : valueTypes.entrySet()) {
            String columnTag = getColumnTag(entry.getKey());
            ValueType valueType = entry.getValue();
            if (valueType == null) {
                continue;
            }
            CellStyle style = styles.get(valueType);
            int colIndex = Arrays.asList(columnDataParams).indexOf(columnTag);
            IntStream.range(dataRowOffset(0), dataRowOffset(repeatRows))
                .mapToObj(sheet::getRow)
                .filter(Objects::nonNull)
                .map(r -> r.getCell(dataColOffset(colIndex)))
                .filter(Objects::nonNull)
                .forEach(c -> setCellStyle(c, style));
        }
    }

    private void setCellStyle(Cell cell, CellStyle style) {
        CellUtil.setCellStyleProperty(cell, CellUtil.DATA_FORMAT, style.getDataFormat());
    }
    private String getColumnTag(String key) {
        return "$R[]{" + key + "}";
    }

    @Override
    public void postBookParse(Workbook workbook, ReportBook reportBook) {
        IntStream.range(0, workbook.getNumberOfSheets())
            .mapToObj(workbook::getSheetAt)
            .forEach(this::mergeCells);
    }

    private void mergeCells(Sheet sheet) {
        int headerOffset = headerPosition == null ? 0 : headerPosition.getRow();
        headerMergedAreas.forEach(a -> mergeCell(sheet, headerPosition, headerOffset, a));

        int footerOffset = footerPosition == null ? 0 : dataRowOffset(repeatRows) + footerPosition.getRow() - dataPosition.getRow() - 1;
        footerMergedAreas.forEach(a -> mergeCell(sheet, footerPosition, footerOffset, a));
    }

    private void mergeCell(Sheet sheet, CellAddress beginPosition, int rowOffset, CellRangeAddress area) {
        if (beginPosition == null) {
            return;
        }
        if (beginPosition.equals(CellAddress.A1) && rowOffset <= 0) {
            sheet.addMergedRegion(area);
            return;
        }
        int colOffset = beginPosition.getColumn();

        var rangeToMerge = new CellRangeAddress(rowOffset + area.getFirstRow(), rowOffset + area.getLastRow(), colOffset + area.getFirstColumn(), colOffset + area.getLastColumn());
        sheet.addMergedRegion(rangeToMerge);
    }

}
