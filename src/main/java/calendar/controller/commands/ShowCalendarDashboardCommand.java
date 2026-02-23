package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.model.CalendarAnalytics;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Command:
 *   show calendar dashboard from YYYY-MM-DD to YYYY-MM-DD
 * Delegates to the controller to compute analytics and prints the formatted dashboard.
 */
public final class ShowCalendarDashboardCommand implements Command {

  private static final String PREFIX = "show calendar dashboard from ";
  private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

  /** Public no-arg constructor required by the invoker. */
  public ShowCalendarDashboardCommand() {
    // no state
  }

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager manager,
                        final CalendarView out) {
    if (line == null) {
      return false;
    }
    final String s = line.trim().toLowerCase(Locale.ROOT);
    if (!s.startsWith(PREFIX)) {
      return false;
    }

    final int toIdx = s.indexOf(" to ", PREFIX.length());
    if (toIdx < 0) {
      out.error("Usage: show calendar dashboard from YYYY-MM-DD to YYYY-MM-DD");
      return true;
    }

    final String fromStr = line.substring(PREFIX.length(), toIdx).trim();
    final String toStr = line.substring(toIdx + 4).trim();

    try {
      final LocalDate from = LocalDate.parse(fromStr, DF);
      final LocalDate to = LocalDate.parse(toStr, DF);
      final CalendarAnalytics analytics = manager.analyze(from, to);
      out.info(analytics.formatText());
    } catch (Exception ex) {
      out.error("Invalid dates. Use YYYY-MM-DD.");
    }
    return true;
  }
}
