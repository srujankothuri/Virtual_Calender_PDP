package calendar.controller.commands;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZoneId;
import org.junit.Test;

/**
 * Guard and error-propagation tests for {@link CommandInvokerControllerAdapter}.
 * We avoid subclassing {@link CommandInvoker} (it's final) by reflecting the
 * adapter's private {@code submit(String)} and feeding it an invalid command.
 */
public final class CommandInvokerControllerAdapterGuardsTest {

  /** Minimal no-op view to satisfy the invoker. */
  private static final class SilentView implements CalendarView {
    @Override
    public void info(final String line) {
      // no-op
    }

    @Override
    public void error(final String line) {
      // no-op
    }
  }

  /** setTimezone should throw when there is no active/current calendar. */
  @Test(expected = IllegalStateException.class)
  public void setTimezone_requiresCurrentCalendar() throws Exception {
    final CalendarManager manager = new CalendarManager();
    final CommandInvoker invoker = new CommandInvoker(manager, new SilentView());
    final CommandInvokerControllerAdapter adapter =
        new CommandInvokerControllerAdapter(invoker, manager);

    adapter.setTimezone(ZoneId.of("UTC"));
  }

  /**
   * If the CLI line doesn't match any command, the adapter's submit should
   * surface that as an IllegalArgumentException.
   */
  @Test
  public void failedCliExecution_bubblesAsIllegalArgumentException() throws Exception {
    final CalendarManager manager = new CalendarManager();
    final CommandInvoker invoker = new CommandInvoker(manager, new SilentView());
    final CommandInvokerControllerAdapter adapter =
        new CommandInvokerControllerAdapter(invoker, manager);

    // Call the private submit(String) with an invalid, non-matching command.
    final Method submit =
        CommandInvokerControllerAdapter.class.getDeclaredMethod("submit", String.class);
    submit.setAccessible(true);
    try {
      submit.invoke(adapter, "__not_a_valid_command__");
      fail("Expected IllegalArgumentException when CLI execution fails");
    } catch (InvocationTargetException ite) {
      final Throwable cause = ite.getCause();
      assertTrue(
          "Expected IllegalArgumentException when CLI execution fails",
          cause instanceof IllegalArgumentException);
    }
  }
}
