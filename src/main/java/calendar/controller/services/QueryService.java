package calendar.controller.services;

import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Service for querying calendar events.
 * This file should be saved as: calendar/controller/services/QueryService.java
 */
public final class QueryService {

  private final CalendarModel model;

  /**
   * Construct with a calendar model.
   *
   * @param model the calendar model
   */
  public QueryService(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
  }

  /**
   * Get all events on a specific date.
   *
   * @param date the date to query
   * @return list of events on that date (may be empty)
   */
  public List<IEvent> eventsOn(final LocalDate date) {
    return model.eventsOn(date);
  }

  /**
   * Get all events that fall within a time range.
   * Events are included if they overlap with the given time range in any way.
   *
   * @param start the start of the time range
   * @param end the end of the time range
   * @return list of events in the range (may be empty)
   */
  public List<IEvent> eventsBetween(final ZonedDateTime start,
                                    final ZonedDateTime end) {
    return model.eventsBetween(start, end);
  }

  /**
   * Check if the user is busy at a specific time.
   * Returns true if there is at least one event scheduled at the given time.
   *
   * @param time the time to check
   * @return true if busy, false if available
   */
  public boolean isBusyAt(final ZonedDateTime time) {
    return model.isBusyAt(time);
  }
}