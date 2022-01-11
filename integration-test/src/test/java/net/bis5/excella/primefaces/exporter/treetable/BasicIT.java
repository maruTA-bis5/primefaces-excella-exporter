package net.bis5.excella.primefaces.exporter.treetable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.primefaces.model.TreeNode;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.treetable.BasicView;
import org.primefaces.showcase.view.data.treetable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
class BasicIT extends AbstractPrimePageTest {

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
    void exportExcellaAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        CommandLink link = page.commandLinkAjax;
        link.click();

        assertFileContent(parentRecord, childRecord, "tt-cars-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        backingBean.initialize();
        TreeNode<DataTypeCheck> parentNode = backingBean.getRoot().getChildren().get(0);
        TreeNode<DataTypeCheck> childNode = parentNode.getChildren().get(0);

        DataTypeCheck parentRecord = (DataTypeCheck) parentNode.getData();
        DataTypeCheck childRecord = (DataTypeCheck) childNode.getData();

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(parentRecord, childRecord, "tt-cars-non-ajax.xlsx");
    }

    private void assertFileContent(DataTypeCheck parentRecord, DataTypeCheck childRecord, String fileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + fileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headers = List.of("String", "YearMonth", "j.u.Date (date)", "j.u.Date (datetime)", "LocalDate", "LocalDateTime", "Integer (int)", "Integer (BigDecimal scale=2)", "Decimal (double)", "Decimal (BigDecimal)", "Link (value specified)", "Link (value not specified)");

            Row headerRow = sheet.getRow(0);
            Row parentNodeRow = sheet.getRow(1);
            Row childNodeRow = sheet.getRow(2);
            assertAll(
                () -> assertAll("Header row", () -> {
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = headerRow.getCell(i);
                        assertEquals(CellType.STRING, cell.getCellType());
                        assertEquals(headers.get(i), cell.getStringCellValue());
                    }
                }),
                () -> assertAll("Parent node row",
                    () -> assertEquals(0, parentNodeRow.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", parentNodeRow.getCell(0), CellType.STRING, parentRecord.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", parentNodeRow.getCell(1), CellType.NUMERIC, parentRecord.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", parentNodeRow.getCell(2), CellType.NUMERIC, parentRecord.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", parentNodeRow.getCell(3), CellType.NUMERIC, parentRecord.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", parentNodeRow.getCell(4), CellType.NUMERIC, parentRecord.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", parentNodeRow.getCell(5), CellType.NUMERIC, parentRecord.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", parentNodeRow.getCell(6), CellType.NUMERIC, Double.valueOf(parentRecord.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", parentNodeRow.getCell(7), CellType.NUMERIC, parentRecord.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", parentNodeRow.getCell(8), CellType.NUMERIC, parentRecord.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", parentNodeRow.getCell(9), CellType.NUMERIC, parentRecord.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", parentNodeRow.getCell(10), CellType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", parentNodeRow.getCell(11), CellType.NUMERIC, parentRecord.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue)
                ),
                () -> assertAll("Child node raw",
                    () -> assertEquals(1, childNodeRow.getCell(0).getCellStyle().getIndention(), "indention"),
                    () -> assertCell("String cell", childNodeRow.getCell(0), CellType.STRING, childRecord.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", childNodeRow.getCell(1), CellType.NUMERIC, childRecord.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("Date cell", childNodeRow.getCell(2), CellType.NUMERIC, childRecord.getDateProperty(), Cell::getDateCellValue),
                    () -> assertCell("Date time cell", childNodeRow.getCell(3), CellType.NUMERIC, childRecord.getDateTimeProperty(), Cell::getDateCellValue),
                    () -> assertCell("LocalDate cell", childNodeRow.getCell(4), CellType.NUMERIC, childRecord.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDateTime cell", childNodeRow.getCell(5), CellType.NUMERIC, childRecord.getLocalDateTimeProperty(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("integer cell", childNodeRow.getCell(6), CellType.NUMERIC, Double.valueOf(childRecord.getIntProperty()), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as integer cell", childNodeRow.getCell(7), CellType.NUMERIC, childRecord.getBigDecimalIntProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("decimal cell", childNodeRow.getCell(8), CellType.NUMERIC, childRecord.getDoubleProperty(), Cell::getNumericCellValue),
                    () -> assertCell("BigDecimal as decimal cell", childNodeRow.getCell(9), CellType.NUMERIC, childRecord.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue),
                    () -> assertCell("Link value specified", childNodeRow.getCell(10), CellType.STRING, "Link", Cell::getStringCellValue),
                    () -> assertCell("Link value not specified", childNodeRow.getCell(11), CellType.NUMERIC, childRecord.getBigDecimalDecimalProperty().doubleValue(), Cell::getNumericCellValue)
                )
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
