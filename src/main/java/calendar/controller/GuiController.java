package calendar.controller;

import calendar.model.CalendarAnalytics;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Thin controller port used by the Swing view.
 *
 * <p>The GUI only talks to this interface; implementations can translate the
 * calls to CLI strings or hit services directly. Keep parsing and business
 * rules out of the view.</p>
 */
public interface GuiController {

  // ---------- Calendar ops ----------

  /**
   * List available calendars.
   *
   * @return names of calendars
   * @throws Exception on failure
   */
  List<String> listCalendars() throws Exception;

  /**
   * Get the name of the current active calendar.
   *
   * @return current calendar name
   */
  String currentCalendar();

  /**
   * Create a new calendar with a time zone.
   *
   * @param name calendar name
   * @param zone calendar time zone
   * @throws Exception on failure
   */
  void createCalendar(String name, ZoneId zone) throws Exception;

  /**
   * Switch the active calendar by name.
   *
   * @param name calendar name to use
   * @throws Exception on failure
   */
  void useCalendar(String name) throws Exception;

  /**
   * Change the active calendar's time zone.
   *
   * @param zone new time zone
   * @throws Exception on failure
   */
  void setTimezone(ZoneId zone) throws Exception;

  // ---------- Create ----------

  /**
   * Create an all-day event on a specific date.
   *
   * @param subject event title
   * @param day     calendar date
   * @throws Exception on failure
   */
  void createAllDayEvent(String subject, LocalDate day) throws Exception;

  /**
   * Create a timed event.
   *
   * @param subject event title
   * @param start   start time (with zone)
   * @param end     end time (with zone)
   * @throws Exception on failure
   */
  void createTimedEvent(String subject, ZonedDateTime start, ZonedDateTime end) throws Exception;

  /**
   * Create a recurring event for a fixed number of occurrences.
   *
   * @param subject  event title
   * @param allDay   true if all-day
   * @param day      first date
   * @param start    local start time (ignored if all-day)
   * @param end      local end time (ignored if all-day)
   * @param days     weekdays on which it recurs
   * @param count    number of occurrences
   * @throws Exception on failure
   */
  void createRecurringByCount(String subject, boolean allDay, LocalDate day,
                              LocalTime start, LocalTime end,
                              EnumSet<DayOfWeek> days, int count) throws Exception;

  /**
   * Create a recurring event until (and including) a given end date.
   *
   * @param subject  event title
   * @param allDay   true if all-day
   * @param day      first date
   * @param start    local start time (ignored if all-day)
   * @param end      local end time (ignored if all-day)
   * @param days     weekdays on which it recurs
   * @param until    final date to include
   * @throws Exception on failure
   */
  void createRecurringByUntil(String subject, boolean allDay, LocalDate day,
                              LocalTime start, LocalTime end,
                              EnumSet<DayOfWeek> days, LocalDate until) throws Exception;

  // ---------- Edit (single, time-anchored) ----------

  /**
   * Rename a single event instance identified by its original start/end.
   *
   * @param anchorSubject subject of the anchored instance
   * @param anchorStart   original start time (with zone)
   * @param anchorEnd     original end time (with zone)
   * @param newSubject    new subject/title
   * @throws Exception on failure
   */
  void editEventSubjectAtStart(String anchorSubject, ZonedDateTime anchorStart,
                               ZonedDateTime anchorEnd, String newSubject) throws Exception;

  /**
   * Change location of a single event instance identified by its original start/end.
   *
   * @param anchorSubject subject of the anchored instance
   * @param anchorStart   original start time (with zone)
   * @param anchorEnd     original end time (with zone)
   * @param newLocation   new location text
   * @throws Exception on failure
   */
  void editEventLocationAtStart(String anchorSubject, ZonedDateTime anchorStart,
                                ZonedDateTime anchorEnd, String newLocation) throws Exception;

  // ---------- Edit (bulk/series) ----------

  /**
   * Update subject for all instances from a given start time (inclusive).
   *
   * @param subject   series subject to match
   * @param fromTime  first instance start to modify
   * @param newSubject new subject/title
   * @throws Exception on failure
   */
  void editEventsSubjectFrom(String subject, ZonedDateTime fromTime, String newSubject)
      throws Exception;

  /**
   * Update location for all instances from a given start time (inclusive).
   *
   * @param subject     series subject to match
   * @param fromTime    first instance start to modify
   * @param newLocation new location text
   * @throws Exception on failure
   */
  void editEventsLocationFrom(String subject, ZonedDateTime fromTime, String newLocation)
      throws Exception;

  /**
   * Update subject for a whole series identified by an anchor start time.
   *
   * @param subject     series subject to match
   * @param anchorStart anchor instance start (with zone)
   * @param newSubject  new subject/title
   * @throws Exception on failure
   */
  void editSeriesSubjectFrom(String subject, ZonedDateTime anchorStart, String newSubject)
      throws Exception;


  /* ---------- Analytics ---------- */

  /**
   * Compute a formatted dashboard string for the active calendar in the
   * inclusive date range {@code [from, to]}.
   *
   * <p>Implementations should keep model access out of the GUI layer; for
   * example, an adapter may delegate to an application manager/service that
   * gathers events and computes analytics.</p>
   *
   * @param from inclusive start date
   * @param to   inclusive end date
   * @return formatted dashboard text
   * @throws Exception on failure
   */
  String computeAnalyticsText(LocalDate from, LocalDate to) throws Exception;

  /**
   * Update location for a whole series identified by an anchor start time.
   *
   * @param subject     series subject to match
   * @param anchorStart anchor instance start (with zone)
   * @param newLocation new location text
   * @throws Exception on failure
   */
  void editSeriesLocationFrom(String subject, ZonedDateTime anchorStart, String newLocation)
      throws Exception;

  // ---------- Reschedule (required by GUI) ----------

  /**
   * Move a single instance to a new date, preserving its local start/end.
   *
   * @param subject series subject
   * @param day     day containing the instance to move
   * @param newDay  new calendar date
   * @throws Exception on failure
   */
  void moveEventDateOnDay(String subject, LocalDate day, LocalDate newDay) throws Exception;

  /**
   * Move a single instance by specifying exact new start/end.
   *
   * @param subject    subject of the anchored instance
   * @param anchorStart original start time (with zone)
   * @param newStart   new start time (with zone)
   * @param newEnd     new end time (with zone)
   * @throws Exception on failure
   */
  void moveEventTimeAtStart(String subject, ZonedDateTime anchorStart,
                            ZonedDateTime newStart, ZonedDateTime newEnd) throws Exception;

  // ---------- Optional series helpers ----------
  // GUI falls back to time-anchored variants if unsupported

  /**
   * Rename a single instance chosen by day (GUI helper).
   *
   * @param subject    subject of the instance
   * @param day        calendar date containing the instance
   * @param newSubject new subject/title
   * @throws Exception on failure
   */
  default void editEventSubjectOnDay(String subject, LocalDate day, String newSubject)
      throws Exception {
    throw new UnsupportedOperationException("editEventSubjectOnDay not supported.");
  }

  /**
   * Change location of a single instance chosen by day (GUI helper).
   *
   * @param subject     subject of the instance
   * @param day         calendar date containing the instance
   * @param newLocation new location text
   * @throws Exception on failure
   */
  default void editEventLocationOnDay(String subject, LocalDate day, String newLocation)
      throws Exception {
    throw new UnsupportedOperationException("editEventLocationOnDay not supported.");
  }



  /**
   * Move future instances in a series by whole days, starting from a date.
   *
   * @param subject series subject
   * @param from    first date to modify (inclusive)
   * @param delta   days to shift (positive or negative)
   * @throws Exception on failure
   */
  default void moveSeriesFromDay(String subject, LocalDate from, int delta) throws Exception {
    throw new UnsupportedOperationException("moveSeriesFromDay not supported.");
  }


}
