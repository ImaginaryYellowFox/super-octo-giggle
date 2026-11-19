package academy.incidentradar.cli;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.support.CliRunner;
import academy.incidentradar.support.TestHttpServer;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IncidentRadarCommand")
class IncidentRadarCommandTest {

    private static final int USAGE_ERROR = 2;
    private static final String SAMPLE_LOG =
            Path.of("samples", "logs", "reference-codes.log").toString();

    @Test
    @DisplayName("Несколько URL в --path: читаются все")
    void readsSeveralUrlsFromCommandLine() {
        try (var server = TestHttpServer.start()) {
            server.respond("/day1.log", HttpURLConnection.HTTP_OK, "a1\na2\n")
                    .respond("/day2.log", HttpURLConnection.HTTP_OK, "b1\n");

            var result = CliRunner.run("--path", server.url("/day1.log"), server.url("/day2.log"));

            assertThat(result.exitCode()).as("%s", result).isZero();
            assertThat(result.stdout()).contains("Файлов: 2").contains("Прочитано строк: 3" + System.lineSeparator());
        }
    }

    @Test
    @DisplayName("Один и тот же файл дважды в --path учитывается один раз")
    void duplicatePathsAreReadOnce() {
        var result = CliRunner.run("--path", SAMPLE_LOG, SAMPLE_LOG);

        assertThat(result.exitCode()).as("%s", result).isZero();
        assertThat(result.stdout()).contains("Файлов: 1");
    }

    @Test
    @DisplayName("--output-format не зависит от регистра")
    void outputFormatIsCaseInsensitive() {
        var result = CliRunner.run("--path", SAMPLE_LOG, "--output-format", "JSON");

        assertThat(result.exitCode()).as("%s", result).isZero();
    }

    @Test
    @DisplayName("Некорректная дата в --from -> exit code 2")
    void invalidDateExitsWithTwo() {
        var result = CliRunner.run("--path", SAMPLE_LOG, "--from", "2025-13-01");

        assertThat(result.exitCode()).as("%s", result).isEqualTo(USAGE_ERROR);
        assertThat(result.stderr()).contains("Invalid value for option '--from'");
    }
}
