package net.bis5.excella.primefaces.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bbreak.excella.core.SheetData;
import org.bbreak.excella.reports.model.ReportSheet;
import org.junit.jupiter.api.Test;

class StyleUpdateListenerTest {

    @Test
    void invalidMinuteStringDoesNotReceiveTimeFormat() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet");
            Cell cell = sheet.createRow(0).createCell(0);
            cell.setCellValue("25:99");
            StyleUpdateListener listener = new StyleUpdateListener(new ReportSheet("Sheet"),
                    Map.of("invalidTime", List.of("25:99")), "data", "headers", "footers", 1,
                    new Object[] {"$R[]{invalidTime}"});
            listener.preBookParse(workbook, null);
            listener.postParse(sheet, null, new SheetData("Sheet"));

            assertEquals("25:99", cell.getStringCellValue());
            assertEquals("General", cell.getCellStyle().getDataFormatString());
            assertNotEquals(ValueType.TIME.getFormat(workbook), cell.getCellStyle().getDataFormat());
        }
    }
}
