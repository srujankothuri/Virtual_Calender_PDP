package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles edit operations on events: single instance, all following instances,
 * and entire series.
 */
public final class EditEventCommands implements Command {

  // Flexible subject pattern: either "Quoted Subject" or SingleWord.
  private static final String SUBJECT_PATTERN = "(?:\"([^\"]+)\"|(\\S+))";

  // edit event <property> <subject> from <start> to <end> with <value>
  private static final Pattern SINGLE_EVENT = Pattern.compile(
      "^edit\\s+event\\s+(\\w+)\\s+" + SUBJECT_PATTERN + "\\s+from\\s+(\\S+)"
          + "\\s+to\\s+(\\S+)\\s+with\\s+(.+)$",
      Pattern.CASE_INSENSITIVE);

  // edit events <property> <subject> from <start> with <value>
  private static final Pattern EVENTS_FROM = Pattern.compile(
      "^edit\\s+events\\s+(\\w+)\\s+" + SUBJECT_PATTERN + "\\s+from\\s+(\\S+)"
          + "\\s+with\\s+(.+)$",
      Pattern.CASE_INSENSITIVE);

  // edit series <property> <subject> from <start> with <value>
  private static final Pattern SERIES = Pattern.compile(
      "^edit\\s+series\\s+(\\w+)\\s+" + SUBJECT_PATTERN + "\\s+from\\s+(\\S+)"
          + "\\s+with\\s+(.+)$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager mgr,
                        final CalendarView view) {

    final String trimmed = line.trim();
    if (!trimmed.toLowerCase().startsWith("edit ")) {
      return false;
    }

    // Check active calendar once.
    final ZoneId zone = mgr.currentZone();
    if (zone == null) {
      return true;
    }

    // Try single event edit.
    final Matcher singleMatcher = SINGLE_EVENT.matcher(trimmed);
    if (singleMatcher.matches()) {
      return handleSingleEdit(singleMatcher, mgr, view, zone);
    }

    // Try events from date edit.
    final Matcher eventsMatcher = EVENTS_FROM.matcher(trimmed);
    if (eventsMatcher.matches()) {
      return handleEventsEdit(eventsMatcher, mgr, view, zone);
    }

    // Try series edit.
    final Matcher seriesMatcher = SERIES.matcher(trimmed);
    if (seriesMatcher.matches()) {
      return handleSeriesEdit(seriesMatcher, mgr, view, zone);
    }

    return false;
  }

  /**
   * Handles editing a single event instance.
   */
  private boolean handleSingleEdit(final Matcher matcher,
                                   final CalendarManager mgr,
                                   final CalendarView view,
                                   final ZoneId zone) {
    try {
      final String property = matcher.group(1).toLowerCase();
      final String subject = matcher.group(2) != null
          ? matcher.group(2) : matcher.group(3);
      final ZonedDateTime start =
          parseInZone(matcher.group(4), zone);
      // group(5) is "to" end time, used only for identification in input.
      String value = matcher.group(6).trim();

      if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
        value = value.substring(1, value.length() - 1);
      }

      if (!validateEdit(property, value, view)) {
        return true;
      }

      mgr.editSingleInstance(property, subject, start, value);
      view.info("Edited " + property + " of event \"" + subject + "\" at " + start + ".");
      return true;

    } catch (Exception e) {
      view.error("Failed to edit event: " + e.getMessage());
      return true;
    }
  }

  /**
   * Handles editing events from a specific time onwards.
   * If the event is not part of a series, it edits only the single instance.
   */
  private boolean handleEventsEdit(final Matcher matcher,
                                   final CalendarManager mgr,
                                   final CalendarView view,
                                   final ZoneId zone) {
    try {
      final String property = matcher.group(1).toLowerCase();
      final String subject = matcher.group(2) != null
          ? matcher.group(2) : matcher.group(3);
      final ZonedDateTime fromTime =
          parseInZone(matcher.group(4), zone);
      String value = matcher.group(5).trim();

      if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
        value = value.substring(1, value.length() - 1);
      }

      if (!validateEdit(property, value, view)) {
        return true;
      }

      if (mgr.findBySubjectAt(subject, fromTime) == null) {
        view.error("No event '" + subject + "' found at " + fromTime + ".");
        return true;
      }

      final java.util.List<calendar.model.IEvent> seriesEvents =
          mgr.find(subject, fromTime, fromTime.plusYears(100));

      if (seriesEvents == null || seriesEvents.size() <= 1) {
        mgr.editSingleInstance(property, subject, fromTime, value);
        view.info("Edited " + property + " of event \"" + subject
            + "\" at " + fromTime + ".");
      } else {
        mgr.editEventsFrom(property, subject, fromTime, value);
        view.info("Edited " + property + " of events \"" + subject
            + "\" from " + fromTime + " onwards.");
      }

      return true;

    } catch (Exception e) {
      view.error("Failed to edit events: " + e.getMessage());
      return true;
    }
  }

  /**
   * Handles editing an entire event series.
   * If not a series, it edits only the single instance.
   */
  private boolean handleSeriesEdit(final Matcher matcher,
                                   final CalendarManager mgr,
                                   final CalendarView view,
                                   final ZoneId zone) {
    try {
      final String property = matcher.group(1).toLowerCase();
      final String subject = matcher.group(2) != null
          ? matcher.group(2) : matcher.group(3);
      final ZonedDateTime anyTime =
          parseInZone(matcher.group(4), zone);
      String value = matcher.group(5).trim();

      if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
        value = value.substring(1, value.length() - 1);
      }

      if (!validateEdit(property, value, view)) {
        return true;
      }

      if (mgr.findBySubjectAt(subject, anyTime) == null) {
        view.error("No event '" + subject + "' found at " + anyTime + ".");
        return true;
      }

      final java.util.List<calendar.model.IEvent> seriesEvents =
          mgr.find(subject, anyTime.minusYears(100), anyTime.plusYears(100));

      if (seriesEvents == null || seriesEvents.size() <= 1) {
        mgr.editSingleInstance(property, subject, anyTime, value);
        view.info("Edited " + property + " of event \"" + subject
            + "\" at " + anyTime + ".");
      } else {
        mgr.editEntireSeries(property, subject, anyTime, value);
        view.info("Edited " + property + " of entire series \"" + subject + "\".");
      }

      return true;

    } catch (Exception e) {
      view.error("Failed to edit series: " + e.getMessage());
      return true;
    }
  }

  /**
   * Validates the edit property and value.
   */
  private boolean validateEdit(final String property,
                               final String value,
                               final CalendarView view) {
    switch (property) {
      case "subject":
        if (value.isEmpty()) {
          view.error("Subject cannot be empty.");
          return false;
        }
        break;

      case "start":
      case "end":
        try {
          ZonedDateTime.parse(value);
        } catch (Exception ex) {
          // Accept local too; parse to confirm but we do not need the result here.
          try {
            java.time.LocalDateTime.parse(value);
          } catch (Exception ex2) {
            view.error("Invalid date/time for " + property + ": " + value);
            return false;
          }
        }
        break;

      case "status":
        if (!"PUBLIC".equals(value) && !"PRIVATE".equals(value)) {
          view.error("Status must be PUBLIC or PRIVATE.");
          return false;
        }
        break;

      case "description":
      case "location":
        // Any string allowed.
        break;

      default:
        view.error("Unknown property: " + property
            + ". Valid: subject, start, end, description, location, status.");
        return false;
    }
    return true;
  }

  /**
   * Parses a date-time as ZonedDateTime if it has an offset, otherwise interprets
   * it as a local date-time in the provided zone.
   */
  private static ZonedDateTime parseInZone(final String text, final ZoneId zone) {
    final boolean hasOffset = text.matches(".*([zZ]|[+-]\\d{2}:?\\d{2})$");
    if (hasOffset) {
      return ZonedDateTime.parse(text);
    }
    return java.time.LocalDateTime.parse(text).atZone(zone);
  }
}
