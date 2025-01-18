package net.bis5.excella.primefaces.exporter.datatable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;

import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.support.FindBy;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.datatable.BasicView;
import org.primefaces.showcase.view.data.datatable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
class DataBeforeWriteResponseListenerTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws IOException, GeneralSecurityException {
        BasicView backingBean = new BasicView();
        DataTypeCheck data = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkAjax;
        link.click();
        PrimeSelenium.wait(1000);

        assertFileContent(data, "encrypted-cars-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws IOException, GeneralSecurityException {
        BasicView backingBean = new BasicView();
        DataTypeCheck data = backingBean.getDataTypes().get(0);

        CommandLink link = page.commandLinkNonAjax;
        link.getRoot().click();

        assertFileContent(data, "encrypted-cars-non-ajax.xlsx");
    }

    private void assertFileContent(DataTypeCheck data, String outputFileName) throws IOException, GeneralSecurityException {
        try (InputStream is = Files.newInputStream(Paths.get(getBaseDir() + "/docker-compose/downloads/" + outputFileName), StandardOpenOption.READ);
                POIFSFileSystem fs = new POIFSFileSystem(is)) {
            EncryptionInfo info = new EncryptionInfo(fs);
            Decryptor dc = Decryptor.getInstance(info);
            assertTrue(dc.verifyPassword("key"), "could not decrypt");

            // verify content
            new DataBasicTest().assertFileContent(data, WorkbookFactory.create(dc.getDataStream(fs)));
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjax")
        CommandLink commandLinkNonAjax;

        @FindBy(id = "form:excellaExportAjax")
        CommandLink commandLinkAjax;

        @Override
        public String getLocation() {
            return "ui/data/datatable/beforeWriteResponse.xhtml";
        }

    }

}
