package net.bis5.excella.primefaces.exporter.listener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents the operation that will be executed before writing the response (file download).
 */
@FunctionalInterface
public interface BeforeWriteResponseListener {

    /**
     * The operation that will be executed before writing the response (file download).
     * @param exportedFilePath the path of the exported file
     * @throws IOException if an I/O error occurs
     */
    void beforeWriteResponse(Path exportedFilePath) throws IOException;

}
