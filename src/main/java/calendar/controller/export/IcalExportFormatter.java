package calendar.controller.export;

import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Exports calendar events to iCal format (RFC 5545).
 */
public final class IcalExportFormatter implements ExportFormatter {

  private static final DateTimeFormatter ICAL_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

  @Override
  public void export(final List<IEvent> events,
                     final BufferedWriter writer,
                     final ZoneId timezone) throws IOException {
    // iCal header
    writer.write("BEGIN:VCALENDAR");
    writer.newLine();
    writer.write("VERSION:2.0");
    writer.newLine();
    writer.write("PRODID:-//Calendar Application//EN");
    writer.newLine();
    writer.write("CALSCALE:GREGORIAN");
    writer.newLine();
    writer.write("METHOD:PUBLISH");
    writer.newLine();

    // Timezone component
    writer.write("BEGIN:VTIMEZONE");
    writer.newLine();
    writer.write("TZID:" + timezone.getId());
    writer.newLine();
    writer.write("END:VTIMEZONE");
    writer.newLine();

    // Events
    for (IEvent e : events) {
      writeEvent(e, writer, timezone);
    }

    // Footer
    writer.write("END:VCALENDAR");
    writer.newLine();
  }

  @Override
  public String[] getSupportedExtensions() {
    return new String[] {".ics", ".ical"};
  }

  private void writeEvent(final IEvent event,
                          final BufferedWriter writer,
                          final ZoneId timezone) throws IOException {
    writer.write("BEGIN:VEVENT");
    writer.newLine();

    // UID
    writer.write("UID:" + generateUid(event));
    writer.newLine();

    // Start and end times
    final String startStr = event.start()
        .withZoneSameInstant(timezone)
        .format(ICAL_FORMATTER);
    final String endStr = event.end()
        .withZoneSameInstant(timezone)
        .format(ICAL_FORMATTER);

    writer.write("DTSTART;TZID=" + timezone.getId() + ":" + startStr);
    writer.newLine();
    writer.write("DTEND;TZID=" + timezone.getId() + ":" + endStr);
    writer.newLine();

    // Summary (subject)
    writer.write("SUMMARY:" + escapeIcal(event.subject()));
    writer.newLine();

    // Description (optional)
    if (event.description() != null && !event.description().isEmpty()) {
      writer.write("DESCRIPTION:" + escapeIcal(event.description()));
      writer.newLine();
    }

    // Location (optional)
    if (event.location() != null && !event.location().isEmpty()) {
      writer.write("LOCATION:" + escapeIcal(event.location()));
      writer.newLine();
    }

    // Status/Class
    if ("PRIVATE".equalsIgnoreCase(event.status())) {
      writer.write("CLASS:PRIVATE");
    } else {
      writer.write("CLASS:PUBLIC");
    }
    writer.newLine();

    // Timestamp
    final String now = ZonedDateTime.now(timezone).format(ICAL_FORMATTER);
    writer.write("DTSTAMP:" + now);
    writer.newLine();

    writer.write("END:VEVENT");
    writer.newLine();
  }

  private static String escapeIcal(final String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\")
        .replace(",", "\\,")
        .replace(";", "\\;")
        .replace("\n", "\\n")
        .replace("\r", "");
  }

  private static String generateUid(final IEvent e) {
    final String base = e.subject() + "@" + e.start().toEpochSecond();
    return UUID.nameUUIDFromBytes(base.getBytes()).toString() + "@calendar.app";
  }
}