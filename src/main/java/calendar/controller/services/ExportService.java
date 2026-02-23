package calendar.controller.services;

import calendar.controller.export.ExportFormatter;
import calendar.controller.export.ExportFormatterFactory;
import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Service for exporting calendar data to various formats.
 * This file should be saved as: calendar/controller/services/ExportService.java
 */
public final class ExportService {

  private final CalendarModel model;

  /**
   * Construct with a calendar model.
   *
   * @param model the calendar model
   */
  public ExportService(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
  }

  /**
   * Export the current calendar to a file.
   * The export format is determined by the file extension.
   * Supported formats: .csv (Google Calendar format), .ics/.ical (iCalendar format)
   *
   * @param filename the output filename (must include extension)
   * @return the absolute path of the exported file
   * @throws IOException if the export fails
   * @throws IllegalArgumentException if the filename is invalid or format unsupported
   */
  public Path export(final String filename) throws IOException {
    // Validate filename
    if (filename == null || filename.trim().isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }

    // Validate file extension
    final String lower = filename.toLowerCase();
    if (!lower.endsWith(".csv") && !lower.endsWith(".ics") && !lower.endsWith(".ical")) {
      throw new IllegalArgumentException(
          "Unsupported file format. Use .csv for Google Calendar format "
              + "or .ics/.ical for iCalendar format");
    }

    // Create path object
    final Path path = Path.of(filename);

    // Get all events from the current calendar
    // Use reasonable bounds instead of LocalDate.MIN/MAX to avoid overflow
    final LocalDate farPast = LocalDate.of(1900, 1, 1);
    final LocalDate farFuture = LocalDate.of(2200, 12, 31);
    final List<IEvent> events = model.eventsBetween(farPast, farFuture);

    // Get the appropriate formatter based on file extension
    final ExportFormatter formatter = ExportFormatterFactory.getFormatter(filename);

    // Write to file using try-with-resources for automatic cleanup
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
      // Export using the selected formatter
      formatter.export(events, writer, model.currentZone());
    } catch (IOException e) {
      // Re-throw with more context
      throw new IOException("Failed to export calendar to " + filename + ": " + e.getMessage(), e);
    }

    // Return the absolute path of the exported file
    return path.toAbsolutePath();
  }

  /**
   * Export events within a date range to a file.
   *
   * @param filename the output filename
   * @param fromDate the start date (inclusive)
   * @param toDate the end date (inclusive)
   * @return the absolute path of the exported file
   * @throws IOException if the export fails
   */
  public Path exportRange(final String filename,
                          final LocalDate fromDate,
                          final LocalDate toDate) throws IOException {
    // Validate inputs
    if (filename == null || filename.trim().isEmpty()) {
      throw new IllegalArgumentException("Filename cannot be null or empty");
    }
    if (fromDate == null || toDate == null) {
      throw new IllegalArgumentException("Date range cannot be null");
    }
    if (fromDate.isAfter(toDate)) {
      throw new IllegalArgumentException("From date must be before or equal to to date");
    }

    // Create path object
    final Path path = Path.of(filename);

    // Get events in the specified range
    final List<IEvent> events = model.eventsBetween(fromDate, toDate);

    // Get the appropriate formatter
    final ExportFormatter formatter = ExportFormatterFactory.getFormatter(filename);

    // Write to file
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
      formatter.export(events, writer, model.currentZone());
    } catch (IOException e) {
      throw new IOException(
          "Failed to export calendar range to " + filename + ": " + e.getMessage(), e);
    }

    return path.toAbsolutePath();
  }
}