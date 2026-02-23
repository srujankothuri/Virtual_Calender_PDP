package calendar.controller.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.model.CalendarService;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link ShowCalendarDashboardCommand} without Mockito.
 * Uses a real CalendarService + CalendarManager and a tiny view double.
 */
public final class ShowCalendarDashboardCommandTest {

  /** Use a stable locale for percentage formatting, etc. */
  @BeforeClass
  public static void setLocale() {
    Locale.setDefault(Locale.US);
  }

  /** Null line should return false and not touch the view. */
  @Test
  public void returnsFalse_whenLineIsNull() {
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CalendarManager mgr = managerWithEmptyModel();
    final CapturingView out = new CapturingView();

    final boolean ok = cmd.tryRun(null, mgr, out);

    assertFalse(ok);
    assertTrue(out.infos.isEmpty());
    assertTrue(out.errors.isEmpty());
  }

  /** Wrong prefix should return false and not touch the view. */
  @Test
  public void returnsFalse_whenNotPrefixed() {
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CalendarManager mgr = managerWithEmptyModel();
    final CapturingView out = new CapturingView();

    final boolean ok = cmd.tryRun(
        "show dash from 2025-01-01 to 2025-01-02", mgr, out);

    assertFalse(ok);
    assertTrue(out.infos.isEmpty());
    assertTrue(out.errors.isEmpty());
  }

  /** Missing " to " delimiter should print usage and return true. */
  @Test
  public void usageError_whenMissingToDelimiter() {
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CalendarManager mgr = managerWithEmptyModel();
    final CapturingView out = new CapturingView();

    final String line =
        "show calendar dashboard from 2025-01-01 until 2025-01-02";
    final boolean ok = cmd.tryRun(line, mgr, out);

    assertTrue(ok);
    assertTrue(out.errors.stream()
        .anyMatch(s -> s.contains("Usage: show calendar dashboard")));
  }

  /**
   * Happy path: parse both dates, call manager.analyze, and print formatted info.
   * Also checks date range and subject bucket appear in the output.
   */
  @Test
  public void happyPath_callsAnalyze_andPrintsInfo() {
    final CalendarService model = new CalendarService();
    model.createCalendar("Work", ZoneId.of("UTC"));
    model.useCalendar("Work");

    final LocalDate d = LocalDate.of(2025, 1, 1);
    final ZonedDateTime s = d.atTime(9, 0).atZone(ZoneId.of("UTC"));
    model.createSingle("x", s, s.plusMinutes(30), "", "online", "PUBLIC");

    final CalendarManager mgr = new CalendarManager(model);
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CapturingView out = new CapturingView();

    final String line =
        "show calendar dashboard from 2025-01-01 to 2025-01-01";
    final boolean ok = cmd.tryRun(line, mgr, out);

    assertTrue(ok);
    assertEquals(1, out.infos.size());
    final String msg = out.infos.get(0);
    assertTrue(msg.contains(
        "Calendar dashboard from 2025-01-01 to 2025-01-01"));
    assertTrue(msg.contains("Total events: 1"));
    assertTrue(msg.contains("By subject:\n  x: 1"));
    assertTrue(out.errors.isEmpty());
  }

  /** Bad date format should hit the parse catch and print the invalid-dates error. */
  @Test
  public void invalidDate_parseFailure_printsError_andReturnsTrue() {
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CalendarManager mgr = managerWithEmptyModel();
    final CapturingView out = new CapturingView();

    final String line =
        "show calendar dashboard from 2025-99-99 to 2025-01-01";
    final boolean ok = cmd.tryRun(line, mgr, out);

    assertTrue(ok);
    assertTrue(out.errors.stream().anyMatch(s -> s.contains("Invalid dates")));
    assertTrue(out.infos.isEmpty());
  }

  /**
   * When from > to, the underlying analytics throws; the command catches and
   * reports "Invalid dates". This exercises the exception path without mocks.
   */
  @Test
  public void managerThrows_printsError_andReturnsTrue() {
    final CalendarService model = new CalendarService();
    model.createCalendar("Work", ZoneId.of("UTC"));
    model.useCalendar("Work");

    final CalendarManager mgr = new CalendarManager(model);
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CapturingView out = new CapturingView();

    final String line =
        "show calendar dashboard from 2025-01-02 to 2025-01-01";
    final boolean ok = cmd.tryRun(line, mgr, out);

    assertTrue(ok);
    assertTrue(out.errors.stream().anyMatch(s -> s.contains("Invalid dates")));
    assertTrue(out.infos.isEmpty());
  }

  /**
   * Verifies online vs not-online (case-insensitive), percentage line, average,
   * weekday + week + month buckets are present and reasonable.
   */
  @Test
  public void percentages_average_and_buckets_arePrinted() {
    final ZoneId z = ZoneId.of("America/New_York");
    final CalendarService model = new CalendarService();
    model.createCalendar("Work", z);
    model.useCalendar("Work");

    final LocalDate day = LocalDate.of(2025, 12, 8); // Monday
    final ZonedDateTime s1 = day.atTime(9, 0).atZone(z);
    final ZonedDateTime s2 = day.atTime(10, 0).atZone(z);
    final ZonedDateTime s3 = day.atTime(11, 0).atZone(z);

    // 3 events on the same day: 2 online (case-insensitive), 1 offline.
    model.createSingle("A", s1, s1.plusHours(1), "", "online", "PUBLIC");
    model.createSingle("B", s2, s2.plusHours(1), "", "ONLINE", "PUBLIC");
    model.createSingle("C", s3, s3.plusHours(1), "", "room", "PUBLIC");

    final CalendarManager mgr = new CalendarManager(model);
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CapturingView out = new CapturingView();

    final String line =
        "show calendar dashboard from 2025-12-08 to 2025-12-08";
    final boolean ok = cmd.tryRun(line, mgr, out);

    assertTrue(ok);
    assertEquals(1, out.infos.size());
    final String msg = out.infos.get(0);

    // Average and percentages.
    assertTrue(msg.contains("Average per day: 3.00"));
    assertTrue(msg.contains("Online: 2 (66.7%), Not online: 1 (33.3%)"));

    // Weekday section includes Monday = 3.
    assertTrue(msg.contains("By weekday:\n  MONDAY: 3"));

    // ISO week section present with a 2025-Wxx key (do not hardcode week number).
    final Pattern wk = Pattern.compile(
        "By ISO week \\(YYYY-Www\\):\\s+2025-W\\d{2}: \\d",
        Pattern.DOTALL);
    assertTrue("Week section missing expected 2025-Wxx line", wk.matcher(msg).find());

    // Month section includes December 2025 bucket with 3.
    assertTrue(msg.contains("By month (YYYY-MM):\n  2025-12: 3"));
  }

  /** Empty range prints N/A for both busiest and least sections. */
  @Test
  public void emptyRange_printsNaForExtrema() {
    final ZoneId z = ZoneId.of("UTC");
    final CalendarService model = new CalendarService();
    model.createCalendar("Work", z);
    model.useCalendar("Work");

    final CalendarManager mgr = new CalendarManager(model);
    final ShowCalendarDashboardCommand cmd = new ShowCalendarDashboardCommand();
    final CapturingView out = new CapturingView();

    final String line =
        "show calendar dashboard from 2025-12-10 to 2025-12-10";
    final boolean ok = cmd.tryRun(line, mgr, out);

    assertTrue(ok);
    assertEquals(1, out.infos.size());
    final String msg = out.infos.get(0);
    assertTrue(msg.contains("Total events: 0"));
    assertTrue(msg.contains("Busiest day(s): N/A"));
    assertTrue(msg.contains("Least busy day(s): N/A"));
  }

  // --------------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------------

  /** Build a manager backed by an empty in-memory model. */
  private static CalendarManager managerWithEmptyModel() {
    final CalendarService model = new CalendarService();
    model.createCalendar("Work", ZoneId.of("UTC"));
    model.useCalendar("Work");
    return new CalendarManager(model);
  }

  /** Tiny CalendarView that records messages for assertions. */
  private static final class CapturingView implements CalendarView {
    final List<String> infos = new ArrayList<>();
    final List<String> errors = new ArrayList<>();

    @Override
    public void info(final String message) {
      infos.add(message);
    }

    @Override
    public void error(final String message) {
      errors.add(message);
    }
  }
}
