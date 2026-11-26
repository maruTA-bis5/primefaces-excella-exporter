package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
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
    void appliesTimeFormatOnlyToStringsWithValidMinutes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            sheet.createRow(0).createCell(0);
            sheet.getRow(0).createCell(1);
            Map<String, List<Object>> values = Map.of(
                    "validTime", List.of("24:01"),
                    "invalidTime", List.of("25:99"));
            Object[] tags = {"$R[]{validTime}", "$R[]{invalidTime}"};
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"), values,
                    "data", "headers", "footers", tags.length, tags);

            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            assertEquals(ValueType.TIME.getFormat(workbook), sheet.getRow(0).getCell(0).getCellStyle().getDataFormat());
            assertEquals(ValueType.STRING.getFormat(workbook), sheet.getRow(0).getCell(1).getCellStyle().getDataFormat());
        }
    }
}
