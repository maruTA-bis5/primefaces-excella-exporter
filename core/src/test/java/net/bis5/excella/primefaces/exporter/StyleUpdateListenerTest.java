package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void appliesTimeFormatOnlyToValidTimeStrings() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            var row = sheet.createRow(0);
            row.createCell(0);
            row.createCell(1);
            Map<String, List<Object>> values = Map.of(
                    "validTime", List.of("1:00"),
                    "invalidTime", List.of("25:99"));
            Object[] columnDataParams = {"$R[]{validTime}", "$R[]{invalidTime}"};
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), values,
                    "data", "headers", "footers", columnDataParams.length, columnDataParams);

            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            assertEquals("[h]:mm", row.getCell(0).getCellStyle().getDataFormatString());
            assertEquals("General", row.getCell(1).getCellStyle().getDataFormatString());
        }
    }
}
