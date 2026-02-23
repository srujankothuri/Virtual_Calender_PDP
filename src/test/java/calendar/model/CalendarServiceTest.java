package calendar.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

/** End-to-end checks on the in-memory {@link CalendarService}. */
public class CalendarServiceTest {

  private CalendarService model;
  private final ZoneId nyZone = ZoneId.of("America/New_York");

  /** Fresh calendar per test. */
  @Before
  public void setUp() {
    model = new CalendarService();
    model.createCalendar("Work", nyZone);
    model.useCalendar("Work");
  }

  /** Creating and renaming calendars should be robust to odd inputs. */
  @Test
  public void createCalendar_validationAndHappyPath() {
    CalendarService m = new CalendarService();
    m.createCalendar("   ", nyZone);
    m.createCalendar("A", null);
    m.createCalendar("A", nyZone);
    m.createCalendar("A", nyZone);
    assertTrue(m.getCalendarNames().contains("A"));

    m.renameCalendar("Nope", "X");
    m.renameCalendar("A", "   ");
    m.renameCalendar("A", "A");
    m.renameCalendar("A", "B");
    assertTrue(m.getCalendarNames().contains("B"));
    m.useCalendar("B");
    m.renameCalendar("B", "C");
    assertEquals("C", m.current());
  }

  /** Using and changing timezones should not throw for benign cases. */
  @Test
  public void timezoneAndUseCalendar_validations() {
    CalendarService m = new CalendarService();
    m.createCalendar("A", nyZone);
    m.setTimezone("Unknown", nyZone);
    m.setTimezone("A", null);
    m.setTimezone("A", ZoneId.of("UTC"));
    m.useCalendar("Unknown");
    m.useCalendar("A");
    assertEquals("A", m.current());
    assertEquals(ZoneId.of("UTC"), m.currentZone());
  }

  /** Handle nulls and reversed times; non-throwing validation path. */
  @Test
  public void createSingle_validations() {
    ZonedDateTime now = ZonedDateTime.now(nyZone);
    model.createSingle("   ", now, now.plusHours(1), "d", "l", "s");
    model.createSingle("Meet", null, now.plusHours(1), "d", "l", "s");
    model.createSingle("Meet", now, null, "d", "l", "s");
    model.createSingle("Meet", now.plusHours(1), now, "d", "l", "s");
  }

  /** Query helpers should see the created event and return unmodifiable lists. */
  @Test
  public void createSingle_andQueries() {
    ZonedDateTime s = ZonedDateTime.of(2024, 3, 11, 10, 0, 0, 0, nyZone);
    ZonedDateTime e = s.plusHours(2);
    model.createSingle("Office Hour", s, e, "help", "room", "PUBLIC");

    List<IEvent> day = model.eventsOn(s.toLocalDate());
    try {
      day.add(null);
      fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // ok
    }
    assertEquals(1, day.size());

    List<IEvent> range = model.eventsBetween(s.minusDays(1), e.plusDays(1));
    assertEquals(1, range.size());

    List<IEvent> found = model.find("office", s.minusDays(1), e.plusDays(1));
    assertEquals(1, found.size());

    Optional<IEvent> exact = model.findBySubjectAt("Office Hour", s);
    assertTrue(exact.isPresent());

    List<IEvent> series = model.findEntireSeries("Office Hour", s);
    assertEquals(1, series.size());
  }

  /** Validate recurring paths and that some events are produced. */
  @Test
  public void allDayAndRecurring_validationAndHappyPath() {
    model.createAllDay("Holiday", null, "d", "l", "s");

    ZonedDateTime s = ZonedDateTime.of(2024, 3, 12, 14, 0, 0, 0, nyZone);
    ZonedDateTime e = s.plusHours(1);
    EnumSet<DayOfWeek> trDays =
        EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

    model.createRecurringSeriesCount("Labs", null, e, trDays, 4, "d", "l", "s");
    model.createRecurringSeriesCount("Labs", s, null, trDays, 4, "d", "l", "s");
    model.createRecurringSeriesCount("Labs", s, e.plusDays(1), trDays, 4, "d", "l", "s");
    model.createRecurringSeriesCount("Labs", s, e, null, 4, "d", "l", "s");
    model.createRecurringSeriesCount("Labs", s, e,
        EnumSet.noneOf(DayOfWeek.class), 4, "d", "l", "s");
    model.createRecurringSeriesCount("Labs", s, e, trDays, 0, "d", "l", "s");

    model.createRecurringSeriesUntil("Labs", s.plusMinutes(1), e.plusMinutes(1),
        trDays, s.toLocalDate().plusDays(10), "d", "l", "s");

    model.createRecurringSeriesCount("Labs", s, e, trDays, 4, "d", "l", "s");
    List<IEvent> labs = model.find("Labs", s.minusDays(1), e.plusDays(30));
    assertTrue(labs.size() >= 2);

    model.createRecurringAllDaySeriesCount("OH", null, trDays, 3, "d", "l", "s");
    model.createRecurringAllDaySeriesCount("OH", s.toLocalDate(),
        EnumSet.noneOf(DayOfWeek.class), 3, "d", "l", "s");
    model.createRecurringAllDaySeriesCount("OH", s.toLocalDate(), trDays, 0,
        "d", "l", "s");

    model.createRecurringAllDaySeriesCount("OH", s.toLocalDate(), trDays, 3,
        "d", "l", "s");
    List<IEvent> all = model.find("OH", s.minusDays(1), e.plusDays(10));
    assertTrue(all.size() >= 2);

    model.createRecurringAllDaySeriesUntil("OH2", null, trDays,
        s.toLocalDate().plusDays(7), "d", "l", "s");
    model.createRecurringAllDaySeriesUntil("OH2", s.toLocalDate(),
        EnumSet.noneOf(DayOfWeek.class), s.toLocalDate().plusDays(7),
        "d", "l", "s");
    model.createRecurringAllDaySeriesUntil("OH2", s.toLocalDate(), trDays, null,
        "d", "l", "s");
    model.createRecurringAllDaySeriesUntil("OH2", s.toLocalDate(), trDays,
        s.toLocalDate().plusDays(7), "d", "l", "s");
    List<IEvent> all2 = model.find("OH2", s.minusDays(1), e.plusDays(10));
    assertTrue(all2.size() >= 2);
  }

  /** Series searchers and recreate/delete flows. */
  @Test
  public void seriesFindersDeleteAndRecreate() {
    ZonedDateTime s = ZonedDateTime.of(2024, 3, 11, 10, 0, 0, 0, nyZone);
    ZonedDateTime e = s.plusHours(2);
    EnumSet<DayOfWeek> mwDays =
        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

    model.createRecurringSeriesUntil("Standup", s, e, mwDays,
        s.toLocalDate().plusDays(10), "d", "l", "s");

    List<IEvent> seriesFrom = model.findSeriesFrom("Standup", s);
    assertFalse(seriesFrom.isEmpty());

    List<IEvent> entire = model.findEntireSeries("Standup", s);
    assertEquals(seriesFrom.size(), entire.size());

    model.deleteBySubjectAt("Nope", s.minusDays(1));
    model.deleteBySubjectAt("Standup", s);
    Optional<IEvent> gone = model.findBySubjectAt("Standup", s);
    assertFalse(gone.isPresent());

    IEvent keep = entire.get(1);
    model.recreate(keep, "  ", null, null, null, null, null);
    model.recreate(keep, null, null, keep.end(), null, null, null);
    model.recreate(keep, null, keep.start(), null, null, null, null);
    model.recreate(keep, null, keep.end(), keep.start(), null, null, null);

    IEvent twin = new Event.Builder()
        .subject(keep.subject()).start(keep.start()).end(keep.end())
        .description(keep.description()).location(keep.location())
        .status(keep.status()).build();

    model.recreate(keep, twin.subject(), twin.start(), twin.end(),
        twin.description(), twin.location(), twin.status());

    ZonedDateTime ns = keep.start().plusHours(1);
    ZonedDateTime ne = keep.end().plusHours(1);
    model.recreate(keep, keep.subject(), ns, ne, keep.description(),
        keep.location(), keep.status());
    Optional<IEvent> moved = model.findBySubjectAt(keep.subject(), ns);
    assertTrue(moved.isPresent());
  }

  /** Crossing events are included; returned range list is unmodifiable. */
  @Test
  public void eventsBetween_includesCrossingEventsAndIsUnmodifiable() {
    ZonedDateTime s = ZonedDateTime.of(2024, 3, 12, 8, 0, 0, 0, nyZone);
    ZonedDateTime e = s.plusHours(1);
    model.createSingle("A", s, e, "", "", "");
    model.createSingle("B", s.minusMinutes(30), s.plusMinutes(30), "", "", "");
    model.createSingle("C", s.plusMinutes(10), s.plusMinutes(50), "", "", "");

    List<IEvent> window = model.eventsBetween(s, e);
    assertEquals(3, window.size());
    try {
      window.add(null);
      fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // ok
    }
  }

  /** Safe defaults when there is no active calendar. */
  @Test
  public void noActiveCalendar_returnsSafeDefaults() {
    CalendarService m = new CalendarService();
    LocalDate today = LocalDate.now(nyZone);
    ZonedDateTime z1 = today.atStartOfDay(nyZone);
    ZonedDateTime z2 = z1.plusHours(1);

    assertTrue(m.eventsOn(today).isEmpty());
    assertTrue(m.eventsBetween(z1, z2).isEmpty());
    assertTrue(m.find("x", z1, z2).isEmpty());
    assertFalse(m.findBySubjectAt("x", z1).isPresent());
    assertTrue(m.findSeriesFrom("x", z1).isEmpty());
    assertTrue(m.findEntireSeries("x", z1).isEmpty());
  }
}
