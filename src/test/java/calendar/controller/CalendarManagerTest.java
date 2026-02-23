package calendar.controller;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic smoke tests for {@link CalendarManager} facade wiring.
 */
public final class CalendarManagerTest {

  private CalendarManager manager;

  /** Initializes a fresh manager for each test. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
  }

  /** Verifies creating and using a calendar sets current name and timezone. */
  @Test
  public void createAndUseCalendar_setsCurrentNameAndZone() {
    final ZoneId ny = ZoneId.of("America/New_York");
    manager.createCalendar("Main", ny);
    manager.useCalendar("Main");

    assertEquals("Main", manager.getCurrentCalendarName());
    assertEquals(ny, manager.getCurrentTimezone());
  }
}
