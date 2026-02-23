package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.model.IEvent;
import calendar.util.DateTimes;
import calendar.util.DayCodes;            // use the shared parser
import calendar.view.CalendarView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles create-event commands (single, all-day, recurring).
 *
 * <p>Signature matches {@link Command#tryRun(String, CalendarManager, CalendarView)}.
 * On errors, we print a message and keep the REPL alive; success messages are printed
 * only when the manager call returns without throwing and a post-check confirms the
 * event exists.</p>
 */
public final class CreateEventCommand implements Command {

  // Body after the literal "repeats "
  private static final Pattern REPEATS_FOR = Pattern.compile(
      "\\A([MTWRFSU,\\s]+)\\s+for\\s+(\\d+)\\s+times?\\z",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern REPEATS_UNTIL = Pattern.compile(
      "\\A([MTWRFSU,\\s]+)\\s+until\\s+(\\d{4}-\\d{2}-\\d{2})\\z",
      Pattern.CASE_INSENSITIVE);

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager mgr,
                        final CalendarView view) {
    final String trimmed = line.trim();
    if (!trimmed.toLowerCase().startsWith("create event ")) {
      return false;
    }

    try {
      String body = trimmed.substring("create event ".length()).trim();
      if (body.isEmpty()) {
        view.error("Expected 'from ... to ...' or 'on <date>' after subject.");
        return true;
      }

      final String subject;
      if (body.startsWith("\"")) {
        final int end = body.indexOf('"', 1);
        if (end < 0) {
          view.error("Missing closing quote in subject.");
          return true;
        }
        subject = body.substring(1, end);
        body = body.substring(end + 1).trim();
      } else {
        final String[] parts = body.split("\\s+", 2);
        subject = parts[0];
        body = parts.length > 1 ? parts[1] : "";
      }

      // ----- HARD GUARD: must have an active calendar before parsing/creating anything -----
      final String activeName = mgr.getCurrentCalendarName(); // swap to mgr.current()
      if (activeName == null) {
        view.error("No active calendar selected. Create one with "
            + "'create calendar --name <name> --timezone <ZoneId>' and then select it with "
            + "'use calendar --name <name>' before creating events.");
        return true;
      }

      if (body.toLowerCase().startsWith("from ")) {
        return handleFromTo(subject, body.substring(5).trim(), mgr, view);
      } else if (body.toLowerCase().startsWith("on ")) {
        return handleAllDay(subject, body.substring(3).trim(), mgr, view);
      }

      view.error("Expected 'from ... to ...' or 'on <date>' after subject.");
      return true;

    } catch (final DateTimeParseException dt) {
      view.error("Bad date/time: " + dt.getParsedString());
      return true;
    } catch (final IllegalArgumentException iae) {
      view.error(iae.getMessage());
      return true;
    }
  }

  // ---------- "from <ZDT> to <ZDT> [repeats ...]" ----------

  private boolean handleFromTo(final String subject,
                               final String tail,
                               final CalendarManager mgr,
                               final CalendarView view) {
    final int toIdx = tail.toLowerCase().indexOf(" to ");
    if (toIdx < 0) {
      view.error("Expected 'from ... to ...' format.");
      return true;
    }

    final String startStr = tail.substring(0, toIdx).trim();
    final String afterTo = tail.substring(toIdx + 4).trim();

    // Parse start using the active calendar's timezone if no zone is present
    final ZoneId zone = mgr.getCurrentTimezone(); // or mgr.currentZone()
    final ZonedDateTime start = DateTimes.parseDateTime(startStr, zone);

    final int space = afterTo.indexOf(' ');
    final String endStr = (space < 0) ? afterTo : afterTo.substring(0, space);
    final String maybeRepeats =
        (space < 0) ? "" : afterTo.substring(space + 1).trim();

    // Parse end using the active calendar's timezone if no zone is present
    final ZonedDateTime end = DateTimes.parseDateTime(endStr, zone);

    if (maybeRepeats.isEmpty()) {
      // Timed single-instance creation + post-check before printing success.
      try {
        mgr.addSingle(subject, start, end, "", "PUBLIC"); // keep your existing API
        if (existsTimed(mgr, subject, start, end)) {
          view.info("Created event: \"" + subject + "\".");
        } else {
          view.error("Event was not created (no active calendar or validation failed).");
        }
        return true;
      } catch (final RuntimeException ex) {
        view.error(ex.getMessage());
        return true;
      }
    }

    if (!maybeRepeats.toLowerCase().startsWith("repeats ")) {
      view.error("Expected 'repeats ...' after end time.");
      return true;
    }

    final String body = maybeRepeats.substring("repeats ".length()).trim();

    final Matcher mFor = REPEATS_FOR.matcher(body);
    if (mFor.matches()) {
      final EnumSet<DayOfWeek> days = DayCodes.parse(mFor.group(1));
      final int count = Integer.parseInt(mFor.group(2));
      try {
        mgr.createRecurringSeries(
            subject, start, end, days, count, null, "", "", "PUBLIC");
        view.info("Created recurring event series: \"" + subject + "\" for " + count + " times.");
        return true;
      } catch (final RuntimeException ex) {
        view.error(ex.getMessage());
        return true;
      }
    }

    final Matcher mUntil = REPEATS_UNTIL.matcher(body);
    if (mUntil.matches()) {
      final EnumSet<DayOfWeek> days = DayCodes.parse(mUntil.group(1));
      // FIX: use group(2) directly (no mUntil.get())
      final LocalDate until = LocalDate.parse(mUntil.group(2));
      try {
        mgr.createRecurringSeries(
            subject, start, end, days, null, until, "", "", "PUBLIC");
        view.info("Created recurring event series: \"" + subject + "\" until " + until + ".");
        return true;
      } catch (final RuntimeException ex) {
        view.error(ex.getMessage());
        return true;
      }
    }

    view.error(
        "Bad repeats clause. Use 'repeats <days> for <N> times' or 'repeats <days> until <date>'.");
    return true;
  }

  // ---------- "on <YYYY-MM-DD> [repeats ...]" ----------

  private boolean handleAllDay(final String subject,
                               final String tail,
                               final CalendarManager mgr,
                               final CalendarView view) {
    final int idx = tail.toLowerCase().indexOf(" repeats ");
    final String dateStr = (idx < 0) ? tail.trim() : tail.substring(0, idx).trim();
    final String repeats =
        (idx < 0) ? null : tail.substring(idx + " repeats ".length()).trim();

    final LocalDate day = LocalDate.parse(dateStr);

    // Single all-day (post-check before saying "Created")
    if (repeats == null || repeats.isEmpty()) {
      try {
        mgr.addAllDay(subject, day);
        if (existsAllDay(mgr, subject, day)) {
          view.info("Created all-day event: \"" + subject + "\" on " + day + ".");
        } else {
          view.error("Event was not created (no active calendar or validation failed).");
        }
        return true;
      } catch (final RuntimeException ex) {
        view.error(ex.getMessage());
        return true;
      }
    }

    // All-day, repeats for N times
    final Matcher mFor = REPEATS_FOR.matcher(repeats);
    if (mFor.matches()) {
      final EnumSet<DayOfWeek> days = DayCodes.parse(mFor.group(1));
      final int count = Integer.parseInt(mFor.group(2));
      try {
        mgr.createRecurringAllDaySeries(
            subject, day, days, count, null, "", "", "PUBLIC");
        view.info("Created recurring all-day series: \"" + subject + "\" for " + count + " times.");
        return true;
      } catch (final RuntimeException ex) {
        view.error(ex.getMessage());
        return true;
      }
    }

    // All-day, repeats until date
    final Matcher mUntil = REPEATS_UNTIL.matcher(repeats);
    if (mUntil.matches()) {
      final EnumSet<DayOfWeek> days = DayCodes.parse(mUntil.group(1));
      final LocalDate until = LocalDate.parse(mUntil.group(2));
      try {
        mgr.createRecurringAllDaySeries(
            subject, day, days, null, until, "", "", "PUBLIC");
        view.info("Created recurring all-day series: \"" + subject + "\" until " + until + ".");
        return true;
      } catch (final RuntimeException ex) {
        view.error(ex.getMessage());
        return true;
      }
    }

    view.error(
        "Bad repeats clause. Use 'repeats <days> for <N> times' or "
            + "'repeats <days> until <date>'.");
    return true;
  }

  // ---------- helpers ----------

  /** Minimal post-check to guard success prints in single all-day path. */
  private static boolean existsAllDay(final CalendarManager mgr,
                                      final String subject,
                                      final LocalDate day) {
    try {
      final List<IEvent> list = mgr.eventsOn(day);
      for (IEvent e : list) {
        if (subject.equals(e.subject())) {
          return true;
        }
      }
      return false;
    } catch (final RuntimeException ex) {
      // If the manager throws here (e.g., no active calendar), treat as not created.
      return false;
    }
  }

  /** Minimal post-check to guard success prints in timed single path. */
  private static boolean existsTimed(final CalendarManager mgr,
                                     final String subject,
                                     final ZonedDateTime start,
                                     final ZonedDateTime end) {
    try {
      final List<IEvent> list = mgr.eventsOn(start.toLocalDate());
      for (IEvent e : list) {
        if (subject.equals(e.subject())
            && start.equals(e.start())
            && end.equals(e.end())) {
          return true;
        }
      }
      return false;
    } catch (final RuntimeException ex) {
      return false;
    }
  }
}
