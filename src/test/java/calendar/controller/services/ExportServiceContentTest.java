package calendar.controller.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.export.CsvExportFormatter;
import calendar.controller.export.IcalExportFormatter;
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
 * Formatter smoke tests placed with service tests so they run alongside others.
 * Directly exercise the formatters with a StringWriter.
 */
public final class ExportServiceContentTest {

  /** Small immutable event stub for formatter tests. */
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

  /** CSV: header + sanitization; iCal: scaffold + VEVENT presence. */
  @Test
  public void csvAndIcalMinimalSmoke() throws IOException {
    ZonedDateTime s = ZonedDateTime.parse("2025-01-01T10:00:00Z");
    ZonedDateTime e = s.plusHours(2);

    // ---- CSV: use a location with comma to verify sanitization ----
    List<IEvent> events = new ArrayList<>();
    events.add(new E("Meet, Team", s, e, "", "Room, 101", "PUBLIC"));

    StringWriter csvSw = new StringWriter();
    BufferedWriter csvBw = new BufferedWriter(csvSw);

    CsvExportFormatter csv = new CsvExportFormatter();
    csv.export(events, csvBw, ZoneId.of("UTC"));
    csvBw.flush();

    String csvOut = csvSw.toString();
    assertTrue(csvOut.startsWith("Subject,Start,End,Location,Status"));
    assertTrue(csvOut.contains("Meet; Team"));
    assertTrue(csvOut.contains("Room; 101"));

    // ---- ICS: use no optional fields so DESCRIPTION/LOCATION are omitted ----
    List<IEvent> eventsIcs = new ArrayList<>();
    eventsIcs.add(new E("Parade", s, e, null, "", "PUBLIC"));

    StringWriter icsSw = new StringWriter();
    BufferedWriter icsBw = new BufferedWriter(icsSw);

    IcalExportFormatter ics = new IcalExportFormatter();
    ics.export(eventsIcs, icsBw, ZoneId.of("UTC"));
    icsBw.flush();

    String icsOut = icsSw.toString();
    assertTrue(icsOut.contains("BEGIN:VCALENDAR"));
    assertTrue(icsOut.contains("BEGIN:VEVENT"));
    assertTrue(icsOut.contains("@calendar.app"));
    assertFalse(icsOut.contains("DESCRIPTION:"));
    assertFalse(icsOut.contains("LOCATION:"));
  }
}
