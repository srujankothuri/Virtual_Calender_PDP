package calendar.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import calendar.model.CalendarService;
import calendar.model.IEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Thin checks for the read-only view model wrapper. */
public class CalendarViewModelTest {

  private CalendarService model;
  private CalendarViewModel viewModel;
  private final ZoneId tz = ZoneId.of("UTC");

  /** Sets up a single calendar used in the tests. */
  @Before
  public void setUp() {
    model = new CalendarService();
    model.createCalendar("X", tz);
    model.useCalendar("X");
    viewModel = new CalendarViewModel(model);
  }

  /** Current calendar name and zone are forwarded correctly. */
  @Test
  public void exposesCurrentCalendarNameAndZone() {
    assertEquals("X", viewModel.currentCalendarName());
    assertEquals(tz, viewModel.currentZone());

    model.renameCalendar("X", "Y");
    model.useCalendar("Y");
    assertEquals("Y", viewModel.currentCalendarName());
  }

  /** Returned event lists are unmodifiable. */
  @Test
  public void eventsOn_isUnmodifiable() {
    LocalDate day = LocalDate.of(2024, 3, 12);
    model.createSingle(
        "Meet",
        day.atTime(10, 0).atZone(tz),
        day.atTime(11, 0).atZone(tz),
        "d",
        "l",
        "s");

    List<IEvent> list = viewModel.eventsOn(day);
    assertEquals(1, list.size());
    try {
      list.add(null);
      fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // ok
    }
  }
}
