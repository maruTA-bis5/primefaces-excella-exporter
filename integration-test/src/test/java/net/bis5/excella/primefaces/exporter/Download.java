package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.openqa.selenium.HasDownloads;
import org.primefaces.selenium.PrimeSelenium;

public class Download {

    private static final int ADDITIONAL_WAIT_MILLIS = 1000;

    public static Path downloadFileToLocal(String fileName) throws IOException {

        // In some environments (especially CI), the file is not ready after clicking the download link.
        if ("true".equals(System.getProperty("exporterIT.enableAdditionalWaitBeforeDownload"))) {
            Logger.getLogger(Download.class.getName()).info(() -> "Waiting " + ADDITIONAL_WAIT_MILLIS + "ms before downloading file " + fileName);
            PrimeSelenium.wait(ADDITIONAL_WAIT_MILLIS);
        }

        Path localDir = Paths.get(System.getProperty("basedir") ,"target", "downloads");
        ((HasDownloads)PrimeSelenium.getWebDriver()).downloadFile(fileName, localDir.toAbsolutePath());
        return localDir.resolve(fileName);
    }
}
