package calendar.controller.export;

import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

/**
 * Exports calendar events to CSV format.
 */
public final class CsvExportFormatter implements ExportFormatter {

  @Override
  public void export(final List<IEvent> events,
                     final BufferedWriter writer,
                     final ZoneId timezone) throws IOException {
    writer.write("Subject,Start,End,Location,Status");
    writer.newLine();

    for (IEvent e : events) {
      writer.write(String.format(
          "%s,%s,%s,%s,%s",
          sanitize(e.subject()),
          e.start(),
          e.end(),
          sanitize(e.location()),
          sanitize(e.status())));
      writer.newLine();
    }
  }

  @Override
  public String[] getSupportedExtensions() {
    return new String[] {".csv"};
  }

  private static String sanitize(final String s) {
    return (s == null) ? "" : s.replace(',', ';');
  }
}