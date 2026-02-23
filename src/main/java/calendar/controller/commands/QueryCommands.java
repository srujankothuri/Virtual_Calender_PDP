package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.model.IEvent;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles all query and system commands including print and show status.
 * This consolidates query-related commands into a single file.
 */
public final class QueryCommands implements Command {

  // Patterns for different query commands
  private static final Pattern PRINT_ON_DATE = Pattern.compile(
      "^print\\s+events\\s+on\\s+(\\d{4}-\\d{2}-\\d{2})$",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern PRINT_BETWEEN = Pattern.compile(
      "^print\\s+events\\s+from\\s+(\\S+)\\s+to\\s+(\\S+)$",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern SHOW_STATUS = Pattern.compile(
      "^show\\s+status\\s+on\\s+(\\S+)$",
      Pattern.CASE_INSENSITIVE);

  // Formatters for output
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager manager,
                        final CalendarView view) {
    final String trimmed = line.trim();

    // Check for print events on date
    Matcher matcher = PRINT_ON_DATE.matcher(trimmed);
    if (matcher.matches()) {
      return handlePrintOnDate(matcher, manager, view);
    }

    // Check for print events between dates
    matcher = PRINT_BETWEEN.matcher(trimmed);
    if (matcher.matches()) {
      return handlePrintBetween(matcher, manager, view);
    }

    // Check for show status
    matcher = SHOW_STATUS.matcher(trimmed);
    if (matcher.matches()) {
      return handleShowStatus(matcher, manager, view);
    }

    return false;
  }

  /**
   * Handle print events on a specific date.
   */
  private boolean handlePrintOnDate(final Matcher matcher,
                                    final CalendarManager manager,
                                    final CalendarView view) {
    try {
      final LocalDate date = LocalDate.parse(matcher.group(1));
      final List<IEvent> events = manager.getEventsOn(date);

      if (events.isEmpty()) {
        view.info("No events on " + date);
      } else {
        view.info("Events on " + date + ":");
        printEventsList(events, view, true);
      }

      return true;

    } catch (Exception e) {
      view.error("Invalid date format: " + e.getMessage());
      return true;
    }
  }

  /**
   * Handle print events between two date/times.
   */
  private boolean handlePrintBetween(final Matcher matcher,
                                     final CalendarManager manager,
                                     final CalendarView view) {
    try {
      final ZonedDateTime start = ZonedDateTime.parse(matcher.group(1));
      final ZonedDateTime end = ZonedDateTime.parse(matcher.group(2));

      final List<IEvent> events = manager.getEventsBetween(start, end);

      if (events.isEmpty()) {
        view.info("No events between " + start + " and " + end);
      } else {
        view.info("Events from " + start + " to " + end + ":");
        printEventsList(events, view, false);
      }

      return true;

    } catch (Exception e) {
      view.error("Invalid date/time format: " + e.getMessage());
      return true;
    }
  }

  /**
   * Handle show status command.
   */
  private boolean handleShowStatus(final Matcher matcher,
                                   final CalendarManager manager,
                                   final CalendarView view) {
    try {
      final ZonedDateTime time = ZonedDateTime.parse(matcher.group(1));
      final boolean busy = manager.isBusyAt(time);

      view.info("Status at " + time + ": " + (busy ? "busy" : "available"));

      return true;

    } catch (Exception e) {
      view.error("Invalid date/time format: " + e.getMessage());
      return true;
    }
  }

  /**
   * Print a list of events with proper formatting.
   *
   * @param events the events to print
   * @param view the view for output
   * @param simpleFormat true for simple format (on date), false for detailed
   */
  private void printEventsList(final List<IEvent> events,
                               final CalendarView view,
                               final boolean simpleFormat) {
    for (IEvent event : events) {
      final StringBuilder sb = new StringBuilder("- ");
      sb.append(event.subject());

      // Check if it's an all-day event by seeing if it spans exactly 24 hours
      // and starts at midnight
      final long durationHours = java.time.Duration.between(
          event.start(), event.end()).toHours();
      final boolean isAllDay = durationHours == 24
          && event.start().getHour() == 0
          && event.start().getMinute() == 0;

      if (isAllDay) {
        sb.append(" (all day)");
      } else if (simpleFormat) {
        // Simple format for "on date" query
        sb.append(" from ").append(event.start().format(TIME_FORMAT));
        sb.append(" to ").append(event.end().format(TIME_FORMAT));
      } else {
        // Detailed format for "between" query
        sb.append(" starting on ").append(event.start().format(DATE_FORMAT));
        sb.append(" at ").append(event.start().format(TIME_FORMAT));
        sb.append(", ending on ").append(event.end().format(DATE_FORMAT));
        sb.append(" at ").append(event.end().format(TIME_FORMAT));
      }

      // Add location if present
      if (event.location() != null && !event.location().isEmpty()) {
        sb.append(" at ").append(event.location());
      }

      view.info(sb.toString());
    }
  }
}