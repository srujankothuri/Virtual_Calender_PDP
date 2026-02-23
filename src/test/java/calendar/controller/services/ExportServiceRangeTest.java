package calendar.controller.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.model.CalendarService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.Test;

/** ExportService tests: validations, IO rethrow, and successful write with strict header checks. */
public final class ExportServiceRangeTest {

  /** Build a real in-memory model with an active UTC calendar (no events needed). */
  private static CalendarService newActiveUtcModel() {
    CalendarService model = new CalendarService();
    model.createCalendar("Main", ZoneId.of("UTC"));
    model.useCalendar("Main");
    return model;
  }

  @Test
  public void validatesFilenameNullOrEmpty() throws Exception {
    ExportService svc = new ExportService(newActiveUtcModel());
    try {
      svc.exportRange(null, LocalDate.now(), LocalDate.now());
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Filename cannot be null or empty", iae.getMessage());
    }

    try {
      svc.exportRange("   ", LocalDate.now(), LocalDate.now());
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Filename cannot be null or empty", iae.getMessage());
    }
  }

  @Test
  public void validatesDateNullsAndOrder() throws Exception {
    ExportService svc = new ExportService(newActiveUtcModel());

    try {
      svc.exportRange("x.ics", null, LocalDate.now());
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Date range cannot be null", iae.getMessage());
    }

    try {
      svc.exportRange("x.ics", LocalDate.now(), null);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("Date range cannot be null", iae.getMessage());
    }

    LocalDate a = LocalDate.of(2025, 6, 2);
    LocalDate b = LocalDate.of(2025, 6, 1);
    try {
      svc.exportRange("x.ics", a, b);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      assertEquals("From date must be before or equal to to date", iae.getMessage());
    }
  }

  @Test
  public void rethrowsIoWithMessageOnWriteFailure() throws Exception {
    ExportService svc = new ExportService(newActiveUtcModel());

    // Create a directory whose name ends with ".ics" and attempt to "write" to it.
    Path parent = Files.createTempDirectory("as-file-parent");
    Path dirIcs = parent.resolve("bad.ics");
    Files.createDirectory(dirIcs);

    try {
      svc.exportRange(dirIcs.toString(), LocalDate.of(2025, 6, 1),
          LocalDate.of(2025, 6, 2));
      fail("expected IOException");
    } catch (IOException ioe) {
      assertTrue(ioe.getMessage().startsWith("Failed to export calendar range to "));
    } finally {
      Files.deleteIfExists(dirIcs);
      Files.deleteIfExists(parent);
    }
  }

  @Test
  public void writesIcsAndReturnsAbsolutePath() throws Exception {
    ExportService svc = new ExportService(newActiveUtcModel());
    Path tmp = Files.createTempFile("export-", ".ics");

    Path out = svc.exportRange(
        tmp.toString(),
        LocalDate.of(2025, 6, 1),
        LocalDate.of(2025, 6, 1));

    assertEquals(out, out.toAbsolutePath());

    String text = Files.readString(out);
    String[] lines = text.split("\\R");

    assertTrue("Expected basic iCal header", lines.length >= 5);
    assertEquals("BEGIN:VCALENDAR", lines[0]);
    assertEquals("VERSION:2.0", lines[1]);
    assertEquals("PRODID:-//Calendar Application//EN", lines[2]);
    assertEquals("CALSCALE:GREGORIAN", lines[3]);
    assertEquals("METHOD:PUBLISH", lines[4]);

    Files.deleteIfExists(out);
  }
}
