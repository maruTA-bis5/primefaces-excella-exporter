package net.bis5.excella.primefaces.exporter.treetable;

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
import org.primefaces.model.TreeNode;
import org.primefaces.selenium.AbstractPrimePage;
import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.CommandLink;
import org.primefaces.showcase.view.data.treetable.BasicView;
import org.primefaces.showcase.view.data.treetable.BasicView.DataTypeCheck;

import net.bis5.excella.primefaces.exporter.TakeScreenShotAfterFailure;

@ExtendWith(TakeScreenShotAfterFailure.class)
class TreeBeforeWriteResponseTest extends AbstractPrimePageTest {

    private String getBaseDir() {
        return System.getProperty("basedir");
    }

    @Test
    void exportExcellaAjax(Page page) throws IOException, GeneralSecurityException {
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

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-encrypt-cars-ajax.xlsx");
    }

    @Test
    void exportExcellaNonAjax(Page page) throws IOException, GeneralSecurityException {
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

        assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, "tt-encrypt-cars-non-ajax.xlsx");
    }

    private void assertFileContent(DataTypeCheck parentRecord1, DataTypeCheck childRecord1, DataTypeCheck parentRecord2, DataTypeCheck childRecord2, String outputFileName) throws IOException, GeneralSecurityException {
        try (InputStream is = Files.newInputStream(Paths.get(getBaseDir() + "/docker-compose/downloads/" + outputFileName), StandardOpenOption.READ);
                POIFSFileSystem fs = new POIFSFileSystem(is)) {
            EncryptionInfo info = new EncryptionInfo(fs);
            Decryptor dc = Decryptor.getInstance(info);
            assertTrue(dc.verifyPassword("key"), "could not decrypt");

            // verify content
            new TreeBasicTest().assertFileContent(parentRecord1, childRecord1, parentRecord2, childRecord2, WorkbookFactory.create(dc.getDataStream(fs)));
        }
    }

    public static class Page extends AbstractPrimePage {

        @FindBy(id = "form:excellaExportNonAjax")
        CommandLink commandLinkNonAjax;

        @FindBy(id = "form:excellaExportAjax")
        CommandLink commandLinkAjax;

        @Override
        public String getLocation() {
            return "ui/data/treetable/beforeWriteResponse.xhtml";
        }

    }
}
