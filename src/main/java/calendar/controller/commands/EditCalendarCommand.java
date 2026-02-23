package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Edits calendar properties from the CLI.
 *
 * <p>Supports:
 *  - {@code edit calendar --name NAME --property name NEW_NAME}
 *  - {@code edit calendar --name NAME --property timezone AREA/LOCATION}
 */
public final class EditCalendarCommand implements Command {

  private static final Pattern P = Pattern.compile(
      "^edit\\s+calendar\\s+--name\\s+(\\S+)\\s+--property\\s+"
          + "(name|timezone)\\s+(.+)$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager manager,
                        final CalendarView view) {
    Matcher m = P.matcher(line);
    if (!m.matches()) {
      return false;
    }

    String name = m.group(1).trim();
    String prop = m.group(2).toLowerCase().trim();
    String value = m.group(3).trim();

    switch (prop) {
      case "name":
        manager.renameCalendar(name, value);
        view.info("Renamed calendar '" + name + "' to '" + value + "'");
        return true;
      case "timezone":
        manager.setTimezone(name, ZoneId.of(value));
        view.info("Set timezone of '" + name + "' to " + value);
        return true;
      default:
        return false;
    }
  }
}
