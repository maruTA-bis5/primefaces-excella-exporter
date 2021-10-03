package net.bis5.excella.primefaces.exporter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.primefaces.selenium.spi.WebDriverProvider;

public class TakeScreenShotAfterFailure implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        File file = ((TakesScreenshot)WebDriverProvider.get()).getScreenshotAs(OutputType.FILE);
        Path to = Paths.get("./target", file.getName());
        try {
            Files.copy(file.toPath(), to);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Logger.getLogger(getClass().getName()).warning("Screenshot saved as: " + to.toAbsolutePath());
    }
}
