package net.bis5.excella.primefaces.exporter.datatable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.poi.EncryptedDocumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
public class DataTableNoDataTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    private void assertNoFile(String outputFileName) {
        var path = Paths.get(getBaseDir(), "/docker-compose/downloads/" + outputFileName);
        assertFalse(path.toFile().exists(), () -> "File " + path + " was unexpectedly created");
    }

    private void assertFileExists(String outputFileName) {
        var path = Paths.get(getBaseDir(), "/docker-compose/downloads/" + outputFileName);
        assertTrue(path.toFile().exists(), () -> "File " + path + " is not found");
    }

    @Test
    void exportExcellaAjaxAll(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkAjaxAll;
        link.click();
        PrimeSelenium.wait(1000);

        assertNoFile("no-data-ajax-all.xlsx");
    }

    @Test
    void exportExcellaAjaxSelectionOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkAjaxSelection;
        link.click();
        PrimeSelenium.wait(1000);

        assertNoFile("no-data-ajax-selection.xlsx");
    }

    @Test
    void exportExcellaAjaxPageOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkAjaxPage;
        link.click();
        PrimeSelenium.wait(1000);

        assertNoFile("no-data-ajax-page.xlsx");
    }

    @Test
    void exportExcellaNonAjaxAll(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkNonAjaxAll;
        link.click();
        PrimeSelenium.wait(1000);

        assertNoFile("no-data-non-ajax-all.xlsx");
    }

    @Test
    void exportExcellaNonAjaxSelectionOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkNonAjaxSelection;
        link.click();
        PrimeSelenium.wait(1000);

        assertNoFile("no-data-non-ajax-selection.xlsx");
    }

    @Test
    void exportExcellaNonAjaxPageOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkNonAjaxPage;
        link.click();
        PrimeSelenium.wait(1000);

        assertNoFile("no-data-non-ajax-page.xlsx");
    }

    // -------------

    @Test
    void exportExcellaAjaxNoExceptionAll(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkAjaxNoExceptionAll;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileExists("no-data-ajax-no-exception-all.xlsx");
    }

    @Test
    void exportExcellaAjaxNoExceptionSelectionOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkAjaxNoExceptionSelection;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileExists("no-data-ajax-no-exception-selection.xlsx");
    }

    @Test
    void exportExcellaAjaxNoExceptionPageOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkAjaxNoExceptionPage;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileExists("no-data-ajax-no-exception-page.xlsx");
    }

    @Test
    void exportExcellaNonAjaxNoExceptionAll(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkNonAjaxNoExceptionAll;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileExists("no-data-non-ajax-no-exception-all.xlsx");
    }

    @Test
    void exportExcellaNonAjaxNoExceptionSelectionOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkNonAjaxNoExceptionSelection;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileExists("no-data-non-ajax-no-exception-selection.xlsx");
    }

    @Test
    void exportExcellaNonAjaxNoExceptionPageOnly(NoDataPage page) throws EncryptedDocumentException, IOException {

        CommandLink link = page.commandLinkNonAjaxNoExceptionPage;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileExists("no-data-non-ajax-no-exception-page.xlsx");
    }

    public static class NoDataPage extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjaxAll")
        CommandLink commandLinkNonAjaxAll;

        @FindBy(id = "form:excellaExportAjaxAll")
        CommandLink commandLinkAjaxAll;

        @FindBy(id = "form:excellaExportNonAjaxSelection")
        CommandLink commandLinkNonAjaxSelection;

        @FindBy(id = "form:excellaExportAjaxSelection")
        CommandLink commandLinkAjaxSelection;

        @FindBy(id = "form:excellaExportNonAjaxPage")
        CommandLink commandLinkNonAjaxPage;

        @FindBy(id = "form:excellaExportAjaxPage")
        CommandLink commandLinkAjaxPage;

        @FindBy(id = "form:excellaExportNonAjaxNoExceptionAll")
        CommandLink commandLinkNonAjaxNoExceptionAll;

        @FindBy(id = "form:excellaExportAjaxNoExceptionAll")
        CommandLink commandLinkAjaxNoExceptionAll;

        @FindBy(id = "form:excellaExportNonAjaxNoExceptionSelection")
        CommandLink commandLinkNonAjaxNoExceptionSelection;

        @FindBy(id = "form:excellaExportAjaxNoExceptionSelection")
        CommandLink commandLinkAjaxNoExceptionSelection;

        @FindBy(id = "form:excellaExportNonAjaxNoExceptionPage")
        CommandLink commandLinkNonAjaxNoExceptionPage;

        @FindBy(id = "form:excellaExportAjaxNoExceptionPage")
        CommandLink commandLinkAjaxNoExceptionPage;

        @Override
        public String getLocation() {
            return "ui/data/datatable/noData.xhtml";
        }

    }
}
