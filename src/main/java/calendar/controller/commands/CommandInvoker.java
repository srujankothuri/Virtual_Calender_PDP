package calendar.controller.commands;

import calendar.controller.CalendarManager;
import calendar.view.CalendarView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Command Invoker that manages and executes commands.
 * Follows the Command Pattern to decouple command execution from command implementation.
 */
public final class CommandInvoker {

  private final List<Command> commands;
  private final CalendarManager manager;
  private final CalendarView view;

  /**
   * Create a new command invoker.
   *
   * @param manager the calendar manager
   * @param view    the calendar view
   */
  public CommandInvoker(final CalendarManager manager, final CalendarView view) {
    this.manager = Objects.requireNonNull(manager);
    this.view = Objects.requireNonNull(view);
    this.commands = new ArrayList<>();
    registerCommands();
  }

  /**
   * Register all available commands.
   * This follows Open-Closed Principle - we can add new commands without modifying existing ones.
   */
  private void registerCommands() {
    // Calendar management commands
    commands.add(new CreateCalendarCommand());
    commands.add(new EditCalendarCommand());
    commands.add(new UseCalendarCommand());

    // Event creation commands
    commands.add(new CreateEventCommand());

    // Event editing commands - all in one class
    commands.add(new EditEventCommands());

    // Copy commands - all in one class
    commands.add(new CopyCommands());

    // Query commands - includes print, show status, and exit
    commands.add(new QueryCommands());

    // Export command
    commands.add(new ExportCommand());


    // Analytics / dashboard (uses CalendarManager facade internally)
    commands.add(new ShowCalendarDashboardCommand());
  }

  /**
   * Execute a command line.
   *
   * @param line the command line to execute
   * @return true if a command was executed, false if no command matched
   */
  public boolean execute(final String line) {
    if (line == null || line.trim().isEmpty()) {
      view.error("Empty command");
      return false;
    }

    final String trimmedLine = line.trim();

    // Try each registered command
    for (Command command : commands) {
      if (command.tryRun(trimmedLine, manager, view)) {
        return true;
      }
    }

    // No command matched
    view.error("Unknown command: " + trimmedLine);
    return false;
  }

  /**
   * Add a new command to the invoker.
   * This allows for runtime extension of commands.
   *
   * @param command the command to add
   */
  public void addCommand(final Command command) {
    commands.add(Objects.requireNonNull(command));
  }

  /**
   * Remove a command from the invoker.
   *
   * @param command the command to remove
   * @return true if the command was removed
   */
  public boolean removeCommand(final Command command) {
    return commands.remove(command);
  }
}
