package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellFill;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.reports.model.ReportSheet;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType;

class StyleUpdateListenerTest {

    @Test
    void noFillHasAnExplicitPatternAndDoesNotChangeTheSourceStyle() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setBorderTop(BorderStyle.THIN);
            CTFill fill = CTFill.Factory.newInstance();
            CTPatternFill pattern = fill.addNewPatternFill();
            pattern.addNewFgColor().setIndexed(64);
            pattern.addNewBgColor().setIndexed(64);
            source.getCoreXf().setFillId(workbook.getStylesSource().putFill(
                    new XSSFCellFill(fill, workbook.getStylesSource().getIndexedColors())));
            Sheet sheet = workbook.createSheet("Sheet");
            Cell first = sheet.createRow(0).createCell(0);
            Cell second = sheet.createRow(1).createCell(0);
            first.setCellStyle(source);
            second.setCellStyle(source);

            update(workbook, sheet, Arrays.asList(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)));

            assertEquals(first.getCellStyle().getIndex(), second.getCellStyle().getIndex());
            assertEquals(BorderStyle.THIN, first.getCellStyle().getBorderTop());
            assertEquals(ValueType.DATE.getFormat(workbook), first.getCellStyle().getDataFormat());
            assertFalse(patternFill(workbook, source).isSetPatternType());
            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle savedStyle = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                CTPatternFill savedPattern = patternFill(saved, savedStyle);
                assertEquals(STPatternType.NONE, savedPattern.getPatternType());
                assertEquals(64, savedPattern.getFgColor().getIndexed());
                assertEquals(64, savedPattern.getBgColor().getIndexed());
                assertEquals(BorderStyle.THIN, savedStyle.getBorderTop());
            }
        }
    }

    @Test
    void absentColorsStayAbsentAndExistingFormatIsNormalized() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setDataFormat(ValueType.DATE.getFormat(workbook));
            CTFill fill = CTFill.Factory.newInstance();
            fill.addNewPatternFill();
            source.getCoreXf().setFillId(workbook.getStylesSource().putFill(
                    new XSSFCellFill(fill, workbook.getStylesSource().getIndexedColors())));
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellStyle(source);

            update(workbook, sheet, List.of(LocalDate.of(2026, 1, 1)));

            try (XSSFWorkbook saved = roundTrip(workbook)) {
                CTPatternFill result = patternFill(saved, saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle());
                assertEquals(STPatternType.NONE, result.getPatternType());
                assertFalse(result.isSetFgColor());
                assertFalse(result.isSetBgColor());
            }
        }
    }

    @Test
    void existingRgbFillAndOtherStylePropertiesArePreserved() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            byte[] rgb = {(byte) 0x12, (byte) 0x34, (byte) 0x56};
            source.setFillForegroundColor(new XSSFColor(rgb, workbook.getStylesSource().getIndexedColors()));
            source.setFillBackgroundColor(IndexedColors.AUTOMATIC.getIndex());
            source.setWrapText(true);
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellStyle(source);

            update(workbook, sheet, List.of(1.5));

            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle result = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                assertEquals(ValueType.DECIMAL.getFormat(saved), result.getDataFormat());
                assertEquals(FillPatternType.SOLID_FOREGROUND, result.getFillPattern());
                assertArrayEquals(rgb, result.getFillForegroundColorColor().getRGB());
                assertEquals(IndexedColors.AUTOMATIC.getIndex(), result.getFillBackgroundColor());
                assertTrue(result.getWrapText());
            }
        }
    }

    @Test
    void themeFillIsPreserved() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFColor themeColor = new XSSFColor(workbook.getStylesSource().getIndexedColors());
            themeColor.setTheme(3);
            themeColor.setTint(0.25);
            source.setFillForegroundColor(themeColor);
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellStyle(source);

            update(workbook, sheet, List.of(1));

            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle result = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                assertEquals(FillPatternType.SOLID_FOREGROUND, result.getFillPattern());
                assertEquals(3, result.getFillForegroundColorColor().getTheme());
                assertEquals(0.25, result.getFillForegroundColorColor().getTint());
            }
        }
    }

    @Test
    void copyingAStyleDoesNotOverwriteAnotherBaseStyle() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CTXf defaultBaseStyle = workbook.getStylesSource().getCellStyleXfAt(0);
            CTXf otherBaseStyle = (CTXf) defaultBaseStyle.copy();
            otherBaseStyle.setNumFmtId(49);
            int otherBaseStyleId = workbook.getStylesSource().putCellStyleXf(otherBaseStyle) - 1;
            CTXf lastBaseStyle = (CTXf) defaultBaseStyle.copy();
            lastBaseStyle.setNumFmtId(14);
            workbook.getStylesSource().putCellStyleXf(lastBaseStyle);
            String originalBaseStyle = lastBaseStyle.xmlText();
            XSSFCellStyle source = workbook.createCellStyle();
            source.getCoreXf().setXfId(otherBaseStyleId);
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellStyle(source);

            update(workbook, sheet, List.of(1));

            assertEquals(originalBaseStyle, lastBaseStyle.xmlText());
            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle result = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                assertEquals(otherBaseStyleId, result.getCoreXf().getXfId());
                assertEquals(49, result.getStyleXf().getNumFmtId());
            }
        }
    }

    @Test
    void gradientFillIsPreserved() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            CTFill fill = CTFill.Factory.parse("<fill xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                    + "<gradientFill degree=\"45\"/></fill>");
            source.getCoreXf().setFillId(workbook.getStylesSource().putFill(
                    new XSSFCellFill(fill, workbook.getStylesSource().getIndexedColors())));
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellStyle(source);

            update(workbook, sheet, List.of(1));

            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle result = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                CTFill savedFill = saved.getStylesSource().getFillAt((int) result.getCoreXf().getFillId()).getCTFill();
                assertFalse(savedFill.isSetPatternFill());
                assertTrue(savedFill.xmlText().contains("gradientFill"));
                assertTrue(savedFill.xmlText().contains("degree=\"45\""));
            }
        }
    }

    @Test
    void allSupportedValueFormatsKeepNoFillSafe() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            Map<String, List<Object>> values = Map.of(
                    "yearMonth", List.of(YearMonth.of(2026, 1)),
                    "date", List.of(LocalDate.of(2026, 1, 1)),
                    "dateTime", List.of(LocalDateTime.of(2026, 1, 1, 12, 30)),
                    "time", List.of(LocalTime.of(12, 30)),
                    "invalidTime", List.of("25:99"),
                    "integer", List.of(42),
                    "decimal", List.of(1.5));
            String[] keys = {"yearMonth", "date", "dateTime", "time", "invalidTime", "integer", "decimal"};
            ValueType[] types = {ValueType.YEAR_MONTH, ValueType.DATE, ValueType.DATE_TIME,
                    ValueType.TIME, ValueType.STRING, ValueType.INTEGER, ValueType.DECIMAL};
            Object[] tags = new Object[keys.length];
            var row = sheet.createRow(0);
            for (int i = 0; i < keys.length; i++) {
                row.createCell(i);
                tags[i] = "$R[]{" + keys[i] + "}";
            }
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), values,
                    "data", "headers", "footers", keys.length, tags);
            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            try (XSSFWorkbook saved = roundTrip(workbook)) {
                for (int i = 0; i < keys.length; i++) {
                    XSSFCellStyle style = saved.getSheetAt(0).getRow(0).getCell(i).getCellStyle();
                    assertEquals(types[i].getFormat(saved), style.getDataFormat(), keys[i]);
                    CTPatternFill pattern = patternFill(saved, style);
                    assertEquals(STPatternType.NONE, pattern.getPatternType(), keys[i]);
                    assertFalse(pattern.isSetFgColor(), keys[i]);
                    assertFalse(pattern.isSetBgColor(), keys[i]);
                }
            }
        }
    }

    @Test
    void unchangedXssfStyleIsNotCopiedWhenItsFillIsValid() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setDataFormat(ValueType.DATE.getFormat(workbook));
            source.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            source.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            Sheet sheet = workbook.createSheet("Sheet");
            Cell cell = sheet.createRow(0).createCell(0);
            cell.setCellStyle(source);
            short sourceStyleIndex = cell.getCellStyle().getIndex();
            int styleCountBeforeUpdate = workbook.getNumCellStyles();

            update(workbook, sheet, List.of(LocalDate.of(2026, 1, 1)));

            assertEquals(sourceStyleIndex, cell.getCellStyle().getIndex());
            assertEquals(styleCountBeforeUpdate + ValueType.values().length, workbook.getNumCellStyles());
            assertEquals(FillPatternType.SOLID_FOREGROUND, cell.getCellStyle().getFillPattern());
        }
    }

    @Test
    void indentationUpdateKeepsTheFillSafeAndReusesStyles() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setDataFormat(ValueType.DATE.getFormat(workbook));
            CTFill fill = CTFill.Factory.newInstance();
            fill.addNewPatternFill();
            source.getCoreXf().setFillId(workbook.getStylesSource().putFill(
                    new XSSFCellFill(fill, workbook.getStylesSource().getIndexedColors())));
            Sheet sheet = workbook.createSheet("Sheet");
            Cell first = sheet.createRow(0).createCell(0);
            Cell second = sheet.createRow(1).createCell(0);
            first.setCellStyle(source);
            second.setCellStyle(source);
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), Map.of(),
                    "data", "headers", "footers", 1, new Object[0]);
            listener.preBookParse(workbook, null);

            listener.setCellIndentation(first, (short) 2);
            listener.setCellIndentation(second, (short) 2);

            assertEquals(first.getCellStyle().getIndex(), second.getCellStyle().getIndex());
            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle result = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                assertEquals(2, result.getIndention());
                assertEquals(ValueType.DATE.getFormat(saved), result.getDataFormat());
                assertEquals(STPatternType.NONE, patternFill(saved, result).getPatternType());
                assertFalse(patternFill(saved, result).isSetFgColor());
                assertFalse(patternFill(saved, result).isSetBgColor());
            }
        }
    }

    @Test
    void xlsKeepsFillAndOtherStyleProperties() throws Exception {
        try (Workbook workbook = new HSSFWorkbook()) {
            CellStyle source = workbook.createCellStyle();
            source.setFillForegroundColor(IndexedColors.RED.getIndex());
            source.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            source.setBorderTop(BorderStyle.THIN);
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0).setCellStyle(source);

            update(workbook, sheet, List.of(42));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            try (HSSFWorkbook saved = new HSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
                CellStyle result = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                assertEquals(ValueType.INTEGER.getFormat(saved), result.getDataFormat());
                assertEquals(FillPatternType.SOLID_FOREGROUND, result.getFillPattern());
                assertEquals(IndexedColors.RED.getIndex(), result.getFillForegroundColor());
                assertEquals(BorderStyle.THIN, result.getBorderTop());
            }
        }
    }

    @Test
    void unchangedHssfStylesDoNotConsumeTheStyleLimit() throws Exception {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            List<Object> values = new ArrayList<>();
            int sourceStyleCount = 3900;
            for (int i = 0; i < sourceStyleCount; i++) {
                CellStyle source = workbook.createCellStyle();
                source.setDataFormat(ValueType.INTEGER.getFormat(workbook));
                sheet.createRow(i).createCell(0).setCellStyle(source);
                values.add(i);
            }
            short firstStyleIndex = sheet.getRow(0).getCell(0).getCellStyle().getIndex();
            int styleCountBeforeUpdate = workbook.getNumCellStyles();

            update(workbook, sheet, values);

            assertEquals(styleCountBeforeUpdate + ValueType.values().length, workbook.getNumCellStyles());
            assertEquals(firstStyleIndex, sheet.getRow(0).getCell(0).getCellStyle().getIndex());
        }
    }

    @Test
    void changedHssfStylesReuseEquivalentStylesNearTheStyleLimit() throws Exception {
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < 3900; i++) {
                CellStyle source = workbook.createCellStyle();
                source.setBorderTop(i == 3899 ? BorderStyle.THICK : BorderStyle.THIN);
                sheet.createRow(i).createCell(0).setCellStyle(source);
                values.add(i);
            }
            int styleCountBeforeUpdate = workbook.getNumCellStyles();

            update(workbook, sheet, values);

            CellStyle first = sheet.getRow(0).getCell(0).getCellStyle();
            CellStyle second = sheet.getRow(1).getCell(0).getCellStyle();
            CellStyle last = sheet.getRow(3899).getCell(0).getCellStyle();
            assertEquals(first.getIndex(), second.getIndex());
            assertEquals(BorderStyle.THIN, first.getBorderTop());
            assertEquals(BorderStyle.THICK, last.getBorderTop());
            assertNotEquals(first.getIndex(), last.getIndex());
            assertEquals(ValueType.INTEGER.getFormat(workbook), first.getDataFormat());
            assertEquals(ValueType.INTEGER.getFormat(workbook), last.getDataFormat());
            assertTrue(workbook.getNumCellStyles() <= styleCountBeforeUpdate + ValueType.values().length + 2);
        }
    }

    @Test
    void hssfIndentationReusesAnExistingEquivalentStyle() throws Exception {
        try (Workbook workbook = new HSSFWorkbook()) {
            CellStyle source = workbook.createCellStyle();
            source.setBorderTop(BorderStyle.THIN);
            CellStyle existing = workbook.createCellStyle();
            existing.cloneStyleFrom(source);
            existing.setIndention((short) 2);
            Sheet sheet = workbook.createSheet("Sheet");
            Cell cell = sheet.createRow(0).createCell(0);
            cell.setCellStyle(source);
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), Map.of(),
                    "data", "headers", "footers", 1, new Object[0]);
            listener.preBookParse(workbook, null);
            int styleCountBeforeUpdate = workbook.getNumCellStyles();

            listener.setCellIndentation(cell, (short) 2);

            assertEquals(existing.getIndex(), cell.getCellStyle().getIndex());
            assertEquals(styleCountBeforeUpdate, workbook.getNumCellStyles());
            assertEquals(BorderStyle.THIN, cell.getCellStyle().getBorderTop());
        }
    }

    @Test
    void equivalentXssfSourcesShareOneNormalizedTargetStyle() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                XSSFCellStyle source = workbook.createCellStyle();
                source.setWrapText(true);
                source.setQuotePrefixed(i == 199);
                sheet.createRow(i).createCell(0).setCellStyle(source);
                values.add(i);
            }
            int before = workbook.getNumCellStyles();

            update(workbook, sheet, values);

            assertEquals(before + ValueType.values().length + 2, workbook.getNumCellStyles());
            try (XSSFWorkbook saved = roundTrip(workbook)) {
                XSSFCellStyle first = saved.getSheetAt(0).getRow(0).getCell(0).getCellStyle();
                XSSFCellStyle second = saved.getSheetAt(0).getRow(1).getCell(0).getCellStyle();
                XSSFCellStyle last = saved.getSheetAt(0).getRow(199).getCell(0).getCellStyle();
                assertEquals(first.getIndex(), second.getIndex());
                assertNotEquals(first.getIndex(), last.getIndex());
                assertFalse(first.getQuotePrefixed());
                assertTrue(last.getQuotePrefixed());
                assertTrue(first.getWrapText());
                assertEquals(ValueType.INTEGER.getFormat(saved), first.getDataFormat());
                assertEquals(STPatternType.NONE, patternFill(saved, first).getPatternType());
            }
        }
    }

    @Test
    void xssfUpdateReusesAnExistingNormalizedTargetStyle() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFCellStyle source = workbook.createCellStyle();
            source.setWrapText(true);
            XSSFCellStyle target = workbook.createCellStyle();
            target.getCoreXf().set(source.getCoreXf());
            target.setDataFormat(ValueType.INTEGER.getFormat(workbook));
            CTFill fill = CTFill.Factory.newInstance();
            fill.addNewPatternFill().setPatternType(STPatternType.NONE);
            target.getCoreXf().setFillId(workbook.getStylesSource().putFill(
                    new XSSFCellFill(fill, workbook.getStylesSource().getIndexedColors())));
            Sheet sheet = workbook.createSheet("Sheet");
            Cell cell = sheet.createRow(0).createCell(0);
            cell.setCellStyle(source);
            int before = workbook.getNumCellStyles();

            update(workbook, sheet, List.of(42));

            assertEquals(target.getIndex(), cell.getCellStyle().getIndex());
            assertEquals(before + ValueType.values().length, workbook.getNumCellStyles());
        }
    }

    private static void update(Workbook workbook, Sheet sheet, List<Object> values) {
        ReportSheet reportSheet = new ReportSheet("Sheet");
        StyleUpdateListener listener = new StyleUpdateListener(reportSheet, Map.of("value", values),
                "data", "headers", "footers", 1, new Object[] {"$R[]{value}"});
        listener.preBookParse(workbook, null);
        listener.postParse(sheet, null, new SheetData("Sheet"));
    }

    private static CTPatternFill patternFill(XSSFWorkbook workbook, XSSFCellStyle style) {
        return workbook.getStylesSource().getFillAt((int) style.getCoreXf().getFillId()).getCTFill().getPatternFill();
    }

    private static XSSFWorkbook roundTrip(XSSFWorkbook workbook) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()));
    }
}
