package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.reports.model.ReportSheet;
import org.junit.jupiter.api.Test;

class StyleUpdateListenerTest {

    @Test
    void appliesElapsedTimeFormatOnlyToValidTimeValues() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            Row row = sheet.createRow(0);
            var stringTimeCell = row.createCell(0);
            var localTimeCell = row.createCell(1);
            var invalidTimeCell = row.createCell(2);
            Map<String, List<Object>> values = Map.of(
                    "stringTime", List.of("24:01"),
                    "localTime", List.of(LocalTime.of(23, 59)),
                    "invalidTime", List.of("25:99"));
            Object[] columnDataParams = {
                    "$R[]{stringTime}",
                    "$R[]{localTime}",
                    "$R[]{invalidTime}"
            };
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), values,
                    "data", "headers", "footers", columnDataParams.length, columnDataParams);
            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            assertAll("time formats",
                    () -> assertEquals("[h]:mm", stringTimeCell.getCellStyle().getDataFormatString()),
                    () -> assertEquals("[h]:mm", localTimeCell.getCellStyle().getDataFormatString()),
                    () -> assertEquals("General", invalidTimeCell.getCellStyle().getDataFormatString()));
        }
    }
}
