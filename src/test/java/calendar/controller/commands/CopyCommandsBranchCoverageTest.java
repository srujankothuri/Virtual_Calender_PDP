package calendar.controller.commands;

import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Test;

/** Branch coverage for CopyCommands: ONE (offset/no-offset), DAY, BETWEEN. */
public final class CopyCommandsBranchCoverageTest {

  /** Minimal spy view that records info messages. */
  private static final class ViewSpy implements CalendarView {
    final List<String> info = new CopyOnWriteArrayList<>();

    @Override
    public void info(final String s) {
      info.add(s);
    }

    @Override
    public void error(final String s) {
      // not used in these tests
    }
  }

  /**.
   * Build a manager with A (active) and B calendars in New York; seed:
   *  - all-day event on 2024-03-15 for DAY/BETWEEN tests
   *  - "Standup" at 2024-03-10T09:00 America/New_York for ONE (no offset)
   *  - "Foo" at 2024-03-10T09:00Z for ONE (explicit offset)
   */
  private static CalendarManager newManagerNySeeded() {
    CalendarManager manager = new CalendarManager();
    ZoneId ny = ZoneId.of("America/New_York");

    manager.createCalendar("A", ny);
    manager.createCalendar("B", ny);
    manager.useCalendar("A");

    // all-day for DAY/BETWEEN
    manager.addAllDay("Day", LocalDate.of(2024, 3, 15));

    // source instance for ONE without offset (uses current zone)
    ZonedDateTime s1 = ZonedDateTime.of(2024, 3, 10, 9, 0, 0, 0, ny);
    ZonedDateTime e1 = s1.plusHours(1);
    manager.addSingle("Standup", s1, e1, "", "", "PUBLIC");

    // source instance for ONE with explicit offsets (UTC time)
    ZonedDateTime s2 = ZonedDateTime.parse("2024-03-10T09:00Z");
    ZonedDateTime e2 = s2.plusHours(1);
    manager.addSingle("Foo", s2, e2, "", "", "PUBLIC");

    return manager;
  }

  @Test
  public void oneWithoutOffset_usesCurrentZone_andPrintsInfo() {
    ViewSpy view = new ViewSpy();
    CalendarManager manager = newManagerNySeeded();

    boolean ok = new CommandInvoker(manager, view).execute(
        "copy event \"Standup\" on 2024-03-10T09:00 "
            + "--target B to 2024-03-11T10:00");

    assertTrue(ok);
    assertTrue(view.info.stream().anyMatch(s -> s.contains("Copied event")));
  }

  @Test
  public void oneWithExplicitOffsets_parsesOffsets_andPrintsInfo() {
    ViewSpy view = new ViewSpy();
    CalendarManager manager = newManagerNySeeded();

    boolean ok = new CommandInvoker(manager, view).execute(
        "copy event Foo on 2024-03-10T09:00Z "
            + "--target B to 2024-03-10T10:00+02:00");

    assertTrue(ok);
    assertTrue(view.info.stream().anyMatch(s -> s.contains("Copied event")));
  }

  @Test
  public void dayCopy_emitsConfirmation() {
    ViewSpy view = new ViewSpy();
    CalendarManager manager = newManagerNySeeded();

    boolean ok = new CommandInvoker(manager, view).execute(
        "copy events on 2024-03-15 --target B to 2024-03-16");

    assertTrue(ok);
    assertTrue(view.info.stream()
        .anyMatch(s -> s.contains("Copied events on 2024-03-15")));
  }

  @Test
  public void betweenCopy_emitsConfirmation() {
    ViewSpy view = new ViewSpy();
    CalendarManager manager = newManagerNySeeded();

    boolean ok = new CommandInvoker(manager, view).execute(
        "copy events between 2024-03-10 and 2024-03-16 "
            + "--target B to 2024-03-20");

    assertTrue(ok);
    assertTrue(view.info.stream()
        .anyMatch(s -> s.contains("Copied events between 2024-03-10 and 2024-03-16")));
  }
}
