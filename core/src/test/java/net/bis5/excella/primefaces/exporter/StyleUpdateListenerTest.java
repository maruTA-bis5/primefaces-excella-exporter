package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.reports.model.ReportSheet;
import org.junit.jupiter.api.Test;

class StyleUpdateListenerTest {

    @Test
    void appliesTimeFormatOnlyToValidTimeValues() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            var row = sheet.createRow(0);
            row.createCell(0);
            row.createCell(1);
            row.createCell(2);

            Map<String, List<Object>> values = Map.of(
                    "localTime", List.of(LocalTime.of(12, 30)),
                    "validTime", List.of("24:01"),
                    "invalidTime", List.of("25:99"));
            Object[] tags = {"$R[]{localTime}", "$R[]{validTime}", "$R[]{invalidTime}"};

            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), values,
                    "data", "headers", "footers", tags.length, tags);
            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            assertEquals(ValueType.TIME.getFormat(workbook), row.getCell(0).getCellStyle().getDataFormat());
            assertEquals(ValueType.TIME.getFormat(workbook), row.getCell(1).getCellStyle().getDataFormat());
            assertEquals(ValueType.STRING.getFormat(workbook), row.getCell(2).getCellStyle().getDataFormat());
        }
    }
}
