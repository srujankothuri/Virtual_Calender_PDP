package calendar.view;

import calendar.model.IEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Read-only query surface intended for Views/GUI layers.
 * Keeps mutation APIs hidden to preserve MVC separation of concerns.
 */
public interface CalendarReadOnly {
  /**
   * Returns the name of the current/active calendar.
   *
   * @return current calendar name
   */
  String currentCalendarName();

  /**
   * Returns the timezone of the active calendar.
   *
   * @return timezone of the active calendar
   */
  ZoneId currentZone();

  /**
   * Returns all events that occur on the given day in the active calendar.
   *
   * @param day day in the active calendar's timezone
   * @return immutable list view of events
   */
  List<IEvent> eventsOn(LocalDate day);
}
