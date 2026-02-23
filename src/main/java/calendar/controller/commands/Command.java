package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;

/**
 * A single-line CLI command. Implementations are pure and interact only
 * through the provided {@link CalendarManager} and {@link CalendarView}.
 */
public interface Command {
  /**
   * Attempts to match and run this command on the given line.
   *
   * @param line    the user input line
   * @param manager the application/controller layer
   * @param view    output sink (no direct I/O in commands)
   * @return true if matched and executed; false otherwise
   */
  boolean tryRun(String line, CalendarManager manager, CalendarView view);
}
