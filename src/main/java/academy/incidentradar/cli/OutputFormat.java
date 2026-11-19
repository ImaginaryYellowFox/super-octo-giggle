package academy.incidentradar.cli;

public enum OutputFormat {
    JSON(".json"),
    MARKDOWN(".md");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }
}
