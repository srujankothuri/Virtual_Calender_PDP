package calendar.controller.commands;

import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for editing calendar properties via CLI.
 */
public final class EditCalendarCommandTest {

  /** Spy that only tracks if any info line was printed. */
  private static final class ViewSpy implements CalendarView {
    private boolean sawInfo;

    @Override
    public void info(final String line) {
      sawInfo = true;
    }

    @Override
    public void error(final String line) {
      // not used
    }

    boolean sawInfo() {
      return sawInfo;
    }
  }

  private CalendarManager manager;
  private CommandInvoker invoker;
  private ViewSpy view;

  /** Creates a calendar called "Main" for the edit tests. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    view = new ViewSpy();
    invoker = new CommandInvoker(manager, view);
    manager.createCalendar("Main", ZoneId.of("America/New_York"));
  }

  /** Renames the calendar and changes timezone; both should succeed. */
  @Test
  public void renameAndTimezone_printInfo() {
    final boolean ok1 = invoker.execute(
        "edit calendar --name Main --property name Primary");
    final boolean ok2 = invoker.execute(
        "edit calendar --name Primary --property timezone Europe/London");

    assertTrue(ok1 && ok2 && view.sawInfo());
  }
}
