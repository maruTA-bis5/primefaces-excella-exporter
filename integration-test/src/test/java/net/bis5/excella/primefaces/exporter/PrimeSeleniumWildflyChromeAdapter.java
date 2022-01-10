package net.bis5.excella.primefaces.exporter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.primefaces.selenium.internal.ConfigProvider;
import org.primefaces.selenium.spi.DeploymentAdapter;
import org.primefaces.selenium.spi.WebDriverAdapter;

public class PrimeSeleniumWildflyChromeAdapter implements WebDriverAdapter, DeploymentAdapter {

    @Override
    public WebDriver createWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setHeadless(Boolean.valueOf(System.getProperty("webdriver.headless")));
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--remote-debugging-port=9222");
        Map<String, Object> prefs = Map.of("download.default_directory", "/tmp/downloads");
        options.setExperimentalOption("prefs", prefs);
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog::loggingPrefs", logPrefs);
        options.setExperimentalOption("w3c", false);

        try {
            return new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void startup() throws Exception {
        // no op
    }

    @Override
    public String getBaseUrl() {
        String baseUrl = System.getProperty("baseUrl");
        if ("".equals(baseUrl)) {
            return "http://127.0.0.1:8080/";
        } else {
            return baseUrl;
        }
    }

    @Override
    public void shutdown() throws Exception {
        // no op
    }

    @Override
    public void initialize(ConfigProvider configProvider) {
        // no op
    }

}
