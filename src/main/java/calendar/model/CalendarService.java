package calendar.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * In-memory Calendar model implementation.
 * Enforces (subject,start,end) uniqueness and uses NavigableMap indices
 * for optimal range queries.
 *
 * <p><strong>Error policy:</strong> never throws for user mistakes. All validation
 * failures print to {@code System.err} and the method returns without changing state.
 * Queries return safe defaults.</p>
 */
public final class CalendarService implements CalendarModel {

  /**
   * Uniqueness key = (subject, start, end).
   * Java 11 compatible (no records).
   */
  private static final class EventKey {
    private final String subject;
    private final ZonedDateTime start;
    private final ZonedDateTime end;

    EventKey(final String subject, final ZonedDateTime start, final ZonedDateTime end) {
      this.subject = subject;
      this.start = start;
      this.end = end;
    }

    String subject() {
      return this.subject;
    }

    ZonedDateTime start() {
      return this.start;
    }

    ZonedDateTime end() {
      return this.end;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof EventKey)) {
        return false;
      }
      final EventKey other = (EventKey) o;
      return subject.equals(other.subject)
          && start.equals(other.start)
          && end.equals(other.end);
    }

    @Override
    public int hashCode() {
      return Objects.hash(subject, start, end);
    }
  }

  /** Deterministic ordering for keys. */
  private static final java.util.Comparator<EventKey> KEY_ORDER =
      java.util.Comparator.comparing(EventKey::start)
          .thenComparing(EventKey::end)
          .thenComparing(k -> k.subject().toLowerCase());

  /** Per-calendar storage bucket. */
  private static final class Cal {
    ZoneId zone;
    final Map<EventKey, IEvent> byId = new LinkedHashMap<>();
    final NavigableMap<ZonedDateTime, NavigableSet<EventKey>> byStart = new TreeMap<>();
    final NavigableMap<LocalDate, NavigableSet<EventKey>> byDay = new TreeMap<>();

    Cal(final ZoneId zone) {
      this.zone = zone;
    }
  }

  private final Map<String, Cal> calendars = new LinkedHashMap<>();
  private String current;

  // ---------- helpers (print-only error policy) ----------

  private static void err(final String msg) {
    System.err.println(msg);
  }

  private static boolean isBlank(final String s) {
    return s == null || s.trim().isEmpty();
  }

  private boolean hasActive() {
    return current != null && calendars.containsKey(current);
  }

  /**
   * Returns the active calendar or {@code null} and prints an error.
   */
  private Cal active() {
    if (!hasActive()) {
      err("No active calendar selected");
      return null;
    }
    return calendars.get(current);
  }

  private void indexAdd(final Cal cal, final IEvent e) {
    final EventKey key = new EventKey(e.subject(), e.start(), e.end());
    if (cal.byId.containsKey(key)) {
      err("Duplicate event (subject,start,end) already exists: " + e.subject());
      return;
    }
    cal.byId.put(key, e);
    cal.byStart.computeIfAbsent(e.start(), t -> new TreeSet<>(KEY_ORDER)).add(key);
    cal.byDay.computeIfAbsent(e.start().toLocalDate(), d -> new TreeSet<>(KEY_ORDER)).add(key);
  }

  private void indexRemove(final Cal cal, final IEvent e) {
    final EventKey key = new EventKey(e.subject(), e.start(), e.end());
    cal.byId.remove(key);

    final NavigableSet<EventKey> ks = cal.byStart.get(e.start());
    if (ks != null) {
      ks.remove(key);
      if (ks.isEmpty()) {
        cal.byStart.remove(e.start());
      }
    }

    final LocalDate day = e.start().toLocalDate();
    final NavigableSet<EventKey> ds = cal.byDay.get(day);
    if (ds != null) {
      ds.remove(key);
      if (ds.isEmpty()) {
        cal.byDay.remove(day);
      }
    }
  }

  private static List<IEvent> resolveKeys(final Cal cal, final Iterable<EventKey> keys) {
    final List<IEvent> out = new ArrayList<>();
    for (EventKey k : keys) {
      final IEvent e = cal.byId.get(k);
      if (e != null) {
        out.add(e);
      }
    }
    return out;
  }

  // ---------- CalendarModel methods ----------

  @Override
  public void createCalendar(final String name, final ZoneId zone) {
    if (isBlank(name)) {
      err("Calendar name must not be empty");
      return;
    }
    if (zone == null) {
      err("Timezone must not be null");
      return;
    }
    final String trimmed = name.trim();
    if (calendars.containsKey(trimmed)) {
      err("Calendar already exists: " + trimmed);
      return;
    }
    calendars.put(trimmed, new Cal(zone));
  }

  @Override
  public void renameCalendar(final String oldName, final String newName) {
    if (isBlank(oldName) || isBlank(newName)) {
      err("Calendar names must not be empty");
      return;
    }
    if (!calendars.containsKey(oldName)) {
      err("Unknown calendar: " + oldName);
      return;
    }
    final String trimmed = newName.trim();
    if (calendars.containsKey(trimmed)) {
      err("Calendar already exists: " + trimmed);
      return;
    }
    final Cal c = calendars.remove(oldName);
    calendars.put(trimmed, c);
    if (oldName.equals(current)) {
      current = trimmed;
    }
  }

  @Override
  public void setTimezone(final String name, final ZoneId zone) {
    if (!calendars.containsKey(name)) {
      err("Unknown calendar: " + name);
      return;
    }
    if (zone == null) {
      err("Timezone must not be null");
      return;
    }
    calendars.get(name).zone = zone;
  }

  @Override
  public void useCalendar(final String name) {
    if (!calendars.containsKey(name)) {
      err("Unknown calendar: " + name);
      return;
    }
    current = name;
  }

  @Override
  public String current() {
    return current;
  }

  @Override
  public ZoneId currentZone() {
    return hasActive() ? calendars.get(current).zone : ZoneId.systemDefault();
  }

  @Override
  public List<String> getCalendarNames() {
    return new ArrayList<>(calendars.keySet());
  }

  // ---------- creation ----------

  @Override
  public void createSingle(final String subject,
                           final ZonedDateTime start,
                           final ZonedDateTime end,
                           final String description,
                           final String location,
                           final String status) {
    final Cal cal = active();
    if (cal == null) {
      return;
    }
    if (isBlank(subject)) {
      err("subject must not be empty");
      return;
    }
    if (start == null || end == null) {
      err("start/end must not be null");
      return;
    }
    if (!start.isBefore(end)) {
      err("start must be strictly before end");
      return;
    }

    final Event e = new Event.Builder()
        .subject(subject)
        .start(start)
        .end(end)
        .description(description)
        .location(location)
        .status(status)
        .build();

    indexAdd(cal, e);
  }

  @Override
  public void createAllDay(final String subject,
                           final LocalDate day,
                           final String description,
                           final String location,
                           final String status) {
    if (day == null) {
      err("day must not be null");
      return;
    }
    final ZoneId z = currentZone();
    final ZonedDateTime start = day.atStartOfDay(z);
    final ZonedDateTime end = start.plusDays(1);
    createSingle(subject, start, end, description, location, status);
  }

  @Override
  public void createRecurringSeriesCount(final String subject,
                                         final ZonedDateTime start,
                                         final ZonedDateTime end,
                                         final EnumSet<DayOfWeek> days,
                                         final int count,
                                         final String description,
                                         final String location,
                                         final String status) {
    final Cal cal = active();
    if (cal == null) {
      return;
    }
    if (start == null || end == null) {
      err("start/end must not be null");
      return;
    }
    if (!start.isBefore(end)) {
      err("start must be strictly before end");
      return;
    }
    if (days == null || days.isEmpty()) {
      err("repeat days must not be empty");
      return;
    }
    if (count <= 0) {
      err("count must be > 0");
      return;
    }

    final Duration dur = Duration.between(start, end);
    final LocalTime st = start.toLocalTime();

    ZonedDateTime s = start;
    int created = 0;
    while (created < count) {
      if (days.contains(s.getDayOfWeek())) {
        // Fix time-of-day explicitly each occurrence; supports DST and cross-day durations
        final ZonedDateTime occStart = s.with(st);
        final ZonedDateTime occEnd = occStart.plus(dur);

        final Event e = new Event.Builder()
            .subject(subject)
            .start(occStart)
            .end(occEnd)
            .description(description)
            .location(location)
            .status(status)
            .build();
        indexAdd(cal, e);
        created += 1;
      }
      s = s.plusDays(1);
    }
  }

  @Override
  public void createRecurringSeriesUntil(final String subject,
                                         final ZonedDateTime start,
                                         final ZonedDateTime end,
                                         final EnumSet<DayOfWeek> days,
                                         final LocalDate untilDate,
                                         final String description,
                                         final String location,
                                         final String status) {
    final Cal cal = active();
    if (cal == null) {
      return;
    }
    if (start == null || end == null || untilDate == null) {
      err("start/end/until must not be null");
      return;
    }
    if (!start.isBefore(end)) {
      err("start must be strictly before end");
      return;
    }
    if (days == null || days.isEmpty()) {
      err("repeat days must not be empty");
      return;
    }

    final Duration dur = Duration.between(start, end);
    final LocalTime st = start.toLocalTime();

    ZonedDateTime s = start;
    while (!s.toLocalDate().isAfter(untilDate)) {
      if (days.contains(s.getDayOfWeek())) {
        final ZonedDateTime occStart = s.with(st);
        final ZonedDateTime occEnd = occStart.plus(dur);

        final Event e = new Event.Builder()
            .subject(subject)
            .start(occStart)
            .end(occEnd)
            .description(description)
            .location(location)
            .status(status)
            .build();
        indexAdd(cal, e);
      }
      s = s.plusDays(1);
    }
  }


  @Override
  public void createRecurringAllDaySeriesCount(final String subject,
                                               final LocalDate startDate,
                                               final EnumSet<DayOfWeek> days,
                                               final int count,
                                               final String description,
                                               final String location,
                                               final String status) {
    if (startDate == null) {
      err("start date must not be null");
      return;
    }
    if (days == null || days.isEmpty()) {
      err("repeat days must not be empty");
      return;
    }
    if (count <= 0) {
      err("count must be > 0");
      return;
    }

    LocalDate d = startDate;
    int created = 0;
    while (created < count) {
      if (days.contains(d.getDayOfWeek())) {
        createAllDay(subject, d, description, location, status);
        created += 1;
      }
      d = d.plusDays(1);
    }
  }

  @Override
  public void createRecurringAllDaySeriesUntil(final String subject,
                                               final LocalDate startDate,
                                               final EnumSet<DayOfWeek> days,
                                               final LocalDate untilDate,
                                               final String description,
                                               final String location,
                                               final String status) {
    if (startDate == null || untilDate == null) {
      err("start/until must not be null");
      return;
    }
    if (days == null || days.isEmpty()) {
      err("repeat days must not be empty");
      return;
    }
    LocalDate d = startDate;
    while (!d.isAfter(untilDate)) {
      if (days.contains(d.getDayOfWeek())) {
        createAllDay(subject, d, description, location, status);
      }
      d = d.plusDays(1);
    }
  }

  // ---------- queries ----------

  @Override
  public List<IEvent> eventsBetween(final LocalDate from, final LocalDate to) {
    final ZoneId z = currentZone();
    final ZonedDateTime start = from.atStartOfDay(z);
    final ZonedDateTime end = to.plusDays(1).atStartOfDay(z).minusNanos(1);
    return eventsBetween(start, end);
  }

  @Override
  public List<IEvent> eventsBetween(final ZonedDateTime from,
                                    final ZonedDateTime to) {
    if (!hasActive()) {
      return new ArrayList<>();
    }
    final Cal cal = calendars.get(current);
    final List<IEvent> out = new ArrayList<>();

    // include event whose start <= from but overlaps into the window
    final ZonedDateTime floor = cal.byStart.floorKey(from);
    if (floor != null) {
      for (EventKey k : new ArrayList<>(cal.byStart.get(floor))) {
        final IEvent e = cal.byId.get(k);
        if (e != null && !e.end().isBefore(from)) {
          out.add(e);
        }
      }
    }

    // add everything that starts in [from,to]
    for (Map.Entry<ZonedDateTime, NavigableSet<EventKey>> en
        : cal.byStart.subMap(from, true, to, true).entrySet()) {
      out.addAll(resolveKeys(cal, en.getValue()));
    }
    return Collections.unmodifiableList(out);
  }


  @Override
  public CalendarAnalytics computeAnalytics(final LocalDate from, final LocalDate to) {
    if (from == null || to == null) {
      err("from/to must not be null");
      // Safe empty result; assumes CalendarAnalytics has a zero-able compute.
      // Falling back to an empty list is fine.
    }
    // Reuse existing query that already respects the active calendar + zone.
    final List<IEvent> window = eventsBetween(from, to);
    // Delegate the math to the analytics class that lives in the model layer.
    return CalendarAnalytics.compute(window, from, to);
  }



  @Override
  public List<IEvent> eventsOn(final LocalDate day) {
    if (!hasActive()) {
      return Collections.emptyList();
    }
    final Cal cal = calendars.get(current);
    final NavigableSet<EventKey> ks = cal.byDay.get(day);
    if (ks == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(resolveKeys(cal, ks));
  }

  @Override
  public List<IEvent> find(final String subject,
                           final ZonedDateTime from,
                           final ZonedDateTime to) {
    final String needle = (subject == null) ? "" : subject.toLowerCase();
    final List<IEvent> base = eventsBetween(from, to);
    final List<IEvent> out = new ArrayList<>();
    for (IEvent e : base) {
      if (e.subject().toLowerCase().contains(needle)) {
        out.add(e);
      }
    }
    return out;
  }

  @Override
  public Optional<IEvent> findBySubjectAt(final String subject,
                                          final ZonedDateTime start) {
    if (!hasActive()) {
      return Optional.empty();
    }
    final Cal cal = calendars.get(current);
    final NavigableSet<EventKey> ks = cal.byStart.get(start);
    if (ks == null) {
      return Optional.empty();
    }
    for (EventKey k : ks) {
      if (k.subject().equals(subject)) {
        return Optional.ofNullable(cal.byId.get(k));
      }
    }
    return Optional.empty();
  }

  @Override
  public List<IEvent> findSeriesFrom(final String subject,
                                     final ZonedDateTime fromTime) {
    if (!hasActive()) {
      return new ArrayList<>();
    }
    final Cal cal = calendars.get(current);
    final LocalTime st = fromTime.toLocalTime();
    final List<IEvent> out = new ArrayList<>();
    for (Map.Entry<ZonedDateTime, NavigableSet<EventKey>> en
        : cal.byStart.tailMap(fromTime, true).entrySet()) {
      for (EventKey k : en.getValue()) {
        if (k.subject().equals(subject) && k.start().toLocalTime().equals(st)) {
          out.add(cal.byId.get(k));
        }
      }
    }
    return out;
  }

  @Override
  public List<IEvent> findEntireSeries(final String subject,
                                       final ZonedDateTime anchorStart) {
    if (!hasActive()) {
      return new ArrayList<>();
    }
    final Cal cal = calendars.get(current);
    final LocalTime st = anchorStart.toLocalTime();
    final List<IEvent> out = new ArrayList<>();
    for (Map.Entry<ZonedDateTime, NavigableSet<EventKey>> en : cal.byStart.entrySet()) {
      for (EventKey k : en.getValue()) {
        if (k.subject().equals(subject) && k.start().toLocalTime().equals(st)) {
          out.add(cal.byId.get(k));
        }
      }
    }
    return out;
  }

  @Override
  public boolean isBusyAt(final ZonedDateTime time) {
    if (!hasActive()) {
      return false;
    }
    final Cal cal = calendars.get(current);
    final ZonedDateTime floor = cal.byStart.floorKey(time);
    if (floor != null) {
      for (EventKey k : cal.byStart.get(floor)) {
        final IEvent e = cal.byId.get(k);
        if (e != null && !e.end().isBefore(time)) {
          return true;
        }
      }
    }
    return false;
  }

  // ---------- modifications ----------

  @Override
  public void deleteBySubjectAt(final String subject, final ZonedDateTime start) {
    final Optional<IEvent> found = findBySubjectAt(subject, start);
    if (!found.isPresent()) {
      err("No matching event to delete");
      return;
    }
    final Cal cal = active();
    if (cal == null) {
      return;
    }
    indexRemove(cal, found.get());
  }

  /**
   * Edit by removing the base event from all indices and inserting
   * the updated copy (after uniqueness check). Prints errors; no throws.
   */
  public void recreate(final IEvent base,
                       final String newSubject,
                       final ZonedDateTime newStart,
                       final ZonedDateTime newEnd,
                       final String newDesc,
                       final String newLoc,
                       final String newStatus) {
    final Cal cal = active();
    if (cal == null) {
      return;
    }

    final String s = (newSubject == null) ? base.subject() : newSubject;
    final ZonedDateTime st = (newStart == null) ? base.start() : newStart;
    final ZonedDateTime en = (newEnd == null) ? base.end() : newEnd;
    final String d = (newDesc == null) ? base.description() : newDesc;
    final String l = (newLoc == null) ? base.location() : newLoc;
    final String t = (newStatus == null) ? base.status() : newStatus;

    if (isBlank(s)) {
      err("subject must not be empty");
      return;
    }
    if (st == null || en == null) {
      err("start/end must not be null");
      return;
    }
    if (!st.isBefore(en)) {
      err("start must be strictly before end");
      return;
    }

    final Event candidate = new Event.Builder()
        .subject(s)
        .start(st)
        .end(en)
        .description(d)
        .location(l)
        .status(t)
        .build();

    final EventKey newKey = new EventKey(candidate.subject(), candidate.start(), candidate.end());
    if (cal.byId.containsKey(newKey)) {
      err("Duplicate event after edit; no changes applied.");
      return;
    }

    indexRemove(cal, base);
    indexAdd(cal, candidate);
  }
}
