
package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles "use calendar" command.
 */
public final class UseCalendarCommand implements Command {

  private static final Pattern P = Pattern.compile(
      "^use\\s+calendar\\s+--name\\s+([^\\s]+)$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public boolean tryRun(
      final String line,
      final CalendarManager mgr,
      final CalendarView view) {
    final Matcher m = P.matcher(line);
    if (!m.matches()) {
      return false;
    }
    final String name = m.group(1);
    mgr.useCalendar(name);
    view.info("Using calendar '" + name + "'");
    return true;
  }
}
