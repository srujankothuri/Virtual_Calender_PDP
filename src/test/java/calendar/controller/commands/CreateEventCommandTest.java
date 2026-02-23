package calendar.controller.commands;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.model.IEvent;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Before;
import org.junit.Test;

/**
 * Formatting-clean sanity tests for {@link CreateEventCommand}.
 */
public final class CreateEventCommandTest {

  /** Spy that records info and error messages. */
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

  private static final ZoneId NY = ZoneId.of("America/New_York");

  private CalendarManager manager;
  private CommandInvoker invoker;
  private ViewSpy view;

  /** Builds a default calendar and invoker before each test. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    view = new ViewSpy();
    invoker = new CommandInvoker(manager, view);

    manager.createCalendar("Main", NY);
    manager.useCalendar("Main");
  }

  /** Creates a timed event and expects confirmation and real insertion. */
  @Test
  public void createSingleTimed_printsCreated_andEventExists() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-11T10:00-04:00[America/New_York]");
    final ZonedDateTime e =
        ZonedDateTime.parse("2024-03-11T12:00-04:00[America/New_York]");

    final String line = "create event \"Standup\" from " + s + " to " + e;
    final boolean ok = invoker.execute(line);

    assertTrue(ok);
    assertTrue(view.infos().stream().anyMatch(t -> t.contains("Created event")));

    // Prove model insertion happened (kills remove-addSingle mutant).
    final List<IEvent> day = manager.eventsOn(s.toLocalDate());
    assertTrue(day.stream().anyMatch(ev -> "Standup".equals(ev.subject())));
  }

  /** Creates an all-day event and expects a confirmation line. */
  @Test
  public void createAllDay_printsCreated() {
    final LocalDate d = LocalDate.of(2024, 3, 12);
    final String line = "create event \"Holiday\" on " + d;
    final boolean ok = invoker.execute(line);

    assertTrue(ok);
    assertTrue(view.infos().stream()
        .anyMatch(t -> t.contains("Created all-day event")));
  }

  /** Unquoted subject path: create all-day. */
  @Test
  public void createAllDay_unquotedSubject_works() {
    final LocalDate d = LocalDate.of(2024, 3, 13);
    final boolean ok = invoker.execute("create event Leave on " + d);
    assertTrue(ok);
    assertTrue(view.infos().stream()
        .anyMatch(t -> t.contains("Created all-day event")));
  }

  /** Unquoted subject path: timed single. */
  @Test
  public void createTimed_unquotedSubject_works() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-14T09:00-04:00[America/New_York]");
    final ZonedDateTime e = s.plusHours(1);
    final boolean ok = invoker.execute("create event Review from " + s + " to " + e);
    assertTrue(ok);
    assertTrue(view.infos().stream().anyMatch(t -> t.contains("Created event")));
  }

  /** Recurring timed: repeats for N times (info line proves success). */
  @Test
  public void createTimed_recurring_forCount_createsSeries_andPrints() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-11T10:00-04:00[America/New_York]");
    final ZonedDateTime e = s.plusHours(1);
    // IMPORTANT: day letters must be separated by spaces or commas.
    final String cmd =
        "create event \"CS\" from " + s + " to " + e + " repeats M W for 3 times";

    assertTrue(invoker.execute(cmd));
    assertTrue(view.infos().stream()
        .anyMatch(t -> t.contains("Created recurring event series")));
  }

  /** Recurring timed: repeats until a date. */
  @Test
  public void createTimed_recurring_untilDate_createsSeries_andPrints() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-18T10:00-04:00[America/New_York]");
    final ZonedDateTime e = s.plusHours(1);
    final String cmd =
        "create event \"TA\" from " + s + " to " + e + " repeats M W until 2024-03-27";

    assertTrue(invoker.execute(cmd));
    assertTrue(view.infos().stream().anyMatch(t -> t.contains("until 2024-03-27")));
  }

  /** Recurring all-day: repeats for N times. */
  @Test
  public void createAllDay_recurring_forCount_createsSeries() {
    final LocalDate d = LocalDate.of(2024, 3, 12);
    final String cmd = "create event \"Leave\" on " + d + " repeats M, R for 2 times";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.infos().stream()
        .anyMatch(t -> t.contains("recurring all-day series")));
  }

  /** Recurring all-day: repeats until a date. */
  @Test
  public void createAllDay_recurring_untilDate_createsSeries() {
    final LocalDate d = LocalDate.of(2024, 3, 13);
    final String cmd =
        "create event \"Conf\" on " + d + " repeats W until 2024-03-27";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.infos().stream().anyMatch(t -> t.contains("until 2024-03-27")));
  }

  /** Missing closing quote in subject triggers the specific error. */
  @Test
  public void error_missingClosingQuote_reports() {
    final String cmd = "create event \"Oops on 2024-03-12";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.errors().stream()
        .anyMatch(t -> t.contains("Missing closing quote")));
  }

  /** Body missing after subject triggers the expected guidance. */
  @Test
  public void error_expectedAfterSubject_whenNoTail() {
    assertTrue(invoker.execute("create event \"X\""));
    assertTrue(view.errors().stream().anyMatch(t ->
        t.contains("Expected 'from ... to ...' or 'on <date>' after subject.")));
  }

  /** 'from' tail without 'to' triggers format error in from-to handler. */
  @Test
  public void error_fromWithoutTo_reports() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-11T10:00-04:00[America/New_York]");
    // Missing 'to' token between start and end.
    final String cmd =
        "create event \"Bad\" from " + s + " 2024-03-11T12:00-04:00[America/New_York]";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.errors().stream()
        .anyMatch(t -> t.contains("Expected 'from ... to ...'")));
  }

  /** After end time, a non-'repeats' token triggers the specific error. */
  @Test
  public void error_expectedRepeatsAfterEnd_reports() {
    final ZonedDateTime s =
        ZonedDateTime.parse("2024-03-11T10:00-04:00[America/New_York]");
    final ZonedDateTime e = s.plusHours(1);
    final String cmd =
        "create event X from " + s + " to " + e + " because reasons";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.errors().stream().anyMatch(t -> t.contains("Expected 'repeats")));
  }

  /** Bad repeats clause because day letters are invalid for the regex. */
  @Test
  public void error_badDayLetters_rejectedAsBadRepeatsClause() {
    final LocalDate d = LocalDate.of(2024, 3, 12);
    final String cmd = "create event X on " + d + " repeats Z for 2 times";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.errors().stream()
        .anyMatch(t -> t.contains("Bad repeats clause")));
  }

  /** Top-level DateTimeParseException path: invalid date for 'on'. */
  @Test
  public void error_badDateTime_topCatch_reportsParsedString() {
    // 2024-02-30 is invalid but matches the 'on' pattern.
    final String cmd = "create event X on 2024-02-30";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.errors().stream()
        .anyMatch(t -> t.startsWith("Bad date/time:")));
  }

  /** Unknown command prefix returns false and should not emit any info line. */
  @Test
  public void returnsFalse_whenNotCreateEvent() {
    final boolean ok = invoker.execute("create calendar Main");
    assertFalse(ok);
    assertTrue(view.infos().isEmpty());
  }

  /**
   * Minimal smoke for parseDays with all day letters:
   * accept comma/space-separated tokens and emit the series message.
   */
  @Test
  public void sanity_parseDaysAlphabet_smoke() {
    final LocalDate d = LocalDate.of(2024, 3, 18); // Monday
    final String cmd =
        "create event \"Week\" on " + d
            + " repeats M, T, W, R, F, S, U for 7 times";
    assertTrue(invoker.execute(cmd));
    assertTrue(view.infos().stream()
        .anyMatch(t -> t.contains("recurring all-day series")));
  }
}
