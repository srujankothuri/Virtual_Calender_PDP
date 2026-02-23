package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles {@code export cal} commands, e.g. {@code export cal res/out.ical}.
 * Uses {@link CalendarManager#export(String)} which returns {@code null} on failure.
 */
public final class ExportCommand implements Command {

  private static final Pattern EXPORT = Pattern.compile(
      "^export\\s+cal\\s+(\\S+)$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public boolean tryRun(final String line,
                        final CalendarManager manager,
                        final CalendarView view) {
    final Matcher m = EXPORT.matcher(line.trim());
    if (!m.matches()) {
      return false;
    }

    final String filename = m.group(1);
    // CalendarManager.export returns a Path on success, or null on failure.
    final Path out = manager.export(filename);

    if (out != null) {
      view.info("Exported calendar to " + out.toAbsolutePath());
    } else {
      view.error("Failed to export calendar to " + filename);
    }
    return true;
  }
}
