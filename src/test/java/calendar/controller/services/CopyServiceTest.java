package calendar.controller.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CopyService} using a tiny in-memory {@link CalendarModel} test double.
 *
 * <p>Note on units: {@link CopyService} preserves duration exactly; this test compares hours,
 * so 7,200 seconds == 2 hours.
 */
public final class CopyServiceTest {

  private InMemoryModel model;
  private CopyService service;
  private final ZoneId ny = ZoneId.of("America/New_York");
  private final ZoneId la = ZoneId.of("America/Los_Angeles");

  /** Create a source and target calendar and point the model at Source. */
  @Before
  public void setUp() {
    model = new InMemoryModel();
    model.createCalendar("Source", ny);
    model.createCalendar("Target", la);
    model.useCalendar("Source");
    service = new CopyService(model);
  }

  /** Copying a single event into another calendar must preserve duration. */
  @Test
  public void copySingle_intoOtherCalendar_preservesDuration() {
    // arrange: 2h meeting in Source
    final ZonedDateTime s = ZonedDateTime.of(2024, 3, 11, 10, 0, 0, 0, ny);
    final ZonedDateTime e = s.plusHours(2);
    model.createSingle("meet", s, e, "", "", "PUBLIC");

    // act: copy to target at different local time
    final ZonedDateTime targetStart = ZonedDateTime.of(2024, 3, 12, 9, 0, 0, 0, la);
    service.copySingleEvent("meet", s, "Target", targetStart);

    // assert: event exists in target with 2 hours duration
    model.useCalendar("Target");
    final Optional<IEvent> got = model.findBySubjectAt("meet", targetStart);
    assertTrue("copied event should exist", got.isPresent());
    final long hours = Duration.between(got.get().start(), got.get().end()).toHours();
    assertEquals(2L, hours);
  }

  // ---------------------------------------------------------------------------
  // Test double for CalendarModel used only by CopyService paths
  // ---------------------------------------------------------------------------

  /** Minimal in-memory impl that satisfies the CalendarModel surface for this test. */
  private static final class InMemoryModel implements CalendarModel {

    /** A single calendar with a name, zone, and its events. */
    private static final class Cal {
      final List<IEvent> events = new ArrayList<>();
      String name;
      ZoneId zone;

      Cal(final String n, final ZoneId z) {
        this.name = n;
        this.zone = z;
      }
    }

    private final List<Cal> calendars = new ArrayList<>();
    private Cal current;

    // ---- helpers ------------------------------------------------------------

    private Cal get(final String name) {
      for (Cal c : calendars) {
        if (c.name.equals(name)) {
          return c;
        }
      }
      throw new IllegalArgumentException("No calendar " + name);
    }

    /** Test-only creator for seeding calendars. */
    public void createCalendar(final String name, final ZoneId zone) {
      calendars.add(new Cal(name, zone));
      if (current == null) {
        current = calendars.get(calendars.size() - 1);
      }
    }

    // ---- CalendarModel: calendar management --------------------------------

    @Override
    public String current() {
      return current == null ? null : current.name;
    }

    @Override
    public void useCalendar(final String name) {
      this.current = get(name);
    }

    @Override
    public ZoneId currentZone() {
      return current.zone;
    }

    @Override
    public void renameCalendar(final String oldName, final String newName) {
      final Cal c = get(oldName);
      c.name = newName;
    }

    @Override
    public void setTimezone(final String name, final ZoneId zone) {
      final Cal c = get(name);
      c.zone = zone;
    }

    @Override
    public List<String> getCalendarNames() {
      final List<String> names = new ArrayList<>();
      for (Cal c : calendars) {
        names.add(c.name);
      }
      return names;
    }

    // ---- CalendarModel: queries used by CopyService -------------------------

    @Override
    public Optional<IEvent> findBySubjectAt(
        final String subject, final ZonedDateTime start) {
      for (IEvent e : current.events) {
        if (e.subject().equals(subject) && e.start().equals(start)) {
          return Optional.of(e);
        }
      }
      return Optional.empty();
    }

    @Override
    public List<IEvent> eventsOn(final LocalDate day) {
      final List<IEvent> out = new ArrayList<>();
      for (IEvent e : current.events) {
        if (e.start().toLocalDate().equals(day)) {
          out.add(e);
        }
      }
      return out;
    }

    /** Interface-required version: between two instants. */
    @Override
    public List<IEvent> eventsBetween(
        final ZonedDateTime from, final ZonedDateTime to) {
      final List<IEvent> out = new ArrayList<>();
      for (IEvent e : current.events) {
        final ZonedDateTime start = e.start();
        if (!start.isBefore(from) && !start.isAfter(to)) {
          out.add(e);
        }
      }
      return out;
    }

    /** Interface-required (older paths): between two dates inclusive. */
    @Override
    public List<IEvent> eventsBetween(final LocalDate from, final LocalDate to) {
      final List<IEvent> out = new ArrayList<>();
      for (IEvent e : current.events) {
        final LocalDate d = e.start().toLocalDate();
        if (!d.isBefore(from) && !d.isAfter(to)) {
          out.add(e);
        }
      }
      return out;
    }

    @Override
    public boolean isBusyAt(final ZonedDateTime time) {
      for (IEvent e : current.events) {
        final boolean startsOrBefore = !time.isBefore(e.start());
        final boolean beforeEnd = time.isBefore(e.end());
        if (startsOrBefore && beforeEnd) {
          return true;
        }
      }
      return false;
    }

    // ---- CalendarModel: mutations used by CopyService -----------------------

    @Override
    public void createSingle(
        final String subject,
        final ZonedDateTime start,
        final ZonedDateTime end,
        final String description,
        final String location,
        final String status) {
      current.events.add(new E(subject, start, end, description, location, status));
    }

    // ---- Newer CalendarModel surface (unused in this test) ------------------

    /**
     * Stub to satisfy the interface; not used by {@link CopyServiceTest}.
     * Throws to make accidental use obvious during tests.
     */
    @Override
    public calendar.model.CalendarAnalytics computeAnalytics(
        final LocalDate from, final LocalDate to) {
      throw new UnsupportedOperationException(
          "computeAnalytics() is not used in CopyService tests");
    }

    // ---- Unused CalendarModel methods (no-ops for this test) ----------------

    @Override
    public void deleteBySubjectAt(final String s, final ZonedDateTime t) {
    }

    @Override
    public List<IEvent> find(
        final String s, final ZonedDateTime a, final ZonedDateTime b) {
      return new ArrayList<>();
    }

    @Override
    public List<IEvent> findSeriesFrom(final String s, final ZonedDateTime f) {
      return new ArrayList<>();
    }

    @Override
    public List<IEvent> findEntireSeries(final String s, final ZonedDateTime any) {
      return new ArrayList<>();
    }

    @Override
    public void createAllDay(
        final String s, final LocalDate d, final String de, final String l, final String st) {
    }

    @Override
    public void createRecurringSeriesCount(
        final String s,
        final ZonedDateTime a,
        final ZonedDateTime b,
        final EnumSet<DayOfWeek> days,
        final int c,
        final String de,
        final String l,
        final String st) {
    }

    @Override
    public void createRecurringSeriesUntil(
        final String s,
        final ZonedDateTime a,
        final ZonedDateTime b,
        final EnumSet<DayOfWeek> days,
        final LocalDate u,
        final String de,
        final String l,
        final String st) {
    }

    @Override
    public void createRecurringAllDaySeriesCount(
        final String s,
        final LocalDate d,
        final EnumSet<DayOfWeek> days,
        final int c,
        final String de,
        final String l,
        final String st) {
    }

    @Override
    public void createRecurringAllDaySeriesUntil(
        final String s,
        final LocalDate d,
        final EnumSet<DayOfWeek> days,
        final LocalDate u,
        final String de,
        final String l,
        final String st) {
    }

    // Helpers retained without @Override on purpose, because the interface does not
    // declare these exact "edit" entry points; controller/services usually handle them.

    /** No-op helper. */
    public void editSingleInstance(
        final String p, final String s, final ZonedDateTime t, final String v) {
    }

    /** No-op helper. */
    public void editEventsFrom(
        final String p, final String s, final ZonedDateTime f, final String v) {
    }

    /** No-op helper. */
    public void editEntireSeries(
        final String p, final String s, final ZonedDateTime any, final String v) {
    }
  }

  /** Simple concrete event for the in-memory model. */
  private static final class E implements IEvent {
    private final String subject;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String description;
    private final String location;
    private final String status;

    E(
        final String subject,
        final ZonedDateTime start,
        final ZonedDateTime end,
        final String description,
        final String location,
        final String status) {
      this.subject = subject;
      this.start = start;
      this.end = end;
      this.description = description;
      this.location = location;
      this.status = status;
    }

    @Override
    public String subject() {
      return subject;
    }

    @Override
    public ZonedDateTime start() {
      return start;
    }

    @Override
    public ZonedDateTime end() {
      return end;
    }

    @Override
    public String description() {
      return description;
    }

    @Override
    public String location() {
      return location;
    }

    @Override
    public String status() {
      return status;
    }
  }
}
