package calendar.controller.services;

import static org.junit.Assert.assertEquals;

import calendar.controller.CalendarManager;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Smoke test for {@link EventService} via {@link CalendarManager}.
 */
public final class EventServiceTest {

  private CalendarManager manager;

  /** Creates a default calendar for the edit test. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    manager.createCalendar("Main", ZoneId.of("America/New_York"));
    manager.useCalendar("Main");
  }

  /** Editing the start should keep the original duration intact. */
  @Test
  public void editStart_preservesDuration() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-11T10:00-04:00[America/New_York]");
    final ZonedDateTime e =
        ZonedDateTime.parse("2024-03-11T12:00-04:00[America/New_York]");
    manager.addSingle("Standup", s, e, "", "PUBLIC");

    manager.editSingleInstance(
        "start",
        "Standup",
        s,
        "2024-03-11T11:00-04:00[America/New_York]"
    );

    assertEquals(
        ZonedDateTime.parse("2024-03-11T13:00-04:00[America/New_York]"),
        manager.findBySubjectAt(
            "Standup",
            ZonedDateTime.parse("2024-03-11T11:00-04:00[America/New_York]")
        ).end()
    );
  }
}
