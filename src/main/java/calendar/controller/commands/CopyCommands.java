package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copy commands for single day, range, and specific event.
 * Supports both quoted multi-word subjects and unquoted single-word subjects.
 */
public final class CopyCommands implements Command {

  // Flexible subject pattern: either "Quoted Subject" or SingleWord.
  private static final String SUBJECT_PATTERN = "(?:\"([^\"]+)\"|(\\S+))";

  // copy event <subject> on <datetime> --target <calName> to <datetime>
  private static final Pattern ONE = Pattern.compile(
      "^copy\\s+event\\s+" + SUBJECT_PATTERN + "\\s+on\\s+([^\\s]+)"
          + "\\s+--target\\s+(\\S+)\\s+to\\s+([^\\s]+)$",
      Pattern.CASE_INSENSITIVE);

  // copy events on <date> --target <calName> to <date>
  private static final Pattern DAY = Pattern.compile(
      "^copy\\s+events\\s+on\\s+(\\d{4}-\\d{2}-\\d{2})\\s+--target\\s+(\\S+)"
          + "\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})$",
      Pattern.CASE_INSENSITIVE);

  // copy events between <date> and <date> --target <calName> to <date>
  private static final Pattern BETWEEN = Pattern.compile(
      "^copy\\s+events\\s+between\\s+(\\d{4}-\\d{2}-\\d{2})\\s+and\\s+"
          + "(\\d{4}-\\d{2}-\\d{2})\\s+--target\\s+(\\S+)\\s+to\\s+"
          + "(\\d{4}-\\d{2}-\\d{2})$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager manager,
                        final CalendarView view) {

    final String trimmed = line.trim();

    // Check active calendar once.
    final ZoneId zone = manager.currentZone();
    if (zone == null) {
      // Even if it is a cross-calendar copy, we still need a current calendar
      // to read from.
      return true;
    }

    Matcher m = ONE.matcher(trimmed);
    if (m.matches()) {
      final String subject = m.group(1) != null ? m.group(1) : m.group(2);
      final ZonedDateTime srcAt = parseInZone(m.group(3), zone);
      final String target = m.group(4);
      final ZonedDateTime newAt = parseInZone(m.group(5), zone);

      manager.copySingleEventFromCurrent(subject, srcAt, target, newAt);
      view.info("Copied event '" + subject + "' to " + target + ".");
      return true;
    }

    m = DAY.matcher(trimmed);
    if (m.matches()) {
      final LocalDate src = LocalDate.parse(m.group(1));
      final String target = m.group(2);
      final LocalDate dst = LocalDate.parse(m.group(3));
      manager.copyEventsOnDateFromCurrent(src, target, dst);
      view.info("Copied events on " + src + " to " + target + "@" + dst + ".");
      return true;
    }

    m = BETWEEN.matcher(trimmed);
    if (m.matches()) {
      final LocalDate a = LocalDate.parse(m.group(1));
      final LocalDate b = LocalDate.parse(m.group(2));
      final String target = m.group(3);
      final LocalDate dst = LocalDate.parse(m.group(4));
      manager.copyEventsBetweenDatesFromCurrent(a, b, target, dst);
      view.info("Copied events between " + a + " and " + b + " to " + target + ".");
      return true;
    }

    return false;
  }

  /**
   * Parses a date-time as ZonedDateTime if it has an offset, otherwise
   * interprets it as a local date-time in the provided zone.
   */
  private static ZonedDateTime parseInZone(final String text, final ZoneId zone) {
    final boolean hasOffset = text.matches(".*([zZ]|[+-]\\d{2}:?\\d{2})$");
    if (hasOffset) {
      return ZonedDateTime.parse(text);
    }
    return java.time.LocalDateTime.parse(text).atZone(zone);
  }
}
