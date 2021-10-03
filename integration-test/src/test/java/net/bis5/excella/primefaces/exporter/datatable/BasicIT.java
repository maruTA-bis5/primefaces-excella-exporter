package net.bis5.excella.primefaces.exporter.datatable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.datatable.BasicView;
import org.primefaces.showcase.view.data.datatable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
public class BasicIT extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    private <T> void assertCell(String description, Cell cell, CellType expectedType, T expectedValue, Function<Cell, T> actualValueMapper) {
        assertAll(description,
            () -> assertEquals(expectedType, cell.getCellType(), "cell type"),
            () -> assertEquals(expectedValue, actualValueMapper.apply(cell), "cell value")
        );
    }
    @Test
    public void exportExcella(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLink;
        link.getRoot().click();

        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/cars.xlsx"), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = List.of("String", "YearMonth", "j.u.Date (date)", "j.u.Date (datetime)", "LocalDate", "LocalDateTime", "Integer (int)", "Integer (BigDecimal scale=2)", "Decimal (double)", "Decimal (BigDecimal)", "Link (value specified)", "Link (value not specified)");

            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(1);
            assertAll(
                () -> assertAll("Header row", () -> {
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.getCell(i);
                        assertEquals(CellType.STRING, cell.getCellType());
                        assertEquals(headers.get(i), cell.getStringCellValue());
                    }
                }),
                () -> assertAll("Data row",
                    () -> assertCell("String cell", dataRow.getCell(0), CellType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(1), CellType.NUMERIC, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", dataRow.getCell(2), CellType.NUMERIC, record.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", dataRow.getCell(3), CellType.NUMERIC, record.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", dataRow.getCell(4), CellType.NUMERIC, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", dataRow.getCell(5), CellType.NUMERIC, record.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", dataRow.getCell(6), CellType.NUMERIC, Double.valueOf(record.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", dataRow.getCell(7), CellType.NUMERIC, record.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", dataRow.getCell(8), CellType.NUMERIC, record.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", dataRow.getCell(9), CellType.NUMERIC, record.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", dataRow.getCell(10), CellType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", dataRow.getCell(11), CellType.NUMERIC, record.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue)
                )
            );
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExport")
        CommandLink commandLink;

        @Override
        public String getLocation() {
            return "ui/data/datatable/basic.xhtml";
        }

    }

}
