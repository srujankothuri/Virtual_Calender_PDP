// src/main/java/calendar/util/DateTimes.java

package calendar.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Small helpers for parsing and formatting date/time values.
 */
public final class DateTimes {

  private DateTimes() {}

  /**
   * Parses an ISO-8601 zoned date time (must contain zone/offset).
   *
   * @param text input text
   * @return parsed value
   */
  public static ZonedDateTime parseZdt(final String text) {
    return ZonedDateTime.parse(text);
  }

  /**
   * Parse either a full ZonedDateTime (with zone/offset) or a local ISO date-time
   * (YYYY-MM-DDTHH:MM) interpreted in the provided timezone.
   *
   * @param text ISO string
   * @param zone timezone to use when {@code text} has no zone
   * @return parsed zoned date-time
   * @throws DateTimeParseException if parsing fails
   * @throws IllegalArgumentException if {@code zone} is null for a local input
   */
  public static ZonedDateTime parseDateTime(final String text, final ZoneId zone) {
    try {
      return ZonedDateTime.parse(text);
    } catch (final DateTimeParseException zdtFail) {
      final LocalDateTime ldt = LocalDateTime.parse(text); // may throw (good)
      if (zone == null) {
        throw new IllegalArgumentException(
            "No active calendar selected (timezone required to interpret local date-time).");
      }
      return ldt.atZone(zone);
    }
  }

  /**
   * Parses a local date in ISO format (YYYY-MM-DD).
   *
   * @param text input text
   * @return parsed date
   */
  public static LocalDate parseDate(final String text) {
    return LocalDate.parse(text);
  }

  /**
   * Formats a zoned date time as an ISO string.
   *
   * @param zdt value
   * @return ISO string
   */
  public static String iso(final ZonedDateTime zdt) {
    return zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
  }
}
