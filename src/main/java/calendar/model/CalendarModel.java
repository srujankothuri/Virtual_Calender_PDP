package calendar.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Read/write interface for a multi-calendar model.
 * Implementations manage calendars, timezones, and events.
 */
public interface CalendarModel {

  // ========== CALENDAR OPERATIONS ==========

  /** Create a calendar with a timezone. */
  void createCalendar(String name, ZoneId zone);

  /** Rename an existing calendar. */
  void renameCalendar(String oldName, String newName);

  /** Set the timezone of a calendar. */
  void setTimezone(String name, ZoneId zone);

  /** Switch the active calendar by name. */
  void useCalendar(String name);

  /** Returns the active calendar name (or null if none). */
  String current();

  /** Returns the timezone of the active calendar. */
  ZoneId currentZone();

  // ========== SINGLE EVENT OPERATIONS ==========

  /** Create a single (non-all-day) event in the active calendar. */
  void createSingle(String subject, ZonedDateTime start, ZonedDateTime end,
                    String description, String location, String status);

  /** Create an all-day event in the active calendar. */
  void createAllDay(String subject, LocalDate day,
                    String description, String location, String status);

  // ========== RECURRING EVENT OPERATIONS ==========

  /** Create recurring series with count limit. */
  void createRecurringSeriesCount(String subject, ZonedDateTime start, ZonedDateTime end,
                                  EnumSet<DayOfWeek> days, int count,
                                  String description, String location, String status);

  /** Create recurring series until date. */
  void createRecurringSeriesUntil(String subject, ZonedDateTime start, ZonedDateTime end,
                                  EnumSet<DayOfWeek> days, LocalDate untilDate,
                                  String description, String location, String status);

  /** Create recurring all-day series with count. */
  void createRecurringAllDaySeriesCount(String subject, LocalDate startDate,
                                        EnumSet<DayOfWeek> days, int count,
                                        String description, String location, String status);

  /** Create recurring all-day series until date. */
  void createRecurringAllDaySeriesUntil(String subject, LocalDate startDate,
                                        EnumSet<DayOfWeek> days, LocalDate untilDate,
                                        String description, String location, String status);

  // ========== QUERY OPERATIONS ==========

  /** Find events whose subject contains {@code subject} in the time range. */
  List<IEvent> find(String subject, ZonedDateTime from, ZonedDateTime to);

  /** Find a specific event by subject and exact start timestamp. */
  Optional<IEvent> findBySubjectAt(String subject, ZonedDateTime start);

  /** Find events in a series starting from a given time. */
  List<IEvent> findSeriesFrom(String subject, ZonedDateTime fromTime);

  /** Find all events in a series. */
  List<IEvent> findEntireSeries(String subject, ZonedDateTime anyTime);

  /** All events that occur on a given day in the active calendar. */
  List<IEvent> eventsOn(LocalDate day);

  /** All events whose date is in {@code [from, to]} inclusive. */
  List<IEvent> eventsBetween(LocalDate from, LocalDate to);

  /** All events in time range. */
  List<IEvent> eventsBetween(ZonedDateTime start, ZonedDateTime end);

  /** Check if busy at specific time. */
  boolean isBusyAt(ZonedDateTime time);

  // ========== MODIFICATION OPERATIONS ==========

  /** Delete the event identified by subject and start in the active calendar. */
  void deleteBySubjectAt(String subject, ZonedDateTime start);

  /**
   * Returns all calendar names known to the model.
   *
   * @return list of calendar names (implementation-defined order)
   */
  List<String> getCalendarNames();

  // ========== ANALYTICS ==========

  /**
   * Compute analytics for the active calendar between {@code from} and {@code to} (inclusive).
   *
   * @param from inclusive start day
   * @param to   inclusive end day
   * @return analytics summary object
   * @throws IllegalArgumentException if {@code to} is before {@code from}
   */
  CalendarAnalytics computeAnalytics(LocalDate from, LocalDate to);
}
