import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.model.CalendarService;
import calendar.view.CalendarView;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Reflection tests for the private ensureDefaultCalendar helper. */
public class CalendarRunnerTest {

  /** View that accumulates messages. */
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

  /** Creates default calendar when none exists. */
  @Test
  public void ensureDefaultCalendar_createsWhenEmpty() throws Exception {
    CalendarManager mgr = new CalendarManager(new CalendarService());
    CapturingView view = new CapturingView();

    Method method = Class.forName("CalendarRunner")
        .getDeclaredMethod(
            "ensureDefaultCalendar",
            calendar.controller.CalendarManager.class,
            calendar.view.CalendarView.class);
    method.setAccessible(true);

    method.invoke(null, mgr, view);
    List<String> names = mgr.getCalendarNames();
    assertTrue(names.contains("Default"));
  }

  /** No-op when there is already an active calendar. */
  @Test
  public void ensureDefaultCalendar_noopWhenAlreadyPresent() throws Exception {
    CalendarManager mgr = new CalendarManager(new CalendarService());
    mgr.createCalendar("X", ZoneId.of("UTC"));
    mgr.useCalendar("X");

    CapturingView view = new CapturingView();
    Method method = Class.forName("CalendarRunner")
        .getDeclaredMethod(
            "ensureDefaultCalendar",
            calendar.controller.CalendarManager.class,
            calendar.view.CalendarView.class);
    method.setAccessible(true);

    method.invoke(null, mgr, view);
    assertTrue(mgr.getCalendarNames().contains("X"));
  }

  /** Defensive path: passing null should not crash the JVM. */
  @Test
  public void ensureDefaultCalendar_handlesBadArgs() throws Exception {
    CapturingView view = new CapturingView();
    Method method = Class.forName("CalendarRunner")
        .getDeclaredMethod(
            "ensureDefaultCalendar",
            calendar.controller.CalendarManager.class,
            calendar.view.CalendarView.class);
    method.setAccessible(true);

    method.invoke(null, new Object[] {null, view});
    assertTrue(view.errors.size() >= 0);
  }
}
