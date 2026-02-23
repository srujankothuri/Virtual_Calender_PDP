package calendar.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable analytics summary for a date range of a calendar.
 *
 * <p>This class is intentionally <b>pure compute</b>: it does not pull data from the model or any
 * view/VM port. Callers gather {@link IEvent} instances for the inclusive date range and pass them
 * to {@link #compute(Iterable, LocalDate, LocalDate)}. The instance can then be formatted with
 * {@link #formatText()} or inspected via getters.</p>
 */
public final class CalendarAnalytics {

  private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  /** Inclusive start and end dates for this summary. */
  private final LocalDate from;
  private final LocalDate to;

  private final int totalEvents;
  private final Map<String, Integer> bySubject;
  private final Map<DayOfWeek, Integer> byWeekday;
  private final Map<String, Integer> byWeek; // labels like 2025-W50 (ISO week)
  private final Map<YearMonth, Integer> byMonth;
  private final double avgPerDay;
  private final List<LocalDate> busiestDays;    // all days that tie for the max
  private final List<LocalDate> leastBusyDays;  // all days that tie for the min (zeros included)
  private final int onlineCount;
  private final int offlineCount;
  private final double pctOnline;               // 0..100
  private final double pctOffline;              // 0..100

  private CalendarAnalytics(
      final LocalDate from,
      final LocalDate to,
      final int totalEvents,
      final Map<String, Integer> bySubject,
      final Map<DayOfWeek, Integer> byWeekday,
      final Map<String, Integer> byWeek,
      final Map<YearMonth, Integer> byMonth,
      final double avgPerDay,
      final List<LocalDate> busiestDays,
      final List<LocalDate> leastBusyDays,
      final int onlineCount,
      final int offlineCount,
      final double pctOnline,
      final double pctOffline) {
    this.from = from;
    this.to = to;
    this.totalEvents = totalEvents;
    this.bySubject = Collections.unmodifiableMap(bySubject);
    this.byWeekday = Collections.unmodifiableMap(byWeekday);
    this.byWeek = Collections.unmodifiableMap(byWeek);
    this.byMonth = Collections.unmodifiableMap(byMonth);
    this.avgPerDay = avgPerDay;
    this.busiestDays = Collections.unmodifiableList(busiestDays);
    this.leastBusyDays = Collections.unmodifiableList(leastBusyDays);
    this.onlineCount = onlineCount;
    this.offlineCount = offlineCount;
    this.pctOnline = pctOnline;
    this.pctOffline = pctOffline;
  }

  /**
   * Compute analytics for the inclusive date interval {@code [from, to]} using the supplied
   * events. Callers are free to pass a superset; this method filters to the range.
   *
   * @param events iterable of events (may contain events outside the range)
   * @param from   inclusive start date
   * @param to     inclusive end date
   * @return immutable analytics summary
   * @throws IllegalArgumentException if dates are null or {@code to} is before {@code from}
   */
  public static CalendarAnalytics compute(
      final Iterable<IEvent> events,
      final LocalDate from,
      final LocalDate to) {

    Objects.requireNonNull(events, "events");
    if (from == null || to == null) {
      throw new IllegalArgumentException("Dates must not be null.");
    }
    if (to.isBefore(from)) {
      throw new IllegalArgumentException("End date must not be before start date.");
    }

    final Map<String, Integer> bySubject = new LinkedHashMap<>();
    final Map<DayOfWeek, Integer> byWeekday = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek d : DayOfWeek.values()) {
      byWeekday.put(d, 0);
    }
    final Map<String, Integer> byWeek = new LinkedHashMap<>();
    final Map<YearMonth, Integer> byMonth = new LinkedHashMap<>();
    final Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
    final WeekFields wf = WeekFields.ISO;

    // Initialize all days in range to 0 so empties are accounted for.
    LocalDate cursor = from;
    while (!cursor.isAfter(to)) {
      byDay.put(cursor, 0);
      cursor = cursor.plusDays(1);
    }

    int total = 0;
    int online = 0;

    for (IEvent e : events) {
      final LocalDate d = e.start().toLocalDate();
      if (d.isBefore(from) || d.isAfter(to)) {
        continue;
      }
      total++;

      // Day count
      byDay.put(d, byDay.get(d) + 1);

      // Subject
      final String subj = safe(e.subject());
      bySubject.put(subj, bySubject.getOrDefault(subj, 0) + 1);

      // Weekday
      final DayOfWeek dow = d.getDayOfWeek();
      byWeekday.put(dow, byWeekday.get(dow) + 1);

      // ISO week (year-week)
      final int week = d.get(wf.weekOfWeekBasedYear());
      final int wyear = d.get(wf.weekBasedYear());
      final String wkey = String.format(Locale.ROOT, "%04d-W%02d", wyear, week);
      byWeek.put(wkey, byWeek.getOrDefault(wkey, 0) + 1);

      // Month
      final YearMonth ym = YearMonth.from(d);
      byMonth.put(ym, byMonth.getOrDefault(ym, 0) + 1);

      // Online / offline
      if (isOnline(e.location())) {
        online++;
      }
    }

    final int days = (int) (to.toEpochDay() - from.toEpochDay()) + 1;
    final double avg = days == 0 ? 0.0 : ((double) total) / days;

    final List<LocalDate> busiest = new ArrayList<>();
    final List<LocalDate> least = new ArrayList<>();
    if (total > 0) {
      int max = 0;
      int min = Integer.MAX_VALUE;
      for (int count : byDay.values()) {
        if (count > max) {
          max = count;
        }
        if (count < min) {
          min = count;
        }
      }
      if (min == Integer.MAX_VALUE) {
        min = 0;
      }
      for (Map.Entry<LocalDate, Integer> en : byDay.entrySet()) {
        if (en.getValue() == max) {
          busiest.add(en.getKey());
        }
        if (en.getValue() == min) {
          least.add(en.getKey());
        }
      }
      busiest.sort(Comparator.naturalOrder());
      least.sort(Comparator.naturalOrder());
    }
    // else: by contract, both lists remain empty

    final int offline = total - online;
    final double pctOn = total == 0 ? 0.0 : (100.0 * online) / total;
    final double pctOff = total == 0 ? 0.0 : 100.0 - pctOn;

    final Map<String, Integer> subjSorted = sortByKey(bySubject);
    final Map<String, Integer> weekSorted = sortByKey(byWeek);
    final Map<YearMonth, Integer> monthSorted = sortByKey(byMonth);

    return new CalendarAnalytics(
        from, to, total, subjSorted, byWeekday, weekSorted, monthSorted,
        avg, busiest, least, online, offline, pctOn, pctOff);
  }

  /**
   * Returns a readable multi-line text report covering all metrics.
   *
   * @return human-friendly analytics text
   */
  public String formatText() {
    final String ls = System.lineSeparator();
    final StringBuilder sb = new StringBuilder(512);
    sb.append("Calendar dashboard from ")
        .append(DAY_FMT.format(from)).append(" to ").append(DAY_FMT.format(to))
        .append(ls).append(ls);

    sb.append("Total events: ").append(totalEvents).append(ls);
    sb.append(String.format(Locale.ROOT, "Average per day: %.2f", avgPerDay)).append(ls);

    // Online/offline with counts and 1-decimal percentages
    sb.append(String.format(Locale.ROOT, "Online: %d (%.1f%%), Not online: %d (%.1f%%)",
        onlineCount, pctOnline, offlineCount, pctOffline)).append(ls);

    // Busiest / least busy days (N/A if none)
    sb.append("Busiest day(s): ")
        .append(busiestDays.isEmpty() ? "N/A" : joinDates(busiestDays)).append(ls);
    sb.append("Least busy day(s): ")
        .append(leastBusyDays.isEmpty() ? "N/A" : joinDates(leastBusyDays)).append(ls);

    // Breakdowns
    sb.append(ls).append("By subject:").append(ls);
    for (Map.Entry<String, Integer> en : bySubject.entrySet()) {
      sb.append("  ").append(en.getKey()).append(": ").append(en.getValue()).append(ls);
    }

    sb.append(ls).append("By weekday:").append(ls);
    for (DayOfWeek d : DayOfWeek.values()) {
      sb.append("  ").append(d).append(": ").append(byWeekday.get(d)).append(ls);
    }

    sb.append(ls).append("By ISO week (YYYY-Www):").append(ls);
    for (Map.Entry<String, Integer> en : byWeek.entrySet()) {
      sb.append("  ").append(en.getKey()).append(": ").append(en.getValue()).append(ls);
    }

    sb.append(ls).append("By month (YYYY-MM):").append(ls);
    final List<YearMonth> months = new ArrayList<>(byMonth.keySet());
    Collections.sort(months);
    for (YearMonth ym : months) {
      sb.append("  ").append(ym).append(": ").append(byMonth.get(ym)).append(ls);
    }

    return sb.toString();
  }

  /* ------------------------------- Getters -------------------------------- */

  /**
   * Inclusive start date used for this summary.
   *
   * @return start date
   */
  public LocalDate from() {
    return from;
  }

  /**
   * Inclusive end date used for this summary.
   *
   * @return end date
   */
  public LocalDate to() {
    return to;
  }

  /**
   * Total number of events in the range.
   *
   * @return total event count
   */
  public int totalEvents() {
    return totalEvents;
  }

  /**
   * Subject breakdown (subject → count).
   *
   * @return unmodifiable map of counts keyed by subject
   */
  public Map<String, Integer> bySubject() {
    return bySubject;
  }

  /**
   * Weekday breakdown (weekday → count).
   *
   * @return unmodifiable map of counts keyed by weekday
   */
  public Map<DayOfWeek, Integer> byWeekday() {
    return byWeekday;
  }

  /**
   * ISO week breakdown with labels such as {@code 2025-W50}.
   *
   * @return unmodifiable map of counts keyed by ISO week label
   */
  public Map<String, Integer> byWeek() {
    return byWeek;
  }

  /**
   * Month breakdown.
   *
   * @return unmodifiable map of counts keyed by {@link YearMonth}
   */
  public Map<YearMonth, Integer> byMonth() {
    return byMonth;
  }

  /**
   * Average number of events per day in the range.
   *
   * @return average events per day
   */
  public double avgPerDay() {
    return avgPerDay;
  }

  /**
   * Backward-compatible alias for {@link #avgPerDay()} used by older tests.
   *
   * @return average events per day
   */
  public double averagePerDay() {
    return avgPerDay();
  }

  /**
   * All days tying for the highest count, sorted ascending.
   *
   * @return unmodifiable list of busiest days
   */
  public List<LocalDate> busiestDays() {
    return busiestDays;
  }

  /**
   * All days tying for the lowest count (zeros included), sorted ascending.
   *
   * @return unmodifiable list of least-busy days
   */
  public List<LocalDate> leastBusyDays() {
    return leastBusyDays;
  }

  /**
   * Number of events classified as online.
   *
   * @return online count
   */
  public int onlineCount() {
    return onlineCount;
  }

  /**
   * Number of events not classified as online.
   *
   * @return offline count
   */
  public int offlineCount() {
    return offlineCount;
  }

  /**
   * Percentage (0..100) of events that are classified online.
   *
   * @return online percentage
   */
  public double pctOnline() {
    return pctOnline;
  }

  /**
   * Percentage (0..100) of events that are not online.
   *
   * @return offline percentage
   */
  public double pctOffline() {
    return pctOffline;
  }

  /* ------------------------------ utilities ------------------------------- */

  private static boolean isOnline(final String location) {
    if (location == null) {
      return false;
    }
    final String s = location.trim().toLowerCase(Locale.ROOT);
    if (s.isEmpty()) {
      return false;
    }
    if ("online".equals(s)) {
      return true;
    }
    return s.startsWith("http://") || s.startsWith("https://")
        || s.contains("zoom") || s.contains("meet.google.com")
        || s.contains("teams.microsoft.com");
  }

  private static String safe(final String s) {
    return s == null ? "" : s.trim();
  }

  private static String joinDates(final List<LocalDate> dates) {
    final List<String> parts = new ArrayList<>();
    for (LocalDate d : dates) {
      parts.add(DAY_FMT.format(d));
    }
    return String.join(", ", parts);
  }

  private static <K extends Comparable<K>> Map<K, Integer> sortByKey(final Map<K, Integer> src) {
    final Set<K> sorted = new LinkedHashSet<>(src.keySet());
    final List<K> list = new ArrayList<>(sorted);
    Collections.sort(list);
    final Map<K, Integer> out = new LinkedHashMap<>();
    for (K k : list) {
      out.put(k, src.get(k));
    }
    return out;
  }
}
