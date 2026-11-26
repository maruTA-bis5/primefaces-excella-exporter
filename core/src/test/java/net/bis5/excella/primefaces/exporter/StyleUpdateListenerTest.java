package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.reports.model.ReportSheet;
import org.junit.jupiter.api.Test;

class StyleUpdateListenerTest {

    @Test
    void recognizesOnlyValidTimeStringsAsTimes() throws IOException {
        Map<String, List<Object>> data = new LinkedHashMap<>();
        data.put("validTime", List.of("1:00"));
        data.put("invalidTime", List.of("25:99"));

        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            Row row = sheet.createRow(0);
            row.createCell(0);
            row.createCell(1);

            Object[] columnDataParams = data.keySet().stream()
                .map(key -> "$R[]{" + key + "}")
                .toArray();
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), data,
                "data", "headers", "footers", data.size(), columnDataParams);
            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            assertEquals("[h]:mm", row.getCell(0).getCellStyle().getDataFormatString());
            assertEquals("General", row.getCell(1).getCellStyle().getDataFormatString());
        }
    }
}
