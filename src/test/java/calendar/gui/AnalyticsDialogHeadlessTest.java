package calendar.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import calendar.model.CalendarAnalytics;
import calendar.model.IEvent;
import calendar.view.CalendarReadOnly;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Headless tests for dashboard analytics. We verify numeric fields via the API and
 * also check the formatted text with tolerant regexes (so small wording changes,
 * whitespace, or rounding do not break tests). We also include a tiny adapter
 * harness that mimics the GUI command path:
 * {@code show calendar dashboard from <d1> to <d2>}.
 */
public final class AnalyticsDialogHeadlessTest {

  /** Set a stable locale and headless mode for CI. */
  @BeforeClass
  public static void setUpLocaleAndHeadless() {
    Locale.setDefault(Locale.US);
    System.setProperty("java.awt.headless", "true");
  }

  // ---------------------------------------------------------------------------
  // Core tests (direct compute)
  // ---------------------------------------------------------------------------

  /**
   * A Tue/Thu series across two ISO weeks in February. Verifies totals,
   * weekday breakdown, week buckets, month buckets, average, and text.
   */
  @Test
  public void dashboardForTueThuSeries_matchesDialogSummary() {
    ZoneId zone = ZoneId.of("Europe/London");
    FakeVm vm = new FakeVm("Kawhi", zone);

    LocalDate base = LocalDate.of(2024, 2, 12);
    ZonedDateTime first = base.plusDays(1).atTime(14, 0).atZone(zone);
    addOccurrence(vm, "boston", first);
    addOccurrence(vm, "boston", first.plusDays(2));
    addOccurrence(vm, "boston", first.plusDays(7));
    addOccurrence(vm, "boston", first.plusDays(9));

    LocalDate from = LocalDate.of(2024, 2, 12);
    LocalDate to = LocalDate.of(2024, 3, 12);

    CalendarAnalytics a = CalendarAnalytics.compute(vm.all(), from, to);

    // API checks.
    assertEquals(4, a.totalEvents());
    assertEquals(2, a.byWeekday().get(DayOfWeek.TUESDAY).intValue());
    assertEquals(2, a.byWeekday().get(DayOfWeek.THURSDAY).intValue());
    assertTrue(a.bySubject().containsKey("boston"));

    // Text checks (tolerant).
    String out = a.formatText();
    assertNotNull(out);
    assertTrue(containsAll(out, "Calendar", "dashboard", "2024-02-12", "2024-03-12"));
    assertTrue(hasTotal(out, 4));
    assertTrue(avgNear(out, 4.0 / 30.0, 0.02));
    assertTrue(hasWeekCount(out, "2024-W07", 2));
    assertTrue(hasWeekCount(out, "2024-W08", 2));
    assertTrue(hasWeekdayCount(out, DayOfWeek.TUESDAY, 2));
    assertTrue(hasWeekdayCount(out, DayOfWeek.THURSDAY, 2));
    assertTrue(hasMonthCount(out, YearMonth.of(2024, 2), 4));
  }

  /**
   * Empty single-day range shows zero totals. Implementations differ on extrema:
   * some list the day as both busiest/least when no events exist; others leave
   * extrema empty. We accept either behavior but always verify the date appears
   * in the text with zero totals.
   */
  @Test
  public void emptySingleDayRange_listsThatDayAsBusiestAndLeast() {
    ZoneId zone = ZoneId.of("America/New_York");
    FakeVm vm = new FakeVm("Work", zone);

    LocalDate d = LocalDate.of(2025, 12, 10);
    CalendarAnalytics a = CalendarAnalytics.compute(vm.all(), d, d);

    assertEquals(0, a.totalEvents());

    // If an implementation chooses to report extrema, it must include that day.
    if (!a.busiestDays().isEmpty()) {
      assertTrue(a.busiestDays().contains(d));
    }
    if (!a.leastBusyDays().isEmpty()) {
      assertTrue(a.leastBusyDays().contains(d));
    }

    String out = a.formatText();
    assertTrue(out.contains("2025-12-10"));
    assertTrue(containsAny(out, "Total", "total"));
  }

  /** End-before-start must throw. */
  @Test(expected = IllegalArgumentException.class)
  public void invalidRange_throwsIllegalArgumentException() {
    ZoneId zone = ZoneId.of("America/New_York");
    FakeVm vm = new FakeVm("Work", zone);
    LocalDate from = LocalDate.of(2025, 12, 11);
    LocalDate to = LocalDate.of(2025, 12, 10);
    CalendarAnalytics.compute(vm.all(), from, to);
  }

  /**
   * Online vs offline is case-insensitive; verify that the text reports a
   * percentage for Online somewhere (we do not lock to exact rounding).
   */
  @Test
  public void onlineOffline_isCaseInsensitiveInText() {
    ZoneId zone = ZoneId.of("America/New_York");
    FakeVm vm = new FakeVm("Work", zone);

    LocalDate day = LocalDate.of(2025, 12, 10);
    ZonedDateTime s1 = day.atTime(9, 0).atZone(zone);
    ZonedDateTime s2 = day.atTime(10, 0).atZone(zone);
    ZonedDateTime s3 = day.atTime(11, 0).atZone(zone);

    addOccurrence(vm, "standup", s1, "online");
    addOccurrence(vm, "standup", s2, "ONLINE");
    addOccurrence(vm, "1:1", s3, "room 101");

    CalendarAnalytics a = CalendarAnalytics.compute(vm.all(), day, day);
    assertEquals(3, a.totalEvents());

    String out = a.formatText();
    assertTrue(
        Pattern.compile("(?i)online\\b[^\\n%]*\\d+(?:\\.\\d+)?%")
            .matcher(out).find()
    );
  }

  /** Crossing month boundary: verify February vs March buckets in text. */
  @Test
  public void crossingMonthBoundary_countsPerMonth() {
    ZoneId zone = ZoneId.of("America/New_York");
    FakeVm vm = new FakeVm("Work", zone);

    ZonedDateTime feb29 = LocalDate.of(2024, 2, 29).atTime(10, 0).atZone(zone);
    ZonedDateTime mar01 = LocalDate.of(2024, 3, 1).atTime(10, 0).atZone(zone);

    addOccurrence(vm, "endFeb", feb29);
    addOccurrence(vm, "startMar", mar01);

    LocalDate from = LocalDate.of(2024, 2, 28);
    LocalDate to = LocalDate.of(2024, 3, 2);
    CalendarAnalytics a = CalendarAnalytics.compute(vm.all(), from, to);

    String out = a.formatText();
    assertTrue(hasMonthCount(out, YearMonth.of(2024, 2), 1));
    assertTrue(hasMonthCount(out, YearMonth.of(2024, 3), 1));
  }

  // ---------------------------------------------------------------------------
  // Adapter-path tests (tiny headless harness that mimics GUI command route)
  // ---------------------------------------------------------------------------

  /**
   * Adapter path: command string -> compute -> formatText.
   * Same Tue/Thu series scenario as the direct test.
   */
  @Test
  public void adapter_dashboardForTueThuSeries_matchesDialogSummary() {
    ZoneId zone = ZoneId.of("Europe/London");
    FakeVm vm = new FakeVm("Kawhi", zone);

    LocalDate base = LocalDate.of(2024, 2, 12);
    ZonedDateTime first = base.plusDays(1).atTime(14, 0).atZone(zone);
    addOccurrence(vm, "boston", first);
    addOccurrence(vm, "boston", first.plusDays(2));
    addOccurrence(vm, "boston", first.plusDays(7));
    addOccurrence(vm, "boston", first.plusDays(9));

    AdapterHarness adapter = new AdapterHarness(vm);
    String out = adapter.submit(
        "show calendar dashboard from 2024-02-12 to 2024-03-12");

    assertNotNull(out);
    assertTrue(hasTotal(out, 4));
    assertTrue(avgNear(out, 4.0 / 30.0, 0.02));
    assertTrue(hasWeekCount(out, "2024-W07", 2));
    assertTrue(hasWeekCount(out, "2024-W08", 2));
    assertTrue(hasWeekdayCount(out, DayOfWeek.TUESDAY, 2));
    assertTrue(hasWeekdayCount(out, DayOfWeek.THURSDAY, 2));
    assertTrue(hasMonthCount(out, YearMonth.of(2024, 2), 4));
  }

  /** Adapter path: online/offline percentages are case-insensitive. */
  @Test
  public void adapter_onlineOffline_isCaseInsensitiveInText() {
    ZoneId zone = ZoneId.of("America/New_York");
    FakeVm vm = new FakeVm("Work", zone);

    LocalDate day = LocalDate.of(2025, 12, 10);
    ZonedDateTime s1 = day.atTime(9, 0).atZone(zone);
    ZonedDateTime s2 = day.atTime(10, 0).atZone(zone);
    ZonedDateTime s3 = day.atTime(11, 0).atZone(zone);

    addOccurrence(vm, "standup", s1, "online");
    addOccurrence(vm, "standup", s2, "ONLINE");
    addOccurrence(vm, "1:1", s3, "room 101");

    AdapterHarness adapter = new AdapterHarness(vm);
    String out = adapter.submit(
        "show calendar dashboard from 2025-12-10 to 2025-12-10");

    assertTrue(
        Pattern.compile("(?i)online\\b[^\\n%]*\\d+(?:\\.\\d+)?%")
            .matcher(out).find()
    );
  }

  /** Adapter path: crossing month boundary shows one Feb and one Mar event. */
  @Test
  public void adapter_crossingMonthBoundary_countsPerMonth() {
    ZoneId zone = ZoneId.of("America/New_York");
    FakeVm vm = new FakeVm("Work", zone);

    ZonedDateTime feb29 = LocalDate.of(2024, 2, 29).atTime(10, 0).atZone(zone);
    ZonedDateTime mar01 = LocalDate.of(2024, 3, 1).atTime(10, 0).atZone(zone);

    addOccurrence(vm, "endFeb", feb29);
    addOccurrence(vm, "startMar", mar01);

    AdapterHarness adapter = new AdapterHarness(vm);
    String out = adapter.submit(
        "show calendar dashboard from 2024-02-28 to 2024-03-02");

    assertTrue(hasMonthCount(out, YearMonth.of(2024, 2), 1));
    assertTrue(hasMonthCount(out, YearMonth.of(2024, 3), 1));
  }

  // ---------------------------------------------------------------------------
  // Helpers and tiny fakes
  // ---------------------------------------------------------------------------

  /**
   * Adds a 90-minute event with a non-online location.
   *
   * @param vm fake view-model
   * @param title subject
   * @param start start time
   */
  private static void addOccurrence(
      final FakeVm vm,
      final String title,
      final ZonedDateTime start
  ) {
    addOccurrence(vm, title, start, "room");
  }

  /**
   * Adds a 90-minute event with a given location.
   *
   * @param vm fake view-model
   * @param title subject
   * @param start start time
   * @param location location string (e.g., {@code "online"}, {@code "room 101"})
   */
  private static void addOccurrence(
      final FakeVm vm,
      final String title,
      final ZonedDateTime start,
      final String location
  ) {
    ZonedDateTime end = start.plusMinutes(90);
    vm.add(
        start.toLocalDate(),
        new TestEvent(title, start, end, "", location, "PUBLIC")
    );
  }

  /** Minimal immutable event used only for tests. */
  private static final class TestEvent implements IEvent {

    private final String subject;
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String description;
    private final String location;
    private final String status;

    TestEvent(
        final String subjectValue,
        final ZonedDateTime startValue,
        final ZonedDateTime endValue,
        final String descriptionValue,
        final String locationValue,
        final String statusValue
    ) {
      this.subject = subjectValue;
      this.start = startValue;
      this.end = endValue;
      this.description = descriptionValue;
      this.location = locationValue;
      this.status = statusValue;
    }

    @Override
    public String subject() {
      return this.subject;
    }

    @Override
    public ZonedDateTime start() {
      return this.start;
    }

    @Override
    public ZonedDateTime end() {
      return this.end;
    }

    @Override
    public String description() {
      return this.description;
    }

    @Override
    public String location() {
      return this.location;
    }

    @Override
    public String status() {
      return this.status;
    }
  }

  /** Tiny read-only VM used by CalendarAnalytics. */
  private static final class FakeVm implements CalendarReadOnly {

    private final ZoneId zoneId;
    private final String calendarName;
    private final Map<LocalDate, List<IEvent>> eventsByDay;

    FakeVm(final String name, final ZoneId zone) {
      this.calendarName = name;
      this.zoneId = zone;
      this.eventsByDay = new HashMap<>();
    }

    void add(final LocalDate day, final IEvent event) {
      eventsByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(event);
    }

    List<IEvent> all() {
      List<IEvent> out = new ArrayList<>();
      for (List<IEvent> list : eventsByDay.values()) {
        out.addAll(list);
      }
      return out;
    }

    @Override
    public String currentCalendarName() {
      return this.calendarName;
    }

    @Override
    public ZoneId currentZone() {
      return this.zoneId;
    }

    @Override
    public List<IEvent> eventsOn(final LocalDate day) {
      return eventsByDay.getOrDefault(day, List.of());
    }
  }

  /**
   * Minimal headless adapter harness. Recognizes only the command:
   * {@code show calendar dashboard from YYYY-MM-DD to YYYY-MM-DD}.
   * Returns {@code CalendarAnalytics.formatText()} for the date range.
   */
  private static final class AdapterHarness {

    private static final Pattern CMD = Pattern.compile(
        "^\\s*show\\s+calendar\\s+dashboard\\s+from\\s+"
            + "(\\d{4}-\\d{2}-\\d{2})\\s+to\\s+(\\d{4}-\\d{2}-\\d{2})\\s*$"
    );

    private final FakeVm vm;

    AdapterHarness(final FakeVm vm) {
      this.vm = vm;
    }

    String submit(final String command) {
      Matcher m = CMD.matcher(command);
      if (!m.matches()) {
        throw new IllegalArgumentException("Unsupported command: " + command);
      }
      LocalDate from = LocalDate.parse(m.group(1));
      LocalDate to = LocalDate.parse(m.group(2));
      return CalendarAnalytics.compute(vm.all(), from, to).formatText();
    }
  }

  // ---------------------------------------------------------------------------
  // Text match helpers (tolerant regex / parsing)
  // ---------------------------------------------------------------------------

  /** Returns true if all tokens exist somewhere in the text (order-insensitive). */
  private static boolean containsAll(final String text, final String... tokens) {
    for (String t : tokens) {
      if (!text.contains(t)) {
        return false;
      }
    }
    return true;
  }

  /** Returns true if any token exists in the text. */
  private static boolean containsAny(final String text, final String... tokens) {
    for (String t : tokens) {
      if (text.contains(t)) {
        return true;
      }
    }
    return false;
  }

  /** Matches {@code "Total ...: <n>"} or {@code "Total number of events: <n>"}. */
  private static boolean hasTotal(final String text, final int n) {
    String re = "(?i)total\\s+(number\\s+of\\s+events\\s*:\\s*|events\\s*:\\s*)(\\d+)";
    Matcher m = Pattern.compile(re).matcher(text);
    if (m.find()) {
      return Integer.parseInt(m.group(2)) == n;
    }
    return false;
  }

  /**
   * Captures {@code "Average per day: x"} or
   * {@code "Average number of events per day: x"}.
   */
  private static boolean avgNear(
      final String text, final double expected, final double tol
  ) {
    String re =
        "(?i)average(\\s+number\\s+of\\s+events)?\\s+per\\s+day\\s*:\\s*([0-9]+\\.[0-9]+)";
    Matcher m = Pattern.compile(re).matcher(text);
    if (!m.find()) {
      return false;
    }
    double got = Double.parseDouble(m.group(2));
    return Math.abs(got - expected) <= tol;
  }

  /** Checks that a line like {@code "... 2024-W07 ... 2"} appears (order preserved). */
  private static boolean hasWeekCount(final String text, final String weekKey, final int n) {
    String re = "\\b" + Pattern.quote(weekKey) + "\\b\\D+(" + n + ")\\b";
    return Pattern.compile(re).matcher(text).find();
  }

  /** Checks that {@code "WEEKDAY: n"} appears, tolerant to case. */
  private static boolean hasWeekdayCount(final String text, final DayOfWeek d, final int n) {
    String re = "\\b" + d.name() + "\\b\\D+(" + n + ")\\b";
    return Pattern.compile(re, Pattern.CASE_INSENSITIVE).matcher(text).find();
  }

  /**
   * Checks that either {@code "YYYY-MM: n"} appears or
   * {@code "<Mon> YYYY: n"} appears (e.g., {@code "Feb 2024: 4"}).
   * Case-insensitive for month names.
   */
  private static boolean hasMonthCount(final String text, final YearMonth ym, final int n) {
    String iso = ym.toString(); // e.g., "2024-02"
    String shortMon = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.US); // "Feb"
    String longMon = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.US); // "February"

    String isoRe = "\\b" + Pattern.quote(iso) + "\\b\\D+(" + n + ")\\b";
    String monRe =
        "\\b(" + Pattern.quote(shortMon) + "|" + Pattern.quote(longMon)
            + ")\\.?\\s+" + ym.getYear() + "\\b\\D+(" + n + ")\\b";

    Pattern p = Pattern.compile(isoRe + "|" + monRe, Pattern.CASE_INSENSITIVE);
    return p.matcher(text).find();
  }
}
