package calendar.controller.export;

import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

/**
 * Strategy interface for exporting calendar events to different formats.
 * Implementations handle format-specific serialization logic.
 */
public interface ExportFormatter {

  /**
   * Writes events to the given writer in the implementation's format.
   *
   * @param events list of events to export
   * @param writer output writer
   * @param timezone the calendar's timezone
   * @throws IOException if writing fails
   */
  void export(List<IEvent> events, BufferedWriter writer, ZoneId timezone)
      throws IOException;

  /**
   * Returns the file extensions this formatter supports.
   *
   * @return array of supported extensions (e.g., [".csv"])
   */
  String[] getSupportedExtensions();
}