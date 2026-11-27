package net.bis5.excella.primefaces.exporter.treetable;

import static net.bis5.excella.primefaces.exporter.PasswordExportAssertions.assertExport;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;

import net.bis5.excella.primefaces.exporter.Download;
import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
class TreeTablePasswordExportTest extends AbstractPrimePageTest {

    @Test
    void exportsAllRowsWithEachPasswordPolicy(Page page) throws IOException {
        clickAndAssertExport(page.allDefault, "password-treetable-all-default.xlsx", 3, false, "", " ");
        clickAndAssertExport(page.allMasked, "password-treetable-all-masked.xlsx", 3, true, "", " | ");
        clickAndAssertExport(page.allCustom, "password-treetable-all-custom.xlsx", 3, true, "MASKED", " | ");
    }

    @Test
    void exportsCurrentPageWithEachPasswordPolicy(Page page) throws IOException {
        clickAndAssertExport(page.pageDefault, "password-treetable-page-default.xlsx", 1, false, "", " ");
        clickAndAssertExport(page.pageMasked, "password-treetable-page-masked.xlsx", 1, true, "", " | ");
        clickAndAssertExport(page.pageCustom, "password-treetable-page-custom.xlsx", 1, true, "MASKED", " | ");
    }

    @Test
    void exportsSelectedRowsWithEachPasswordPolicy(Page page) throws IOException {
        page.firstNode.click();
        PrimeSelenium.wait(1000);

        clickAndAssertExport(page.selectionDefault, "password-treetable-selection-default.xlsx", 1, false, "", " ");
        clickAndAssertExport(page.selectionMasked, "password-treetable-selection-masked.xlsx", 1, true, "", " | ");
        clickAndAssertExport(page.selectionCustom, "password-treetable-selection-custom.xlsx", 1, true, "MASKED", " | ");
    }

    private void clickAndAssertExport(CommandLink link, String fileName, int dataRowCount,
            boolean masked, String replacement, String separator) throws IOException {
        link.getRoot().click();
        Path localFile = Download.downloadFileToLocal(fileName);
        try (Workbook workbook = WorkbookFactory.create(localFile.toFile(), null, true)) {
            assertExport(workbook, "tree", dataRowCount, masked, replacement, separator);
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:allDefault")
        CommandLink allDefault;

        @FindBy(id = "form:allMasked")
        CommandLink allMasked;

        @FindBy(id = "form:allCustom")
        CommandLink allCustom;

        @FindBy(id = "form:pageDefault")
        CommandLink pageDefault;

        @FindBy(id = "form:pageMasked")
        CommandLink pageMasked;

        @FindBy(id = "form:pageCustom")
        CommandLink pageCustom;

        @FindBy(id = "form:selectionDefault")
        CommandLink selectionDefault;

        @FindBy(id = "form:selectionMasked")
        CommandLink selectionMasked;

        @FindBy(id = "form:selectionCustom")
        CommandLink selectionCustom;

        @FindBy(id = "form:tbl_node_0")
        WebElement firstNode;

        @Override
        public String getLocation() {
            return "ui/data/treetable/passwordExport.xhtml";
        }
    }
}
