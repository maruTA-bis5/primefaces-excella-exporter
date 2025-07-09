package org.primefaces.showcase.view.data.treetable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.primefaces.showcase.view.data.ExCellaExporterOptionsImpl;

import net.bis5.excella.primefaces.exporter.ExCellaExporterOptions;

@Named("ttEncryptView")
@ViewScoped
public class EncryptView extends BasicView {

    public ExCellaExporterOptions getEncryptOptions() {
        var options = new ExCellaExporterOptionsImpl();
        options.addBeforeWriteResponseListener(this::encrypt);
        return options;
    }

    private void encrypt(Path outputFilePath) throws IOException {
        Path beforeEncrypt = Files.createTempFile("before-encrypt", ".xlsx");
        Files.copy(outputFilePath, beforeEncrypt, StandardCopyOption.REPLACE_EXISTING);
        try (var fs = new POIFSFileSystem()) {
            var info = new EncryptionInfo(EncryptionMode.agile);
            var enc = info.getEncryptor();
            enc.confirmPassword("key");

            try (OutputStream os = enc.getDataStream(fs)) {
                Files.copy(beforeEncrypt, os);
            }

            try (var out = Files.newOutputStream(outputFilePath, StandardOpenOption.TRUNCATE_EXISTING)) {
                fs.writeFilesystem(out);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        } finally {
            Files.delete(beforeEncrypt);
        }
    }

}
