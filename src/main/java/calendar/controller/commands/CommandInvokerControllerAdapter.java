package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.controller.GuiController;
import calendar.model.CalendarAnalytics;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;


/**
 * Adapter that exposes {@code GuiController} as CLI-like commands by delegating
 * to a {@code CalendarManager}. This class only translates parameters/flows;
 * it does not own business logic.
 */
public final class CommandInvokerControllerAdapter implements GuiController {

  private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private final CommandInvoker invoker;
  private final CalendarManager manager;

  /**
   * It is an adapter that exposes {@code GuiController} as CLI-like commands.
   */
  public CommandInvokerControllerAdapter(final CommandInvoker invoker,
                                         final CalendarManager manager) {
    this.invoker = invoker;
    this.manager = manager;
  }

  // --------- calendar ops (unchanged from your last version) ---------
  @Override
  public List<String> listCalendars() throws Exception {
    final List<String> out = new ArrayList<>(manager.getCalendarNames());
    Collections.sort(out);
    return out;
  }

  @Override
  public String currentCalendar() {
    return manager.current();
  }

  @Override
  public void createCalendar(final String name, final ZoneId zone) throws Exception {
    submit("create calendar --name " + quoteBare(name) + " --timezone " + zone.getId());
  }

  @Override
  public void useCalendar(final String name) throws Exception {
    submit("use calendar --name " + quoteBare(name));
  }

  @Override
  public void setTimezone(final ZoneId zone) throws Exception {
    final String cur = manager.current();
    if (cur == null || cur.isEmpty()) {
      throw new IllegalStateException("Select a calendar first.");
    }
    submit("edit calendar --name " + quoteBare(cur) + " --property timezone " + zone.getId());
  }

  // --------- create (same as before) ---------
  @Override
  public void createAllDayEvent(final String subject, final LocalDate day) throws Exception {
    submit("create event " + quoted(subject) + " on " + day);
  }

  @Override
  public void createTimedEvent(final String subject,
                               final ZonedDateTime start,
                               final ZonedDateTime end) throws Exception {
    submit("create event " + quoted(subject) + " from " + fmt(start) + " to " + fmt(end));
  }

  @Override
  public void createRecurringByCount(final String subject, final boolean allDay,
                                     final LocalDate day, final LocalTime start,
                                     final LocalTime end, final EnumSet<DayOfWeek> days,
                                     final int count) throws Exception {
    final String repeats = " repeats " + weekdayLetters(days) + " for " + count + " times";
    if (allDay) {
      submit("create event " + quoted(subject) + " on " + day + repeats);
      return;
    }
    final ZonedDateTime zs = day.atTime(start).atZone(manager.currentZone());
    final ZonedDateTime ze = day.atTime(end).atZone(manager.currentZone());
    submit("create event " + quoted(subject) + " from " + fmt(zs) + " to " + fmt(ze) + repeats);
  }

  @Override
  public void createRecurringByUntil(final String subject, final boolean allDay,
                                     final LocalDate day, final LocalTime start,
                                     final LocalTime end, final EnumSet<DayOfWeek> days,
                                     final LocalDate until) throws Exception {
    final String repeats = " repeats " + weekdayLetters(days) + " until " + until;
    if (allDay) {
      submit("create event " + quoted(subject) + " on " + day + repeats);
      return;
    }
    final ZonedDateTime zs = day.atTime(start).atZone(manager.currentZone());
    final ZonedDateTime ze = day.atTime(end).atZone(manager.currentZone());
    submit("create event " + quoted(subject) + " from " + fmt(zs) + " to " + fmt(ze) + repeats);
  }

  // --------- edit single (time-anchored) ---------
  @Override
  public void editEventSubjectAtStart(final String anchorSubject,
                                      final ZonedDateTime anchorStart,
                                      final ZonedDateTime anchorEnd,
                                      final String newSubject) throws Exception {
    submit("edit event subject " + subjectToken(anchorSubject)
        + " from " + fmt(anchorStart) + " to " + fmt(anchorEnd)
        + " with " + quoted(newSubject));
  }

  @Override
  public void editEventLocationAtStart(final String anchorSubject,
                                       final ZonedDateTime anchorStart,
                                       final ZonedDateTime anchorEnd,
                                       final String newLocation) throws Exception {
    submit("edit event location " + subjectToken(anchorSubject)
        + " from " + fmt(anchorStart) + " to " + fmt(anchorEnd)
        + " with " + quoted(newLocation));
  }

  // --------- edit bulk/series ---------
  @Override
  public void editEventsSubjectFrom(final String subject, final ZonedDateTime fromTime,
                                    final String newSubject) throws Exception {
    submit("edit events subject " + subjectToken(subject)
        + " from " + fmt(fromTime) + " with " + quoted(newSubject));
  }

  @Override
  public void editEventsLocationFrom(final String subject, final ZonedDateTime fromTime,
                                     final String newLocation) throws Exception {
    submit("edit events location " + subjectToken(subject)
        + " from " + fmt(fromTime) + " with " + quoted(newLocation));
  }

  @Override
  public void editSeriesSubjectFrom(final String subject, final ZonedDateTime anchorStart,
                                    final String newSubject) throws Exception {
    submit("edit series subject " + subjectToken(subject)
        + " at " + fmt(anchorStart) + " with " + quoted(newSubject));
  }

  @Override
  public void editSeriesLocationFrom(final String subject, final ZonedDateTime anchorStart,
                                     final String newLocation) throws Exception {
    submit("edit series location " + subjectToken(subject)
        + " at " + fmt(anchorStart) + " with " + quoted(newLocation));
  }

  // --------- reschedule (required by GUI) ---------
  @Override
  public void moveEventDateOnDay(final String subject, final LocalDate day,
                                 final LocalDate newDay) throws Exception {
    submit("edit event move-date " + subjectToken(subject) + " on " + day + " to " + newDay);
  }

  @Override
  public void moveEventTimeAtStart(final String subject, final ZonedDateTime anchorStart,
                                   final ZonedDateTime newStart,
                                   final ZonedDateTime newEnd) throws Exception {
    submit("edit event move-time " + subjectToken(subject)
        + " from " + fmt(anchorStart) + " to " + fmt(newStart) + " end " + fmt(newEnd));
  }


  @Override
  public void editEventLocationOnDay(final String subject, final LocalDate day,
                                     final String newLocation) throws Exception {
    submit("edit event location " + subjectToken(subject) + " on " + day
        + " with " + quoted(newLocation));
  }


  @Override
  public String computeAnalyticsText(final LocalDate from, final LocalDate to) {
    final CalendarAnalytics analytics = manager.analyze(from, to);
    return (analytics == null) ? "" : analytics.formatText();
  }


  @Override
  public void moveSeriesFromDay(final String subject, final LocalDate from, final int delta)
      throws Exception {
    submit("edit series move-days " + subjectToken(subject) + " from " + from + " by " + delta);
  }


  // --------- helpers ---------
  private void submit(final String line) throws Exception {
    if (!invoker.execute(line)) {
      throw new IllegalArgumentException(
          "That action could not be performed. Please check your inputs.");
    }
  }

  private static String fmt(final ZonedDateTime z) {
    return z.format(ISO_OFFSET);
  }

  private static String quoted(final String s) {
    return "\"" + s.replace("\"", "\\\"") + "\"";
  }

  private static String quoteBare(final String s) {
    return s.trim().replace(' ', '_');
  }

  private static String subjectToken(final String s) {
    return (s.contains(" ") || s.contains("\"")) ? quoted(s) : s;
  }

  private static String weekdayLetters(final EnumSet<DayOfWeek> days) {
    final StringBuilder sb = new StringBuilder();
    if (days.contains(DayOfWeek.MONDAY)) {
      sb.append('M');
    }
    if (days.contains(DayOfWeek.TUESDAY)) {
      sb.append('T');
    }
    if (days.contains(DayOfWeek.WEDNESDAY)) {
      sb.append('W');
    }
    if (days.contains(DayOfWeek.THURSDAY)) {
      sb.append('R');
    }
    if (days.contains(DayOfWeek.FRIDAY)) {
      sb.append('F');
    }
    if (days.contains(DayOfWeek.SATURDAY)) {
      sb.append('S');
    }
    if (days.contains(DayOfWeek.SUNDAY)) {
      sb.append('U');
    }
    return sb.toString();
  }
}
