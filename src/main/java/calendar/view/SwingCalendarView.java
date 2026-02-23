package calendar.view;

import javax.swing.JOptionPane;

/**
 * Adapter that routes CalendarView messages to Swing dialogs.
 * Keeps ConsoleView for CLI modes, but in GUI we can use this.
 */
public final class SwingCalendarView implements CalendarView {

  @Override
  public void info(String message) {
    JOptionPane.showMessageDialog(null, message, "Info", JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  public void error(String message) {
    JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
  }
}
