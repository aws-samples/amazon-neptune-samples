package software.amazon.neptune.onegraph.playground.client;

import org.springframework.shell.Input;
import software.amazon.neptune.onegraph.playground.client.api.ConfigType;
import software.amazon.neptune.onegraph.playground.client.api.DataFormat;
import software.amazon.neptune.onegraph.playground.client.api.QueryType;
import software.amazon.neptune.onegraph.playground.client.service.DataService;
import software.amazon.neptune.onegraph.playground.client.service.InfoService;
import software.amazon.neptune.onegraph.playground.client.service.QueryService;
import software.amazon.neptune.onegraph.playground.client.service.ScenarioService;
import software.amazon.neptune.onegraph.playground.client.service.ServiceException;
import software.amazon.neptune.onegraph.playground.client.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ShellComponent
public class Commands {

    private final ScenarioService scenarioService;
    private final InfoService infoService;
    private final DataService dataService;
    private final QueryService queryService;
    private final SettingsService settingsService;

    @Autowired
    public Commands(ScenarioService service,
                    InfoService infoService,
                    QueryService queryService,
                    DataService dataService,
                    SettingsService settingsService) {
        this.scenarioService = service;
        this.infoService = infoService;
        this.dataService = dataService;
        this.queryService = queryService;
        this.settingsService = settingsService;
    }

    @ShellMethod(value = "Load data into the current dataset.", prefix = "-")
    public String load(@ShellOption(value = {"-type","-t"}) String type,
                       String path1,
                       @ShellOption(defaultValue = ShellOption.NULL) String path2) {

        try {
            DataFormat dataFormat = DataFormat.dataFormatFromCLIArgument(type);

            try (InputStream stream1 = Files.newInputStream(Paths.get(path1))) {
                if (path2 != null) {
                    try (InputStream stream2 = Files.newInputStream(Paths.get(path2))) {
                        return this.dataService.load(dataFormat, stream1, stream2);
                    }
                }
                return this.dataService.load(dataFormat, stream1, null);

            }
        } catch (Exception e) {
            return stringInRed(e.getMessage());
        }
    }


    @ShellMethod(value = "View the current dataset", prefix = "-")
    public String view(@ShellOption(value = {"-type","-t"}) String type) {
        try {
            DataFormat dataFormat = DataFormat.dataFormatFromCLIArgument(type);
            return this.dataService.view(dataFormat);
        } catch (Exception e) {
            return stringInRed(e.getMessage());
        }
    }

    @ShellMethod(value = "Export the current dataset.", prefix = "-")
    public String export(@ShellOption(value = {"-type","-t"}) String type,
                         String path1,
                         @ShellOption(defaultValue = ShellOption.NULL) String path2) {
        try {
            DataFormat dataFormat = DataFormat.dataFormatFromCLIArgument(type);
            return this.dataService.export(dataFormat, path1, path2);
        } catch (Exception e) {
            return stringInRed(e.getMessage());
        }
    }

    @ShellMethod("Clear the current dataset.")
    public String clearData() {
        try {
            return this.dataService.clear();
        } catch (ServiceException e) {
            return stringInRed(e.getMessage());
        }
    }

    @ShellMethod(value = "Configure settings", prefix = "-")
    public String settings(@ShellOption(value={"-configuration","-c"}, defaultValue = ShellOption.NULL) String configuration,
                           @ShellOption(defaultValue = ShellOption.NULL) String pathToConfig,
                           @ShellOption(value={"-reversible","-r"}, defaultValue = ShellOption.NULL) String reversible,
                           @ShellOption(value={"-timeout","-t"}, defaultValue = ShellOption.NULL) Long timeout,
                           boolean show) {
        try {
            Boolean reversibleBool = null;
            if (reversible != null) {
                reversibleBool = parseBooleanString(reversible);
            }
            ConfigType type = ConfigType.configTypeFromCLIArgument(configuration);
            String response = this.settingsService.updateSettings(type, pathToConfig, reversibleBool, timeout);

            if (show) {
                response += this.settingsService.getCurrentSettings();
            }
            return response;
        } catch (Exception e) {
            return stringInRed(e.getMessage());
        }
    }

    @ShellMethod(value = "Query the current dataset.", prefix = "-")
    public String query(boolean sparql,
                        boolean gremlin,
                        @ShellOption(value={"-file","-f"}) boolean file,
                        String input) {
        try {
            QueryType type = QueryType.queryTypeFromCLIArguments(sparql, gremlin);
            return this.queryService.query(type, file, input);
        } catch (Exception e) {
            return stringInRed(e.getMessage());
        }
    }

    @ShellMethod(value = "Show info for the current data set.", prefix = "-")
    public String info() {
        try {
            return this.infoService.info();
        } catch (Exception e) {
            return stringInRed(e.getMessage());
        }
    }

    @ShellMethod(value = "Complete a scenario", prefix = "-")
    public void scenario(boolean sc1,
                           boolean sc2,
                           boolean sc3) {

        this.scenarioService.runScenario(sc1, sc2, sc3);
    }

    private boolean parseBooleanString(String booleanString) throws IllegalArgumentException {
        if (booleanString.equalsIgnoreCase("true")) {
            return true;
        }
        if (booleanString.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("Please specify either true or false in the reversible argument.");
    }

    private String stringInRed(String input) {
        return String.format("\u001b[31m%s\u001b[0m", input);
    }
}
