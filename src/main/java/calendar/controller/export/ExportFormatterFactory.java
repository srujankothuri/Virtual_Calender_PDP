package calendar.controller.export;

import java.util.Arrays;
import java.util.List;

/**
 * Factory for creating export formatters based on file extension.
 * New formatters can be registered here without modifying existing code.
 */
public final class ExportFormatterFactory {

  // Register all available formatters here
  private static final List<ExportFormatter> FORMATTERS = Arrays.asList(
      new CsvExportFormatter(),
      new IcalExportFormatter()
      // Add new formatters here: new JsonExportFormatter(), etc.
  );

  private ExportFormatterFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Returns the appropriate formatter for the given file path.
   *
   * @param filePath path with extension
   * @return appropriate formatter
   * @throws IllegalArgumentException if no formatter supports the extension
   */
  public static ExportFormatter getFormatter(final String filePath) {
    final String lowerPath = filePath.toLowerCase();

    for (ExportFormatter formatter : FORMATTERS) {
      for (String extension : formatter.getSupportedExtensions()) {
        if (lowerPath.endsWith(extension)) {
          return formatter;
        }
      }
    }

    throw new IllegalArgumentException(
        "Unsupported export format for file: "
            + filePath
            + ". Supported extensions: " + getSupportedExtensions());
  }

  private static String getSupportedExtensions() {
    final StringBuilder sb = new StringBuilder();
    for (ExportFormatter formatter : FORMATTERS) {
      for (String ext : formatter.getSupportedExtensions()) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(ext);
      }
    }
    return sb.toString();
  }
}