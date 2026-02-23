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

/** CLI export command wiring tests. */
public class ExportCommandTest {

  /** View that captures messages for assertions. */
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

  private ExportCommand cmd;
  private CalendarManager manager;
  private CapturingView view;

  /** Prepare a manager with one calendar. */
  @Before
  public void setUp() {
    cmd = new ExportCommand();
    manager = new CalendarManager();
    manager.createCalendar("X", ZoneId.of("UTC"));
    manager.useCalendar("X");
    view = new CapturingView();
  }

  /** Non-matching lines should return false and not log anything. */
  @Test
  public void nonMatchReturnsFalse() {
    boolean matched = cmd.tryRun("export wrong", manager, view);
    assertFalse(matched);
    assertTrue(view.infos.isEmpty());
    assertTrue(view.errors.isEmpty());
  }

  /** Success prints info; failure prints error. */
  @Test
  public void successPrintsInfo_andFailurePrintsError() {
    boolean matchedOk = cmd.tryRun("export cal build/out.ics", manager, view);
    assertTrue(matchedOk);
    assertFalse(view.infos.isEmpty());

    boolean matchedErr = cmd.tryRun("export cal /no_dir/exp_cmd.ics", manager, view);
    assertTrue(matchedErr);
    assertFalse(view.errors.isEmpty());
  }
}
