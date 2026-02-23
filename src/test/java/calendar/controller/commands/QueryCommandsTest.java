package calendar.controller.commands;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for query/print/status commands formatting paths.
 *
 * <p>Covers routing fall-through, empty/non-empty results, all-day detection,
 * location-present branch, busy/available status, and error paths.</p>
 */
public final class QueryCommandsTest {

  private static final ZoneId NY = ZoneId.of("America/New_York");
  private static final DateTimeFormatter ISO =
      DateTimeFormatter.ISO_ZONED_DATE_TIME;

  /** Spy view that records info and error messages. */
  private static final class ViewSpy implements CalendarView {
    private final List<String> info = new CopyOnWriteArrayList<>();
    private final List<String> errors = new CopyOnWriteArrayList<>();

    @Override
    public void info(final String line) {
      info.add(line);
    }

    @Override
    public void error(final String line) {
      errors.add(line);
    }

    List<String> infos() {
      return info;
    }

    List<String> errors() {
      return errors;
    }
  }

  private CalendarManager manager;
  private CommandInvoker invoker;
  private ViewSpy view;

  /** Creates a calendar with one 2h event on 2024-03-11 for query scenarios. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    view = new ViewSpy();
    invoker = new CommandInvoker(manager, view);

    manager.createCalendar("Main", NY);
    manager.useCalendar("Main");

    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-11T10:00-04:00[America/New_York]");
    final ZonedDateTime e =
        ZonedDateTime.parse("2024-03-11T12:00-04:00[America/New_York]");
    // description omitted, location empty, PUBLIC
    manager.addSingle("Standup", s, e, "", "PUBLIC");
  }

  /** Prints events and status; both must generate info output. */
  @Test
  public void printBetween_and_status_emitInfo() {
    final boolean ok1 = invoker.execute(
        "print events from 2024-03-11T00:00-04:00[America/New_York] "
            + "to 2024-03-12T00:00-04:00[America/New_York]");
    final boolean ok2 = invoker.execute(
        "show status on 2024-03-11T10:30-04:00[America/New_York]");

    assertTrue(ok1 && ok2);
    assertTrue(view.infos().size() >= 2);
  }

  /** Unknown command should return false (routing fallback). */
  @Test
  public void tryRun_returnsFalseForUnknownCommand() {
    final boolean ok = invoker.execute("noop nothing-here");
    assertFalse(ok);
  }

  /** print on date: empty window emits the "No events on yyyy-MM-dd" line. */
  @Test
  public void printOnDate_empty_emitsNoEventsLine() {
    final boolean ok = invoker.execute("print events on 2024-03-12");
    assertTrue(ok);
    assertTrue(view.infos().stream()
        .anyMatch(s -> s.contains("No events on 2024-03-12")));
  }

  /** print on date: non-empty prints header and a timed line (no location). */
  @Test
  public void printOnDate_nonEmpty_formatsTimedEvent() {
    final boolean ok = invoker.execute("print events on 2024-03-11");
    assertTrue(ok);
    assertTrue(view.infos().stream()
        .anyMatch(s -> s.contains("Events on 2024-03-11")));
    // Should contain a bullet for Standup and time range HH:mm .. HH:mm
    assertTrue(view.infos().stream().anyMatch(s ->
        s.contains("-") && s.contains("Standup")
            && s.contains("from 10:00") && s.contains("to 12:00")));
  }

  /** print between: empty interval prints the "No events between ..." line. */
  @Test
  public void printBetween_empty_emitsNoEventsLine() {
    final ZonedDateTime start =
        ZonedDateTime.parse("2024-03-12T00:00-04:00[America/New_York]");
    final ZonedDateTime end = start.plusDays(1);
    final boolean ok = invoker.execute(
        "print events from " + start.format(ISO) + " to " + end.format(ISO));
    assertTrue(ok);
    assertTrue(view.infos().stream()
        .anyMatch(s -> s.contains("No events between ")));
  }

  /** print between: an all-day event (midnight+24h) prints "(all day)". */
  @Test
  public void printBetween_formatsAllDayEvent_withLocation() {
    // Add all-day with location to hit "(all day)" and " at <loc>" branches.
    final LocalDate day = LocalDate.of(2024, 3, 12);
    manager.addAllDay("Holiday", day, "Room 101", "PUBLIC");

    final ZonedDateTime a = day.atTime(LocalTime.MIN).atZone(NY);
    final ZonedDateTime b = day.plusDays(1).atTime(LocalTime.MIN).atZone(NY);

    final boolean ok = invoker.execute(
        "print events from " + a.format(ISO) + " to " + b.format(ISO));
    assertTrue(ok);
    assertTrue(view.infos().stream().anyMatch(s ->
        s.contains("-") && s.contains("Holiday")
            && s.contains("(all day)") && s.contains(" at Room 101")));
  }

  /** show status: busy during event and available outside the event. */
  @Test
  public void showStatus_reportsBusyAndAvailable() {
    final boolean busyOk = invoker.execute(
        "show status on 2024-03-11T10:30-04:00[America/New_York]");
    final boolean freeOk = invoker.execute(
        "show status on 2024-03-11T08:30-04:00[America/New_York]");

    assertTrue(busyOk && freeOk);
    assertTrue(view.infos().stream().anyMatch(s ->
        s.contains("Status at ") && s.contains("busy")));
    assertTrue(view.infos().stream().anyMatch(s ->
        s.contains("Status at ") && s.contains("available")));
  }

  /**
   * Error paths:
   * - bad date for "on" that still matches the regex (e.g., 2024-02-30),
   * - bad zoned datetimes for "between" and "status" (match pattern, fail parse).
   */
  @Test
  public void errors_onBadDateAndBadDateTimeFormats() {
    // invalid but regex-matching yyyy-MM-dd  triggers catch in handlePrintOnDate
    assertTrue(invoker.execute("print events on 2024-02-30"));

    // invalid zoned datetime tokens (match \S+ but fail ZonedDateTime.parse)
    assertTrue(invoker.execute("print events from NOT_A_ZDT to ALSO_NOT_A_ZDT"));
    assertTrue(invoker.execute("show status on TOTALLY_BAD_ZDT"));

    // We should have at least one date-format error and at least two datetime errors.
    assertTrue(view.errors().stream()
        .anyMatch(s -> s.startsWith("Invalid date format")));
    long dtErrors = view.errors().stream()
        .filter(s -> s.startsWith("Invalid date/time format"))
        .count();
    assertTrue(dtErrors >= 2);
  }

  /** Location-present via timed event using addSingle overload with location. */
  @Test
  public void printBetween_includesLocationWhenPresent() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-13T09:00-04:00[America/New_York]");
    final ZonedDateTime e = s.plusHours(1);
    manager.addSingle("Review", s, e, "Room 101", "PUBLIC");

    final ZonedDateTime a = s.withHour(0).withMinute(0);
    final ZonedDateTime b = a.plusDays(1);

    final boolean ok = invoker.execute(
        "print events from " + a.format(ISO) + " to " + b.format(ISO));
    assertTrue(ok);
    assertTrue(view.infos().stream().anyMatch(t ->
        t.contains("-") && t.contains("Review") && t.contains(" at Room 101")));
  }
}
