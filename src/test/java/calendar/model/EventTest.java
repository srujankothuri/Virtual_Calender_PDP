package calendar.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

/** Contract tests for equality, hash, toString, and compareTo. */
public class EventTest {

  /** Same subject + start + end are considered equal; ordering by subject works. */
  @Test
  public void equalsHashCodeToStringAndComparable() {
    ZoneId tz = ZoneId.of("UTC");
    ZonedDateTime s = ZonedDateTime.of(2024, 1, 1, 9, 0, 0, 0, tz);
    ZonedDateTime e = s.plusHours(1);

    IEvent a = new Event.Builder()
        .subject("Meet").start(s).end(e)
        .description("d").location("l").status("PUBLIC").build();

    IEvent b = new Event.Builder()
        .subject("Meet").start(s).end(e)
        .description("d2").location("l2").status("PRIVATE").build();

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.toString().contains("Meet@"));

    IEvent c = new Event.Builder()
        .subject("aaa").start(s).end(e)
        .description("").location("").status("").build();

    assertTrue(c.compareTo(a) < 0);
  }
}
