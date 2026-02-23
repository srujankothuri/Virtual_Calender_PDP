package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles "create calendar" commands.
 */
public final class CreateCalendarCommand implements Command {

  private static final Pattern P = Pattern.compile(
      "^create\\s+calendar\\s+--name\\s+([^\\s]+)\\s+--timezone\\s+([^\\s]+)$",
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
    final String zoneId = m.group(2);
    mgr.createCalendar(name, ZoneId.of(zoneId));
    view.info("Created calendar '" + name + "' in zone " + zoneId);
    return true;
  }
}
