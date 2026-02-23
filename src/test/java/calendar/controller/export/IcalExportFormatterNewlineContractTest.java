package calendar.controller.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Test;

/** Verifies exact line structure so removed newLine() mutants cannot survive. */
public final class IcalExportFormatterNewlineContractTest {

  /** Minimal immutable event for formatter tests. */
  private static final class E implements IEvent {
    private final String subject;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String description;
    private final String location;
    private final String status;

    E(final String subjectVal,
        final ZonedDateTime startVal,
        final ZonedDateTime endVal,
        final String descriptionVal,
        final String locationVal,
        final String statusVal) {
      this.subject = subjectVal;
      this.start = startVal;
      this.end = endVal;
      this.description = descriptionVal;
      this.location = locationVal;
      this.status = statusVal;
    }

    @Override
    public String subject() {
      return subject;
    }

    @Override
    public ZonedDateTime start() {
      return start;
    }

    @Override
    public ZonedDateTime end() {
      return end;
    }

    @Override
    public String description() {
      return description;
    }

    @Override
    public String location() {
      return location;
    }

    @Override
    public String status() {
      return status;
    }
  }

  private static int indexOfExact(final String[] lines, final String expected) {
    for (int i = 0; i < lines.length; i++) {
      if (expected.equals(lines[i])) {
        return i;
      }
    }
    return -1;
  }

  @Test
  public void headerTimezoneAndEventAreEachOnTheirOwnLine() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-06-01T09:30:00Z");
    IEvent ev = new E("Standup", s, s.plusHours(1), "Daily sync", "HQ 2F", "PUBLIC");
    List<IEvent> events = List.of(ev);

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    new IcalExportFormatter().export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String[] lines = sw.toString().split("\\R");

    int i0 = indexOfExact(lines, "BEGIN:VCALENDAR");
    int i1 = indexOfExact(lines, "VERSION:2.0");
    int i2 = indexOfExact(lines, "PRODID:-//Calendar Application//EN");
    int i3 = indexOfExact(lines, "CALSCALE:GREGORIAN");
    int i4 = indexOfExact(lines, "METHOD:PUBLISH");
    int i5 = indexOfExact(lines, "BEGIN:VTIMEZONE");
    int i6 = indexOfExact(lines, "TZID:UTC");
    int i7 = indexOfExact(lines, "END:VTIMEZONE");
    int bv = indexOfExact(lines, "BEGIN:VEVENT");
    int evEnd = indexOfExact(lines, "END:VEVENT");
    int vcEnd = indexOfExact(lines, "END:VCALENDAR");

    assertTrue(i0 >= 0 && i1 >= 0 && i2 >= 0 && i3 >= 0 && i4 >= 0);
    assertTrue(i5 >= 0 && i6 >= 0 && i7 >= 0);
    assertTrue(bv >= 0 && evEnd >= 0 && vcEnd >= 0);

    // Ensure strict order (proves there was a newline between each).
    assertTrue(i0 < i1 && i1 < i2 && i2 < i3 && i3 < i4);
    assertTrue(i4 < i5 && i5 < i6 && i6 < i7);
    assertTrue(i7 < bv && bv < evEnd && evEnd < vcEnd);
  }

  /** Summary must be escaped and appear as an exact single line. */
  @Test
  public void escapesReservedCharactersInSummary() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-12-31T23:00:00Z");
    String tricky = "A\\B, C;D\nE";
    IEvent ev = new E(tricky, s, s.plusHours(1), "", "", "PUBLIC");
    List<IEvent> events = List.of(ev);

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);
    new IcalExportFormatter().export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String[] lines = sw.toString().split("\\R");
    String summary = null;
    for (String ln : lines) {
      if (ln.startsWith("SUMMARY:")) {
        summary = ln;
        break;
      }
    }

    assertNotNull("SUMMARY line missing", summary);
    assertEquals("SUMMARY:A\\\\B\\, C\\;D\\nE", summary);
  }
}
