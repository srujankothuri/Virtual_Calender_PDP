package calendar.controller.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.model.IEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Content-level tests for {@link CsvExportFormatter}.
 * Verifies header, sanitization, null handling, and newlines.
 */
public final class CsvExportFormatterTest {

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

  /** CSV should include a header and sanitize commas inside fields. */
  @Test
  public void writesHeaderAndSanitizesCommas() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-03-12T10:00:00Z");
    ZonedDateTime e = s.plusHours(2);
    List<IEvent> events = new ArrayList<>();
    events.add(new E("Meet, Team", s, e, "", "Room, 101", "PUBLIC"));

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    CsvExportFormatter f = new CsvExportFormatter();
    f.export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String out = sw.toString();

    assertTrue(out.startsWith("Subject,Start,End,Location,Status"));
    assertTrue(out.contains("Meet; Team"));
    assertTrue(out.contains("Room; 101"));
    assertTrue(out.contains(s.toString()));
    assertTrue(out.contains(e.toString()));

    boolean endsWithNl = out.endsWith(System.lineSeparator()) || out.endsWith("\n");
    assertTrue(endsWithNl);
  }

  /** CSV sanitization must handle null fields without throwing. */
  @Test
  public void handlesNullFieldsGracefully() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-07-04T09:00:00Z");
    ZonedDateTime e = s.plusHours(1);
    List<IEvent> events = new ArrayList<>();
    events.add(new E("Independence", s, e, null, null, null));

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    CsvExportFormatter f = new CsvExportFormatter();
    f.export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String out = sw.toString();

    assertFalse(out.contains("null"));
    assertTrue(out.contains("Independence"));
  }

  /** Multiple events should result in multiple lines in the file. */
  @Test
  public void writesMultipleEvents() throws IOException {
    ZonedDateTime s1 = ZonedDateTime.parse("2025-05-01T08:00:00Z");
    ZonedDateTime e1 = s1.plusHours(1);
    ZonedDateTime s2 = ZonedDateTime.parse("2025-05-01T10:00:00Z");
    ZonedDateTime e2 = s2.plusHours(2);

    List<IEvent> events = new ArrayList<>();
    events.add(new E("One", s1, e1, "", "", "PUBLIC"));
    events.add(new E("Two", s2, e2, "", "", "PRIVATE"));

    StringWriter sw = new StringWriter();
    BufferedWriter bw = new BufferedWriter(sw);

    CsvExportFormatter f = new CsvExportFormatter();
    f.export(events, bw, ZoneId.of("UTC"));
    bw.flush();

    String out = sw.toString();

    assertTrue(out.contains("One"));
    assertTrue(out.contains("Two"));
  }
}