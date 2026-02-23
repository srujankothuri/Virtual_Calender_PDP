package calendar.controller.services;

import static org.junit.Assert.assertNotNull;

import calendar.controller.CalendarManager;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/**
 * Quick test for exporting a calendar file from the manager.
 */
public final class ExportServiceTest {

  private CalendarManager manager;

  /** Creates a calendar and seeds a simple all-day event. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    manager.createCalendar("Main", ZoneId.of("America/New_York"));
    manager.useCalendar("Main");
    manager.addAllDay("Holiday", LocalDate.of(2024, 3, 12));
  }

  /** Export should return a path on success. */
  @Test
  public void export_returnsPathOnSuccess() {
    final Path p = manager.export("test.ics");
    assertNotNull(p);
  }
}
