package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.support.CliRunner;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Обязательные тесты: валидация аргументов командной строки. */
@DisplayName("MR1. Валидация аргументов CLI")
class CliValidationTest {

    private static final int USAGE_ERROR = 2;
    private static final String SAMPLE_LOG =
            Path.of("samples", "logs", "referenced-codes.log").toString();

    private static void assertUsageError(CliRunner.Result result, String expectedMessage) {
        assertThat(result.exitCode())
                .as("Ожидался код 2 (ошибка использования).%n%s", result)
                .isEqualTo(USAGE_ERROR);
        assertThat(result.stderr())
                .as("В stderr должно быть сообщение об ошибке")
                .contains(expectedMessage);
    }

    @Test
    @DisplayName("Не передан обязательный --path -> exit code 2")
    void missingPathExitsWithTwo() {
        var result = CliRunner.run("--output-format", "json");

        assertUsageError(result, "Missing required option");
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "csv", "txt"})
    @DisplayName("Неподдерживаемый --output-format \"{0}\" -> exit code 2")
    void unsupportedOutputFormatExitsWithTwo(String format) {
        var result = CliRunner.run("--path", SAMPLE_LOG, "--output-format", format);

        assertUsageError(result, "Invalid value for option '--output-format'");
        assertThat(result.stderr()).contains("'" + format + "'");
    }

    @Test
    @DisplayName("--from больше или равен --to -> exit code 2")
    void fromNotBeforeToExitsWithTwo() {
        var fromAfterTo = CliRunner.run("--path", SAMPLE_LOG, "--from", "2025-03-02", "--to", "2025-03-01");
        var fromEqualsTo = CliRunner.run("--path", SAMPLE_LOG, "--from", "2025-03-01", "--to", "2025-03-01");

        assertUsageError(fromAfterTo, "должен быть раньше");
        assertUsageError(fromEqualsTo, "должен быть раньше");
    }

    @Test
    @DisplayName("Несуществующий локальный файл -> exit code 2")
    void missingLocalFileExitsWithTwo() {
        var missing = Path.of("samples", "logs", "no-such-file.log").toString();

        var result = CliRunner.run("--path", missing);
        assertUsageError(result, "Файл не найден");
        assertThat(result.stderr()).contains("no-such-file.log");
    }

    @Test
    @DisplayName("Расширение файла отчёта не соответствует формату -> exit code 2")
    void outputExtensionMismatchExitsWithTwo() {
        var jsonToMd = CliRunner.run("--path", SAMPLE_LOG, "--output-format", "json", "--output-path", "report.md");
        var mdToJson =
                CliRunner.run("--path", SAMPLE_LOG, "--output-format", "markdown", "--output-path", "report.json");

        assertUsageError(jsonToMd, "должен иметь расширение .json");
        assertUsageError(mdToJson, "должен иметь расширение .md");
    }
}
