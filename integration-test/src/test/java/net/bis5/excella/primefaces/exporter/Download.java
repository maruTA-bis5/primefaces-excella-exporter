package net.bis5.excella.primefaces.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.openqa.selenium.HasDownloads;
import org.primefaces.selenium.PrimeSelenium;

public class Download {

    public static Path downloadFileToLocal(String fileName) throws IOException {
        Path localDir = Paths.get(System.getProperty("basedir") ,"target", "downloads");
        ((HasDownloads)PrimeSelenium.getWebDriver()).downloadFile(fileName, localDir.toAbsolutePath());
        return localDir.resolve(fileName);
    }
}
