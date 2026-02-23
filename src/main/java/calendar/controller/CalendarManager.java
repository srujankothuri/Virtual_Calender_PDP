package calendar.controller;

import calendar.controller.services.CalendarService;
import calendar.controller.services.CopyService;
import calendar.controller.services.EventService;
import calendar.controller.services.ExportService;
import calendar.controller.services.QueryService;
import calendar.model.CalendarAnalytics;
import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Facade for calendar operations.
 *
 * <p>Provides a unified interface to various calendar services and is the single
 * entry point used by command classes. The facade keeps controllers decoupled
 * from the underlying model/services and centralizes error handling so that
 * commands can simply report messages to the view instead of crashing.
 */
public final class CalendarManager {

  private final CalendarModel model;
  private final CalendarService calendarService;
  private final EventService eventService;
  private final CopyService copyService;
  private final ExportService exportService;
  private final QueryService queryService;

  /**
   * Creates a manager backed by the in-memory model implementation.
   */
  public CalendarManager() {
    this(new calendar.model.CalendarService());
  }

  /**
   * Creates a manager for a supplied {@link CalendarModel}.
   *
   * @param model calendar model (non-null)
   */
  public CalendarManager(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
    this.calendarService = new CalendarService(model);
    this.eventService = new EventService(model);
    this.copyService = new CopyService(model);
    this.exportService = new ExportService(model);
    this.queryService = new QueryService(model);
  }

  // ========== CALENDAR OPERATIONS ==========

  /**
   * Creates a new calendar.
   *
   * @param name calendar name (must be unique)
   * @param zone timezone in IANA format
   */
  public void createCalendar(final String name, final ZoneId zone) {
    calendarService.create(name, zone);
  }

  /**
   * Renames an existing calendar.
   *
   * @param oldName current name
   * @param newName desired new name
   */
  public void renameCalendar(final String oldName, final String newName) {
    calendarService.rename(oldName, newName);
  }

  /**
   * Sets the timezone for a calendar.
   *
   * @param name calendar name
   * @param zone timezone to set
   */
  public void setTimezone(final String name, final ZoneId zone) {
    calendarService.setTimezone(name, zone);
  }

  /**
   * to perform analysis and show on dashboard.
   */
  public CalendarAnalytics analyze(final LocalDate from, final LocalDate to) {
    // assumes you already have a 'model' field of type CalendarModel
    return model.computeAnalytics(from, to);
  }

  /**
   * Marks a calendar as the active "current" calendar.
   *
   * @param name calendar to use
   */
  public void useCalendar(final String name) {
    calendarService.use(name);
  }

  /**
   * Returns the name of the current calendar.
   *
   * @return current calendar name, or {@code null} if none
   */
  public String getCurrentCalendarName() {
    return calendarService.getCurrentName();
  }

  /**
   * Returns the timezone of the current calendar.
   *
   * @return current calendar timezone, or {@code null} if none
   */

  public ZoneId getCurrentTimezone() {
    return calendarService.getCurrentZone();
  }

  // ========== EVENT CREATION ==========

  /**
   * Adds a single timed event with full details.
   *
   * @param subject     subject
   * @param start       start time
   * @param end         end time
   * @param description description text
   * @param location    location text
   * @param status      {@code PUBLIC} or {@code PRIVATE}
   */
  public void addSingle(final String subject,
                        final ZonedDateTime start,
                        final ZonedDateTime end,
                        final String description,
                        final String location,
                        final String status) {
    eventService.createSingle(subject, start, end, description, location, status);
  }


  /**
   * Adds a single timed event (description omitted).
   *
   * @param subject  subject
   * @param start    start time
   * @param end      end time
   * @param location location text
   * @param status   {@code PUBLIC} or {@code PRIVATE}
   */
  public void addSingle(final String subject,
                        final ZonedDateTime start,
                        final ZonedDateTime end,
                        final String location,
                        final String status) {
    eventService.createSingle(subject, start, end, "", location, status);
  }

  /**
   * Adds an all-day event.
   *
   * @param subject subject
   * @param day     date
   */
  public void addAllDay(final String subject, final LocalDate day) {
    eventService.createAllDay(subject, day);
  }

  /**
   * Adds an all-day event with location and status.
   *
   * @param subject  subject
   * @param day      date
   * @param location location
   * @param status   {@code PUBLIC} or {@code PRIVATE}
   */
  public void addAllDay(final String subject,
                        final LocalDate day,
                        final String location,
                        final String status) {
    eventService.createAllDay(subject, day, "", location, status);
  }

  /**
   * Creates a recurring timed series.
   *
   * @param subject     subject
   * @param start       start time of each instance
   * @param end         end time of each instance
   * @param days        days of week
   * @param count       number of occurrences (nullable if {@code untilDate} used)
   * @param untilDate   last date inclusive (nullable if {@code count} used)
   * @param description description text
   * @param location    location text
   * @param status      {@code PUBLIC} or {@code PRIVATE}
   */
  public void createRecurringSeries(final String subject,
                                    final ZonedDateTime start,
                                    final ZonedDateTime end,
                                    final EnumSet<DayOfWeek> days,
                                    final Integer count,
                                    final LocalDate untilDate,
                                    final String description,
                                    final String location,
                                    final String status) {
    eventService.createRecurringSeries(
        subject, start, end, days, count, untilDate, description, location, status);
  }

  /**
   * Creates a recurring all-day series.
   *
   * @param subject   subject
   * @param startDate first date
   * @param days      days of week
   * @param count     number of occurrences (nullable)
   * @param untilDate last date inclusive (nullable)
   * @param description description text
   * @param location    location text
   * @param status      {@code PUBLIC} or {@code PRIVATE}
   */
  public void createRecurringAllDaySeries(final String subject,
                                          final LocalDate startDate,
                                          final EnumSet<DayOfWeek> days,
                                          final Integer count,
                                          final LocalDate untilDate,
                                          final String description,
                                          final String location,
                                          final String status) {
    eventService.createRecurringAllDaySeries(
        subject, startDate, days, count, untilDate, description, location, status);
  }

  // ========== EVENT EDITING ==========

  /**
   * Edits a single event instance.
   *
   * @param property property to edit (subject/start/end/description/location/status)
   * @param subject  subject to match
   * @param start    start time of the instance
   * @param value    new value
   */
  public void editSingleInstance(final String property,
                                 final String subject,
                                 final ZonedDateTime start,
                                 final String value) {
    eventService.editSingleInstance(property, subject, start, value);
  }

  /**
   * Edits events in a series from a given time onward.
   *
   * @param property property to edit
   * @param subject  subject to match
   * @param fromTime edit from this time (inclusive)
   * @param value    new value
   */
  public void editEventsFrom(final String property,
                             final String subject,
                             final ZonedDateTime fromTime,
                             final String value) {
    eventService.editEventsFrom(property, subject, fromTime, value);
  }

  /**
   * Edits an entire event series.
   *
   * @param property property to edit
   * @param subject  subject to match
   * @param anyTime  any time of an instance in the series
   * @param value    new value
   */
  public void editEntireSeries(final String property,
                               final String subject,
                               final ZonedDateTime anyTime,
                               final String value) {
    eventService.editEntireSeries(property, subject, anyTime, value);
  }

  // ========== COPY OPERATIONS ==========

  /**
   * Copies a single event from the current calendar into a target calendar.
   *
   * @param subject        subject to match
   * @param sourceStart    start time in the source
   * @param targetCalendar target calendar name
   * @param targetStart    start time in the target calendar
   * @return the copied event, or {@code null} if not found
   */
  public IEvent copySingleEventFromCurrent(final String subject,
                                           final ZonedDateTime sourceStart,
                                           final String targetCalendar,
                                           final ZonedDateTime targetStart) {
    return copyService.copySingleEvent(subject, sourceStart, targetCalendar, targetStart);
  }

  /**
   * Copies all events on a date from the current calendar.
   *
   * @param sourceDate     date to copy
   * @param targetCalendar target calendar name
   * @param targetDate     target start date
   * @return copied events
   */
  public List<IEvent> copyEventsOnDateFromCurrent(final LocalDate sourceDate,
                                                  final String targetCalendar,
                                                  final LocalDate targetDate) {
    return copyService.copyEventsOnDate(sourceDate, targetCalendar, targetDate);
  }

  /**
   * Copies events overlapping a date range from the current calendar.
   *
   * @param fromDate       inclusive start date
   * @param toDate         inclusive end date
   * @param targetCalendar target calendar name
   * @param targetDate     target start date
   * @return copied events
   */
  public List<IEvent> copyEventsBetweenDatesFromCurrent(final LocalDate fromDate,
                                                        final LocalDate toDate,
                                                        final String targetCalendar,
                                                        final LocalDate targetDate) {
    return copyService.copyEventsBetween(fromDate, toDate, targetCalendar, targetDate);
  }

  /**
   * Copies a single event (convenience alias).
   *
   * @param subject        subject to match
   * @param sourceStart    start time in the source
   * @param targetCalendar target calendar name
   * @param targetStart    start time in the target calendar
   * @return the copied event, or {@code null} if not found
   */
  public IEvent copySingleEvent(final String subject,
                                final ZonedDateTime sourceStart,
                                final String targetCalendar,
                                final ZonedDateTime targetStart) {
    return copyService.copySingleEvent(subject, sourceStart, targetCalendar, targetStart);
  }

  /**
   * Copies events on a date (convenience alias).
   *
   * @param sourceDate     source date
   * @param targetCalendar target calendar name
   * @param targetDate     target date
   * @return copied events
   */
  public List<IEvent> copyEventsOnDate(final LocalDate sourceDate,
                                       final String targetCalendar,
                                       final LocalDate targetDate) {
    return copyService.copyEventsOnDate(sourceDate, targetCalendar, targetDate);
  }

  /**
   * Copies events across dates (convenience alias).
   *
   * @param fromDate       inclusive start date
   * @param toDate         inclusive end date
   * @param targetCalendar target calendar name
   * @param targetDate     target start date
   * @return copied events
   */
  public List<IEvent> copyEventsBetween(final LocalDate fromDate,
                                        final LocalDate toDate,
                                        final String targetCalendar,
                                        final LocalDate targetDate) {
    return copyService.copyEventsBetween(fromDate, toDate, targetCalendar, targetDate);
  }

  // ========== QUERY OPERATIONS ==========

  /**
   * Returns all events on a specific date in the current calendar.
   *
   * @param date date to query
   * @return events on that date
   */
  public List<IEvent> getEventsOn(final LocalDate date) {
    return queryService.eventsOn(date);
  }

  /**
   * Returns events between two instants in the current calendar.
   *
   * @param start start time (inclusive)
   * @param end   end time (exclusive or inclusive per model semantics)
   * @return events in the interval
   */
  public List<IEvent> getEventsBetween(final ZonedDateTime start,
                                       final ZonedDateTime end) {
    return queryService.eventsBetween(start, end);
  }

  /**
   * Reports whether the current calendar is busy at a given time.
   *
   * @param time time to check
   * @return {@code true} if any event overlaps {@code time}
   */
  public boolean isBusyAt(final ZonedDateTime time) {
    return queryService.isBusyAt(time);
  }

  // ========== EXPORT OPERATIONS ==========

  /**
   * Exports the current calendar to a file. Any {@link IOException} is handled
   * internally and reported by returning {@code null}.
   *
   * @param filename output file path
   * @return absolute path of the exported file, or {@code null} on failure
   */
  public Path export(final String filename) {
    try {
      return exportService.export(filename);
    } catch (IOException ioe) {
      // Swallow and signal failure to the caller (controller/view will print error).
      return null;
    }
  }

  // ========== LEGACY SUPPORT METHODS (for Commands) ==========

  /**
   * Legacy accessor for current calendar name.
   *
   * @return current calendar name
   */
  public String current() {
    return getCurrentCalendarName();
  }

  /**
   * Legacy accessor for current timezone.
   *
   * @return current timezone
   */
  public ZoneId currentZone() {
    return getCurrentTimezone();
  }

  /**
   * Legacy method: returns events on date.
   *
   * @param date date to query
   * @return events on date
   */
  public List<IEvent> eventsOn(final LocalDate date) {
    return getEventsOn(date);
  }

  /**
   * Legacy method: returns events between dates using the raw model.
   *
   * @param from start date inclusive
   * @param to   end date inclusive
   * @return events in range
   */
  public List<IEvent> eventsBetween(final LocalDate from, final LocalDate to) {
    return model.eventsBetween(from, to);
  }

  /**
   * Finds events by subject in a time range.
   *
   * @param subject subject to match
   * @param from    start instant
   * @param to      end instant
   * @return matching events
   */
  public List<IEvent> find(final String subject,
                           final ZonedDateTime from,
                           final ZonedDateTime to) {
    return model.find(subject, from, to);
  }

  /**
   * Finds an event by subject and start time.
   *
   * @param subject subject to match
   * @param start   start instant to match
   * @return event or {@code null} if not found
   */
  public IEvent findBySubjectAt(final String subject,
                                final ZonedDateTime start) {
    return model.findBySubjectAt(subject, start).orElse(null);
  }

  /**
   * Deletes an event by subject and start time.
   *
   * @param subject subject to match
   * @param start   start instant to match
   */
  public void deleteBySubjectAt(final String subject,
                                final ZonedDateTime start) {
    model.deleteBySubjectAt(subject, start);
  }

  /**
   * Returns all calendar names (for GUI dropdowns etc.).
   */
  public List<String> getCalendarNames() {
    return calendarService.getCalendarNames(); // delegate to service
  }

}
