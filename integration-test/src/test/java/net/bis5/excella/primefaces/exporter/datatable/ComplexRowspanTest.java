package net.bis5.excella.primefaces.exporter.datatable;

import static net.bis5.excella.primefaces.exporter.Assertions.assertCell;
import static net.bis5.excella.primefaces.exporter.Assertions.assertHeaderCell;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
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
class ComplexRowspanTest extends AbstractPrimePageTest {

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

        assertFileContent(record, "complex-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws EncryptedDocumentException, IOException {
        BasicView backingBean = new BasicView();
        DataTypeCheck record = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(record, "complex-non-ajax.xlsx");
    }

    private void assertMergedRegion(Sheet sheet, int fromRowIndex, int fromColIndex, int toRowIndex, int toColIndex) {
        CellRangeAddress expectedRange = new CellRangeAddress(fromRowIndex, toRowIndex, fromColIndex, toColIndex);
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        assertTrue(mergedRegions.contains(expectedRange), () -> "Cell range [" + expectedRange + "] is not merged. merged regions: " + mergedRegions);
    }

    private void assertFileContent(DataTypeCheck record, String outputFileName) throws EncryptedDocumentException, IOException {
        try (Workbook workbook = WorkbookFactory.create(new File(getBaseDir()+"/docker-compose/downloads/" + outputFileName), null, true)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<String> headerRowAText = List.of("rowspan", "headerText A-1", "headerText A-2", "rowspan2");
            List<String> headerRowBText = List.of("", "headerText B-1", "headerText B-2", "");
            List<String> headerRowCText = List.of("", "headerText C-1", "headerText C-2", "");

            Row headerRowA = sheet.getRow(0);
            Row headerRowB = sheet.getRow(1);
            Row headerRowC = sheet.getRow(2);
            Row dataRow = sheet.getRow(3);
            assertAll(
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    assertions.add(() -> assertMergedRegion(sheet, 0, 0, 2, 0));
                    assertions.add(() -> assertMergedRegion(sheet, 0, 3, 2, 3));
                    for (int i = 0; i < headerRowAText.size(); i++) {
                        Cell cell = headerRowA.getCell(i);
                        String expectedHeaderValue = headerRowAText.get(i);
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
                    }
                    assertAll("headerRowA", assertions.toArray(Executable[]::new));
                },
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headerRowBText.size(); i++) {
                        Cell cell = headerRowB.getCell(i);
                        String expectedHeaderValue = headerRowBText.get(i);
                        if (expectedHeaderValue.isEmpty()) {
                            assertions.add(() -> assertTrue(cell == null || cell.getCellType() == CellType.BLANK,
                                () -> "merged area cell type must be blank. observed: " + cell.getCellType() + ", value: " + cell.getStringCellValue()));
                            continue;
                        }
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
                    }
                    assertAll("headerRowB", assertions.toArray(Executable[]::new));
                },
                () -> {
                    List<Executable> assertions = new ArrayList<>();
                    for (int i = 0; i < headerRowCText.size(); i++) {
                        Cell cell = headerRowC.getCell(i);
                        String expectedHeaderValue = headerRowCText.get(i);
                        if (expectedHeaderValue.isEmpty()) {
                            assertions.add(() -> assertTrue(cell == null || cell.getCellType() == CellType.BLANK,
                                () -> "merged area cell type must be blank. observed: " + cell.getCellType() + ", value: " + cell.getStringCellValue()));
                            continue;
                        }
                        var index = i;
                        assertions.add(() -> assertHeaderCell(index, cell, expectedHeaderValue));
                    }
                    assertAll("headerRowC", assertions.toArray(Executable[]::new));
                },
                () -> assertAll("Data row",
                    () -> assertCell("String cell", dataRow.getCell(0), CellType.STRING, ValueType.STRING, record.getStringProperty(), Cell::getStringCellValue),
                    () -> assertCell("YearMonth cell", dataRow.getCell(1), CellType.NUMERIC, ValueType.YEAR_MONTH, record.getYearMonthProperty().atDay(1).atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("LocalDate cell", dataRow.getCell(2), CellType.NUMERIC, ValueType.DATE, record.getLocalDateProperty().atStartOfDay(), Cell::getLocalDateTimeCellValue),
                    () -> assertCell("String cell 2", dataRow.getCell(3), CellType.STRING, ValueType.STRING, record.getStringProperty(), Cell::getStringCellValue)
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
            return "ui/data/datatable/complexRowspan.xhtml";
        }

    }

}
