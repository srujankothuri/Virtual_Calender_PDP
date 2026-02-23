package calendar.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

/** Check basic helpers in {@link DateTimes}. */
public class DateTimesTest {

  /**
   * Parses a date and verifies ISO formatting helper.
   * We only assert stable substrings (date and hour) to avoid
   * brittleness across offset or zone rendering.
   */
  @Test
  public void parseDate_andIso() {
    LocalDate d = DateTimes.parseDate("2024-03-12");
    assertEquals(2024, d.getYear());
    assertEquals(3, d.getMonthValue());
    assertEquals(12, d.getDayOfMonth());

    ZoneId tz = ZoneId.of("UTC");
    ZonedDateTime z = ZonedDateTime.of(2024, 3, 12, 10, 0, 0, 0, tz);
    String iso = DateTimes.iso(z);

    // Be tolerant of formats like:
    //  - 2024-03-12T10:00:00+00:00
    //  - 2024-03-12T10:00Z[UTC]
    //  - 2024-03-12T10:00+00:00[UTC]
    assertTrue("date fragment missing", iso.contains("2024-03-12"));
    assertTrue("time fragment missing", iso.contains("T10:00"));
  }
}
