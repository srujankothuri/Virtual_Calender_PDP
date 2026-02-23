// src/main/java/calendar/view/CalendarView.java

package calendar.view;

/**
 * Abstraction for user-visible messages.
 */
public interface CalendarView {

  /**
   * Prints an informational message.
   *
   * @param message text to print
   */
  void info(String message);

  /**
   * Prints an error message.
   *
   * @param message text to print
   */
  void error(String message);
}
