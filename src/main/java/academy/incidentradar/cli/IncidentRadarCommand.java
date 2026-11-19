package academy.incidentradar.cli;

import academy.incidentradar.app.IncidentRadar;
import academy.incidentradar.app.RadarResult;
import academy.incidentradar.filter.DateRange;
import academy.incidentradar.source.LogSourceException;
import academy.incidentradar.stats.ResponseSizeStatistics;
import academy.incidentradar.stats.Statistics;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(
        name = "incident-radar",
        mixinStandardHelpOptions = true,
        description = "Анализ NGINX-логов: файлы, glob-маски и URL.")
public final class IncidentRadarCommand implements Callable<Integer> {

    @Spec
    @SuppressWarnings("NullAway.Init")
    private CommandSpec spec;

    @Option(
            names = {"-p", "--path"},
            required = true,
            arity = "1..*",
            paramLabel = "<path>",
            description = "Файл, glob-маска или URL; можно указать несколько")
    private List<String> paths = new ArrayList<>();

    @Option(
            names = {"-f", "--output-format"},
            description = "Формат отчёта: ${COMPLETION-CANDIDATES} (по умолчанию ${DEFAULT-VALUE})")
    private OutputFormat outputFormat = OutputFormat.MARKDOWN;

    @Option(
            names = {"-o", "--output-path"},
            description = "Путь для файла отчёта; если не указан, отчёт выводится в консоль")
    private @Nullable Path outputPath;

    @Option(names = "--from", description = "Начало периода, yyyy-MM-dd")
    private @Nullable LocalDate from;

    @Option(names = "--to", description = "Конец периода, yyyy-MM-dd")
    private @Nullable LocalDate to;

    @Override
    public Integer call() {
        validate();

        try {
            RadarResult result = new IncidentRadar().run(paths, new DateRange(from, to));
            print(result);
        } catch (LogSourceException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
        return CommandLine.ExitCode.OK;
    }

    private void print(RadarResult result) {
        PrintWriter out = spec.commandLine().getOut();
        out.printf("Файлов: %d%n", result.files().size());
        result.files().forEach(file -> out.println("  " + file));
        out.printf("Прочитано строк: %d%n", result.linesRead());
        out.printf("Записей в периоде: %d%n", result.linesRead());
        out.printf("Вне периода: %d%n", result.outOfRange());
        out.printf("Пропущено битых строк: %d%n", result.skippedLines());

        Statistics stats = result.statistics();
        out.printf("%nВсего запросов: %d%n", stats.totalRequests());

        out.println("Коды ответа:");
        stats.responseCodes().forEach((code, count) -> out.printf("  %d: %d%n", code, count));

        out.println("Top-10 ресурсов:");
        stats.topResources().forEach(top -> out.printf("  %s: %d%n", top.resource(), top.count()));

        ResponseSizeStatistics size = stats.responseSize();
        out.printf("Размер ответа: average %s, max %d, p95 %d%n", size.average(), size.max(), size.p95());
    }

    private void validate() {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ParameterException(
                    spec.commandLine(), "--from (" + from + ") должен быть раньше --to (" + to + ")");
        }

        if (outputPath != null
                && !outputPath.toString().toLowerCase(Locale.ROOT).endsWith(outputFormat.extension())) {
            throw new ParameterException(
                    spec.commandLine(),
                    "Для формата " + outputFormat + " файл отчёта должен иметь расширение "
                            + outputFormat.extension()
                            + ", получено: " + outputPath);
        }
    }
}
