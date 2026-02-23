package calendar.controller.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Test;

/**
 * Content-level tests for {@link IcalExportFormatter}.
 * Verifies scaffold, timezone block, VEVENT fields, optional sections,
 * CLASS branch, UID suffix, and escaping.
 */
public final class IcalExportFormatterTest {

  /** Minimal immutable event for formatter tests. */
  private static final class E implements IEvent {
    private final String subject;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String description;
    private final String location;
    private final String status;

    E(String subjectVal,
        ZonedDateTime startVal,
        ZonedDateTime endVal,
        String descriptionVal,
        String locationVal,
        String statusVal) {
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

  /** iCal scaffold and required fields must be present (PRIVATE branch). */
  @Test
  public void writesScaffoldAndPrivateClass() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-06-01T09:30:00Z");
    ZonedDateTime e = s.plusHours(1);
    List<IEvent> events = List.of(new E("Standup", s, e, "Daily sync", "HQ 2F", "PRIVATE"));

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    IcalExportFormatter f = new IcalExportFormatter();
    f.export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String out = sw.toString();

    assertTrue(out.contains("BEGIN:VCALENDAR"));
    assertTrue(out.contains("VERSION:2.0"));
    assertTrue(out.contains("PRODID:-//Calendar Application//EN"));
    assertTrue(out.contains("CALSCALE:GREGORIAN"));
    assertTrue(out.contains("METHOD:PUBLISH"));
    assertTrue(out.contains("BEGIN:VTIMEZONE"));
    assertTrue(out.contains("TZID:UTC"));
    assertTrue(out.contains("END:VTIMEZONE"));
    assertTrue(out.contains("BEGIN:VEVENT"));
    assertTrue(out.contains("DTSTART;TZID=UTC:"));
    assertTrue(out.contains("DTEND;TZID=UTC:"));
    assertTrue(out.contains("SUMMARY:Standup"));
    assertTrue(out.contains("DESCRIPTION:Daily sync"));
    assertTrue(out.contains("LOCATION:HQ 2F"));
    assertTrue(out.contains("CLASS:PRIVATE"));
    assertTrue(out.contains("@calendar.app"));
    assertTrue(out.contains("DTSTAMP:"));
    assertTrue(out.contains("END:VEVENT"));
    assertTrue(out.contains("END:VCALENDAR"));
  }

  /** When optional fields empty, omit them and use PUBLIC class. */
  @Test
  public void omitsOptionalFieldsAndUsesPublicClass() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-07-04T00:00:00Z");
    ZonedDateTime e = s.plusHours(2);
    List<IEvent> events = List.of(new E("Parade", s, e, null, "", "PUBLIC"));

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    IcalExportFormatter f = new IcalExportFormatter();
    f.export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String out = sw.toString();

    assertFalse(out.contains("DESCRIPTION:"));
    assertFalse(out.contains("LOCATION:"));
    assertTrue(out.contains("CLASS:PUBLIC"));
  }

  /** Summary must be properly escaped for reserved characters. */
  @Test
  public void escapesReservedCharactersInSummary() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-12-31T23:00:00Z");
    ZonedDateTime e = s.plusHours(1);
    String tricky = "A\\B, C;D";
    List<IEvent> events = List.of(new E(tricky, s, e, "", "", "PUBLIC"));

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    IcalExportFormatter f = new IcalExportFormatter();
    f.export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String out = sw.toString();

    assertTrue(out.contains("SUMMARY:A\\\\B\\, C\\;D"));
  }
}