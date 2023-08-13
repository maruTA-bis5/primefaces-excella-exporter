package net.bis5.excella.primefaces.exporter.treetable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.primefaces.model.TreeNode;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.treetable.BasicView;
import org.primefaces.showcase.view.data.treetable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;
import net.bis5.excella.primefaces.exporter.ValueType;

@ExtendWith(TakeScreenShotAfterFailure.class)
class TreeBasicTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-cars-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord1 = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord1 = (DataTypeCheck) childNode.getData();
        DataTypeCheck parentRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getData();
        DataTypeCheck childRecord2 = (DataTypeCheck) backingBean.getRoot().getChildren().get(1).getChildren().get(0).getData();

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-cars-non-ajax.xlsx");
    }

    private void assertFileContent(DataTypeCheck parentRecord1, DataTypeCheck childRecord1, DataTypeCheck parentRecord2, DataTypeCheck childRecord2, String fileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + fileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = Arrays.asList(
                "String",
                "YearMonth",
                "j.u.Date (date)",
                "j.u.Date (datetime)",
                "LocalDate",
                "LocalDateTime",
                "Integer (int)",
                "Integer (BigDecimal scale=2)",
                "Decimal (double)",
                "Decimal (BigDecimal)",
                "Link (value specified)",
                "Link (value not specified)",
                "header line break",
                "exportable component",
                "exportable component(value null)"
            );

            Row headerRow = sheet.getRow(0);
            Row parentNodeRow1 = sheet.getRow(1);
            Row childNodeRow2 = sheet.getRow(2);
            Row parentNodeRow3 = sheet.getRow(3);
            Row childNodeRow4 = sheet.getRow(4);
            Row footerRow = sheet.getRow(5);

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
                () -> assertAll("Parent node row 1",
                    () -> assertEquals(0, parentNodeRow1.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", parentNodeRow1.getCell(0), CellType.STRING, ValueType.STRING, parentRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNodeRow1.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, parentRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", parentNodeRow1.getCell(2), CellType.NUMERIC, ValueType.DATE, parentRecord1.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", parentNodeRow1.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, parentRecord1.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", parentNodeRow1.getCell(4), CellType.NUMERIC, ValueType.DATE, parentRecord1.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", parentNodeRow1.getCell(5), CellType.NUMERIC, ValueType.DATE_TIME, parentRecord1.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", parentNodeRow1.getCell(6), CellType.NUMERIC, ValueType.INTEGER, Double.valueOf(parentRecord1.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", parentNodeRow1.getCell(7), CellType.NUMERIC, ValueType.INTEGER, parentRecord1.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", parentNodeRow1.getCell(8), CellType.NUMERIC, ValueType.DECIMAL, parentRecord1.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", parentNodeRow1.getCell(9), CellType.NUMERIC, ValueType.DECIMAL, parentRecord1.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", parentNodeRow1.getCell(10), CellType.STRING, ValueType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", parentNodeRow1.getCell(11), CellType.NUMERIC, ValueType.DECIMAL, parentRecord1.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("remove br tag", parentNodeRow1.getCell(12), CellType.STRING, ValueType.STRING, "value row line break", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder", parentNodeRow1.getCell(13), CellType.STRING, ValueType.STRING, "Export text1", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder(value null)", parentNodeRow1.getCell(14), CellType.STRING, ValueType.STRING, "Export text2", Cell::getStringCellValue)
                ),
                () -> assertAll("Child node row 2",
                    () -> assertEquals(1, childNodeRow2.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", childNodeRow2.getCell(0), CellType.STRING, ValueType.STRING, childRecord1.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNodeRow2.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord1.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", childNodeRow2.getCell(2), CellType.NUMERIC, ValueType.DATE, childRecord1.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", childNodeRow2.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, childRecord1.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", childNodeRow2.getCell(4), CellType.NUMERIC, ValueType.DATE, childRecord1.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", childNodeRow2.getCell(5), CellType.NUMERIC, ValueType.DATE_TIME, childRecord1.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", childNodeRow2.getCell(6), CellType.NUMERIC, ValueType.INTEGER, Double.valueOf(childRecord1.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", childNodeRow2.getCell(7), CellType.NUMERIC, ValueType.INTEGER, childRecord1.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", childNodeRow2.getCell(8), CellType.NUMERIC, ValueType.DECIMAL, childRecord1.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", childNodeRow2.getCell(9), CellType.NUMERIC, ValueType.DECIMAL, childRecord1.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", childNodeRow2.getCell(10), CellType.STRING, ValueType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", childNodeRow2.getCell(11), CellType.NUMERIC, ValueType.DECIMAL, childRecord1.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("remove br tag", childNodeRow2.getCell(12), CellType.STRING, ValueType.STRING, "value row line break", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder", childNodeRow2.getCell(13), CellType.STRING, ValueType.STRING, "Export text1", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder(value null)", childNodeRow2.getCell(14), CellType.STRING, ValueType.STRING, "Export text2", Cell::getStringCellValue)
                ),
                () -> assertAll("Parent node row 3",
                    () -> assertEquals(0, parentNodeRow3.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", parentNodeRow3.getCell(0), CellType.STRING, ValueType.STRING, parentRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNodeRow3.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, parentRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", parentNodeRow3.getCell(2), CellType.NUMERIC, ValueType.DATE, parentRecord2.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", parentNodeRow3.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, parentRecord2.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", parentNodeRow3.getCell(4), CellType.NUMERIC, ValueType.DATE, parentRecord2.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", parentNodeRow3.getCell(5), CellType.NUMERIC, ValueType.DATE_TIME, parentRecord2.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", parentNodeRow3.getCell(6), CellType.NUMERIC, ValueType.INTEGER, Double.valueOf(parentRecord2.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", parentNodeRow3.getCell(7), CellType.NUMERIC, ValueType.INTEGER, parentRecord2.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", parentNodeRow3.getCell(8), CellType.NUMERIC, ValueType.DECIMAL, parentRecord2.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", parentNodeRow3.getCell(9), CellType.NUMERIC, ValueType.DECIMAL, parentRecord2.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", parentNodeRow3.getCell(10), CellType.STRING, ValueType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", parentNodeRow3.getCell(11), CellType.NUMERIC, ValueType.DECIMAL, parentRecord2.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("remove br tag", parentNodeRow3.getCell(12), CellType.STRING, ValueType.STRING, "value row line break", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder", parentNodeRow3.getCell(13), CellType.STRING, ValueType.STRING, "Export text1", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder(value null)", parentNodeRow3.getCell(14), CellType.STRING, ValueType.STRING, "Export text2", Cell::getStringCellValue)
                ),
                () -> assertAll("Child node row 4",
                    () -> assertEquals(1, childNodeRow4.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", childNodeRow4.getCell(0), CellType.STRING, ValueType.STRING, childRecord2.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNodeRow4.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, childRecord2.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", childNodeRow4.getCell(2), CellType.NUMERIC, ValueType.DATE, childRecord2.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", childNodeRow4.getCell(3), CellType.NUMERIC, ValueType.DATE_TIME, childRecord2.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", childNodeRow4.getCell(4), CellType.NUMERIC, ValueType.DATE, childRecord2.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", childNodeRow4.getCell(5), CellType.NUMERIC, ValueType.DATE_TIME, childRecord2.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", childNodeRow4.getCell(6), CellType.NUMERIC, ValueType.INTEGER, Double.valueOf(childRecord2.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", childNodeRow4.getCell(7), CellType.NUMERIC, ValueType.INTEGER, childRecord2.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", childNodeRow4.getCell(8), CellType.NUMERIC, ValueType.DECIMAL, childRecord2.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", childNodeRow4.getCell(9), CellType.NUMERIC, ValueType.DECIMAL, childRecord2.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", childNodeRow4.getCell(10), CellType.STRING, ValueType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", childNodeRow4.getCell(11), CellType.NUMERIC, ValueType.DECIMAL, childRecord2.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("remove br tag", childNodeRow4.getCell(12), CellType.STRING, ValueType.STRING, "value row line break", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder", childNodeRow4.getCell(13), CellType.STRING, ValueType.STRING, "Export text1", Cell::getStringCellValue),
                    () -> assertCell("exportable value holder(value null)", childNodeRow4.getCell(14), CellType.STRING, ValueType.STRING, "Export text2", Cell::getStringCellValue)
                ),
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = footerRow.getCell(i);
                        String expectedHeaderValue = headers.get(i);
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
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
            return "ui/data/treetable/basic.xhtml";
        }

    }

}
