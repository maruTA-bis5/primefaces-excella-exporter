package net.bis5.excella.primefaces.exporter.datatable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.datatable.BasicView;
import org.primefaces.showcase.view.data.datatable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
public class DataBasicTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(record, "cars-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(record, "cars-non-ajax.xlsx");
    }

    private void assertFileContent(DataTypeCheck record, String outputFileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = Arrays.asList("String", "YearMonth", "j.u.Date (date)", "j.u.Date (datetime)", "LocalDate", "LocalDateTime", "Integer (int)", "Integer (BigDecimal scale=2)", "Decimal (double)", "Decimal (BigDecimal)", "Link (value specified)", "Link (value not specified)", "header line break");

            Row headerRow = sheet.getRow(0);
            Row dataRow = sheet.getRow(1);
            Row footerRow = sheet.getRow(2);

            assertAll(
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.getCell(i);
                        String expectedHeaderValue = headers.get(i);
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
                    }
                    assertAll("Header row", assertions.toArray(Executable[]::new));
                },
                () -> assertAll("Data row",
                    () -> assertCell("String cell", dataRow.getCell(0), CellType.STRING, ValueType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", dataRow.getCell(2), CellType.NUMERIC, ValueType.DATE, record.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", dataRow.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, record.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", dataRow.getCell(4), CellType.NUMERIC, ValueType.DATE, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", dataRow.getCell(5), CellType.NUMERIC, ValueType.DATE_TIME, record.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", dataRow.getCell(6), CellType.NUMERIC, ValueType.INTEGER, Double.valueOf(record.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", dataRow.getCell(7), CellType.NUMERIC, ValueType.INTEGER, record.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", dataRow.getCell(8), CellType.NUMERIC, ValueType.DECIMAL, record.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", dataRow.getCell(9), CellType.NUMERIC, ValueType.DECIMAL, record.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", dataRow.getCell(10), CellType.STRING, ValueType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", dataRow.getCell(11), CellType.NUMERIC, ValueType.DECIMAL, record.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("remove br tag", dataRow.getCell(12), CellType.STRING, ValueType.STRING, "value row line break", Cell::getStringCellValue)
                ),
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = footerRow.getCell(i);
                        String expectedFooterValue = headers.get(i);
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedFooterValue));
                    }
                    assertAll("Footer row", assertions.toArray(Executable[]::new));
                }
            );
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjax")
        CommandLink commandLinkNonAjax;

        @FindBy(id = "form:excellaExportAjax")
        CommandLink commandLinkAjax;

        @Override
        public String getLocation() {
            return "ui/data/datatable/basic.xhtml";
        }

    }

}
