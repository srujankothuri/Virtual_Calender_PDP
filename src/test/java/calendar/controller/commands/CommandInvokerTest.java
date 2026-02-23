package calendar.controller.commands;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** CommandInvoker should route commands and support custom additions. */
public class CommandInvokerTest {

  /** Simple view that records info/errors. */
  private static final class CapturingView implements CalendarView {
    private final List<String> infos = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    @Override
    public void info(String message) {
      infos.add(message);
    }

    @Override
    public void error(String message) {
      errors.add(message);
    }
  }

  private CommandInvoker invoker;
  private CalendarManager manager;
  private CapturingView view;

  /** Sets up a calendar and an invoker. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    manager.createCalendar("X", ZoneId.of("UTC"));
    manager.useCalendar("X");
    view = new CapturingView();
    invoker = new CommandInvoker(manager, view);
  }

  /** Empty input should be rejected. */
  @Test
  public void emptyInputEmitsError() {
    boolean ok = invoker.execute("");
    assertFalse(ok);
    assertFalse(view.errors.isEmpty());
  }

  /** Happy path for export command through invoker. */
  @Test
  public void exportCommandThroughInvoker() {
    boolean ok = invoker.execute("export cal build/invoker.ics");
    assertTrue(ok);
    assertFalse(view.infos.isEmpty());
  }

  /** Custom command can be added and later removed. */
  @Test
  public void addAndRemoveCustomCommand() {
    Command custom = new Command() {
      @Override
      public boolean tryRun(
          String line,
          calendar.controller.CalendarManager m,
          calendar.view.CalendarView v) {
        if ("ping".equals(line)) {
          v.info("pong");
          return true;
        }
        return false;
      }
    };
    invoker.addCommand(custom);
    assertTrue(invoker.execute("ping"));
    assertTrue(invoker.removeCommand(custom));
  }
}
