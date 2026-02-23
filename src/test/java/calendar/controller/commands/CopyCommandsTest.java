package calendar.controller.commands;

import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Before;
import org.junit.Test;

/**
 * Minimal formatting-clean test for {@link CopyCommands}.
 * Verifies parser consumes expected syntaxes and emits a confirmation.
 */
public final class CopyCommandsTest {

  /** Simple spy that records info/error messages. */
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
  }

  private CalendarManager manager;
  private CommandInvoker invoker;
  private ViewSpy view;

  /** Seeds two calendars and an all-day event in the source before each test. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    view = new ViewSpy();
    invoker = new CommandInvoker(manager, view);

    final ZoneId ny = ZoneId.of("America/New_York");
    manager.createCalendar("A", ny);
    manager.createCalendar("B", ny);
    manager.useCalendar("A");

    manager.addAllDay("Day", LocalDate.of(2024, 3, 15));
  }

  /** Copies events on a date and expects an info confirmation line. */
  @Test
  public void copyEventsOnDate_emitsConfirmation() {
    final String line = "copy events on 2024-03-15 --target B to 2024-03-16";
    final boolean ok = invoker.execute(line);

    assertTrue(ok);
    assertTrue(view.infos().stream().anyMatch(s ->
        s.contains("Copied events on 2024-03-15")));
  }
}
