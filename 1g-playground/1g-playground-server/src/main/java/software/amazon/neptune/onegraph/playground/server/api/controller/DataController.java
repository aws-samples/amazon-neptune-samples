package software.amazon.neptune.onegraph.playground.server.api.controller;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.rdf4j.query.algebra.Load;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.neptune.onegraph.playground.server.servicespring.ClearServiceSpring;
import software.amazon.neptune.onegraph.playground.server.servicespring.ExportServiceSpring;
import software.amazon.neptune.onegraph.playground.server.servicespring.LoadServiceSpring;
import software.amazon.neptune.onegraph.playground.server.servicespring.ViewServiceSpring;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.api.request.LoadRequest;
import software.amazon.neptune.onegraph.playground.server.service.ExportService.ExportException;
import software.amazon.neptune.onegraph.playground.server.service.LoadService.LoadException;
import software.amazon.neptune.onegraph.playground.server.service.ViewService.ViewException;
import software.amazon.neptune.onegraph.playground.server.state.State;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * REST controller for data loading, viewing and exporting.
 * Handles following commands: <br />
 * - load <br />
 * - view <br />
 * - export <br />
 */
@RestController
@RequestMapping("/data")
public class DataController {

    private static final Logger logger = LoggerFactory.getLogger(DataController.class);

    private final ExportServiceSpring exportService;
    private final LoadServiceSpring loadService;
    private final ViewServiceSpring viewService;
    private final ClearServiceSpring clearService;

    @Autowired
    public DataController(ExportServiceSpring es,
                          LoadServiceSpring ls,
                          ViewServiceSpring vs,
                          ClearServiceSpring cs) {
        this.exportService = es;
        this.loadService = ls;
        this.viewService = vs;
        this.clearService = cs;
    }

    /**
     * Callback for GET requests to the /data/export path, exports the current data in the
     * {@link State} to file.
     * @param dataFormat The data format to export the current data in.
     * @param path1 Path to file to export to.
     * @param path2 Second path to file to export to, only pass this when {@code dataFormat} is {@code NEPTUNECSV},
     *              the nodes of the {@code NEPTUNECSV} will be exported to {@code path1}, the edges to {@code path2}.
     * @throws IllegalArgumentException When {@code path2} is provided but {@code dataFormat} is not {@code NEPTUNECSV} or,
     * when a {@code path2} is not provided but the {@code dataFormat} is {@code NEPTUNECSV}
     * @return A string containing information for the client about the export.
     */
    @GetMapping("/export")
    @ResponseBody
    public String export(@RequestParam(name="dataFormat") DataFormat dataFormat,
                         @RequestParam(name="path1") String path1,
                         @RequestParam(name="path2", required = false) String path2) throws IllegalArgumentException {
        validateExportParameters(dataFormat, (path2 != null));

        Path p1 = Paths.get(path1);
        Path p2 = null;
        if (path2 != null && !path2.isEmpty()) {
            p2 = Paths.get(path2);
        }
        exportService.exportData(dataFormat, p1, p2);

        String resultString = "Export successful, written to " + p1;
        if (p2 != null) {
            resultString += " and " + p2;
        }

        return resultString;
    }

    /**
     * Callback for GET requests to the /data path, returns the current data in the
     * {@link State} to the client.
     * @param dataFormat The data format the returned data should be in.
     * @return The serialized data currently in the {@link State} in the given {@code dataFormat}.
     */
    @GetMapping
    @ResponseBody
    public String view(@RequestParam(name="dataFormat") DataFormat dataFormat) {
        return this.viewService.viewOGDatasetInDataFormat(dataFormat);
    }

    /**
     * Callback for POST requests to the /data path, loads data from {@link LoadRequest#path1} and {@link LoadRequest#path2},
     * into the {@link State}.
     * @return A string containing information for the client about the load.
     * @throws IllegalArgumentException When {@link LoadRequest#path2} is provided but {@link LoadRequest#dataFormat} is
     * not {@code NEPTUNECSV} or, when {@link LoadRequest#path2} is not provided but the {@link LoadRequest#dataFormat} is {@code NEPTUNECSV}.
     */
    @RequestMapping(method = POST, consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public String load(@RequestParam DataFormat format, @RequestPart MultipartFile data,
                       @RequestPart(required = false) MultipartFile additionalData) throws IllegalArgumentException, IOException {
        StringBuilder status = new StringBuilder();
        validateLoadParameters(format, (additionalData != null));

        try (InputStream stream1 = data.getInputStream()) {
            if (additionalData != null) {
                try (InputStream stream2 = additionalData.getInputStream()) {
                    loadService.loadDataAndUpdateState(format,
                            stream1, stream2, status);
                }
            } else {
                loadService.loadDataAndUpdateState(format,
                        stream1, null, status);
            }
        } catch (IOException e) {
            throw new LoadException("Input stream to file could not be established", e);
        }
        status.append("Data loaded successfully");
        return status.toString();
    }

    /**
     * Callback for DELETE requests to the /data path, clears all data currently in the {@link State}.
     * @return A string containing information for the client about the clear.
     */
    @DeleteMapping
    public String clear() {
        StringBuilder status = new StringBuilder();
        this.clearService.clearData(status);
        return status.toString();
    }

    // Checks if path2 is given only if dataFormat is NEPTUNECSV
    private void validateExportParameters(DataFormat dataFormat,
                                          boolean hasSecondFile) throws IllegalArgumentException {
        if (hasSecondFile && dataFormat != DataFormat.NEPTUNECSV) {
            throw new IllegalArgumentException("Invalid argument combination: no path2 argument should be provided if data format is not NEPTUNECSV");
        }
        if (!hasSecondFile && dataFormat == DataFormat.NEPTUNECSV) {
            throw new IllegalArgumentException("Invalid argument combination: path2 argument is required when data format is NEPTUNECSV");
        }
    }

    // Checks if path2 is given only if dataFormat is NEPTUNECSV and checks if dataFormat is not FORMAL,
    // because data in FORMAL format can not be loaded.
    private void validateLoadParameters(DataFormat dataFormat, boolean hasSecondFile) throws IllegalArgumentException {
        if (dataFormat == DataFormat.FORMAL) {
            throw new IllegalArgumentException("Invalid data format argument: can not load data of format FORMAL");
        }
        validateExportParameters(dataFormat, hasSecondFile);
    }

    // Exception handling

    @ExceptionHandler({ IllegalArgumentException.class })
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        logger.warn("Argument exception - " + e.getMessage());

        String errorToReturn = String.format("Incorrect arguments provided; %s", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorToReturn);
    }

    @ExceptionHandler({ ExportException.class })
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleException(ExportException exportException) {
        logger.warn("Export exception - " + exportException.getCause().getMessage());

        Throwable rootCause = ExceptionUtils.getRootCause(exportException);

        String errorToReturn = String.format("An unknown exception has occurred during export CAUSE: %s ROOT CAUSE: %s",
                exportException.getMessage(),
                rootCause.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorToReturn);
    }

    @ExceptionHandler({ ViewException.class })
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleException(ViewException viewException) {
        logger.warn("View exception - " + viewException.getCause().getMessage());

        Throwable rootCause = ExceptionUtils.getRootCause(viewException);

        String errorToReturn = String.format("An unknown exception has occurred during view CAUSE: %s ROOT CAUSE: %s",
                viewException.getMessage(),
                rootCause.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorToReturn);
    }

    @ExceptionHandler({ LoadException.class })
    public ResponseEntity<String> handleException(LoadException loadException) {
        logger.warn("Load exception - " + loadException);

        Throwable rootCause = ExceptionUtils.getRootCause(loadException);
        if (rootCause instanceof NoSuchFileException) {
            String errorToReturn = "An exception occurred during load; There was no file found at one of the provided paths";

            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(errorToReturn);
        } else {
            String errorToReturn = String.format("An unknown exception has occurred during file load CAUSE: %s ROOT CAUSE: %s",
                    loadException.getMessage(),
                    rootCause.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorToReturn);
        }
    }
}
