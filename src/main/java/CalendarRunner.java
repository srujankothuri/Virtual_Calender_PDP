import calendar.controller.CalendarManager;
import calendar.controller.GuiController;
import calendar.controller.commands.CommandInvoker;
import calendar.controller.commands.CommandInvokerControllerAdapter;
import calendar.gui.SwingCalendarApp;
import calendar.model.CalendarService;
import calendar.view.CalendarReadOnly;
import calendar.view.CalendarView;
import calendar.view.CalendarViewModel;
import calendar.view.ConsoleView;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.List;
import java.util.Scanner;

/**
 * Unified launcher for the Calendar application.
 *
 * <p>Supported modes:
 * <ul>
 *   <li>GUI (default): no args or <code>--mode gui</code></li>
 *   <li>Interactive CLI: <code>--mode interactive</code></li>
 *   <li>Headless script: <code>--mode headless &lt;commands.txt&gt;</code></li>
 * </ul>
 * Fallbacks:
 * <ul>
 *   <li>Single arg (a filename) is treated as a headless script file.</li>
 *   <li>Unknown mode prints usage and exits.</li>
 * </ul>
 */
public final class CalendarRunner {

  private CalendarRunner() {
    // no instances
  }

  /**
   * Application entry point.
   *
   * @param args command-line arguments
   * @throws IOException if reading a command file fails
   */
  public static void main(final String[] args) throws IOException {
    // Core wiring (shared across all modes)
    final CalendarService model = new CalendarService();
    final CalendarManager manager = new CalendarManager(model);
    final CalendarView view = new ConsoleView(System.out, System.err);
    final CommandInvoker invoker = new CommandInvoker(manager, view);

    // Explicit mode switch first
    if (args.length >= 2 && "--mode".equalsIgnoreCase(args[0])) {
      final String mode = args[1].toLowerCase();
      switch (mode) {
        case "gui":
          {
          launchGui(manager, model, view, invoker);
          return;
          }
        case "interactive":
          {
          try (InputStreamReader in = new InputStreamReader(System.in)) {
            runLoop(in, invoker, view);
          }
          return;
          }
        case "headless":
          {
          if (args.length < 3) {
            view.error("Headless mode requires a file: --mode headless <commands.txt>");
            printUsage(view);
            return;
          }
          try (FileReader fr = new FileReader(args[2])) {
            runLoop(fr, invoker, view);
          }
          return;
          }
        default:
          {
          view.error("Unknown mode: " + args[1]);
          printUsage(view);
          return;
          }
      }
    }

    // Fallbacks
    if (args.length == 0) {
      // Default to GUI
      launchGui(manager, model, view, invoker);
      return;
    }

    if (args.length == 1) {
      // Treat a single argument as a headless script file
      try (FileReader fr = new FileReader(args[0])) {
        runLoop(fr, invoker, view);
      }
      return;
    }

    // Anything else → usage
    view.error("Invalid arguments.");
    printUsage(view);
  }

  /** Starts the Swing GUI, creating a default calendar if none exists. */
  private static void launchGui(final CalendarManager manager,
                                final CalendarService model,
                                final CalendarView view,
                                final CommandInvoker invoker) {
    ensureDefaultCalendar(manager, view);

    final CalendarReadOnly vm = new CalendarViewModel(model);
    final GuiController ctl = new CommandInvokerControllerAdapter(invoker, manager);

    // Host window for SwingCalendarApp (JFrame is-a Frame)
    final javax.swing.JFrame host = new javax.swing.JFrame();
    new SwingCalendarApp(host, vm, view, ctl).show();
  }

  /**
   * Runs the CLI loop over any {@link Readable} (stdin or a file).
   * Prints a banner, supports 'menu' and 'exit'. Exits the JVM at the end.
   */
  private static void runLoop(final Readable in,
                              final CommandInvoker invoker,
                              final CalendarView view) {
    view.info("Calendar CLI (type 'menu' for help, 'exit' to quit)");
    boolean exitedNormally = false;

    try (Scanner sc = new Scanner(in)) {
      while (sc.hasNextLine()) {
        final String line = sc.nextLine();
        final String trimmed = line.trim();
        if (trimmed.isEmpty()) {
          continue;
        }
        if ("exit".equalsIgnoreCase(trimmed)) {
          view.info("Goodbye!");
          exitedNormally = true;
          break;
        }
        if ("menu".equalsIgnoreCase(trimmed) || "help".equalsIgnoreCase(trimmed)) {
          printMenu(view);
          continue;
        }
        invoker.execute(trimmed);
      }
    }

    if (!exitedNormally) {
      view.error("Input ended without an 'exit' command. Exiting.");
    }

    System.exit(0);
  }

  /** Small help menu for the CLI. Keep in sync with your commands. */
  private static void printMenu(final CalendarView view) {
    view.info(String.join(System.lineSeparator(),
        "Supported commands:",
        "  create calendar --name <calName> --timezone Area/Location",
        "  edit calendar --name <name> --property <name|timezone> <value>",
        "  use calendar --name <name>",
        "  create event \"Subject\" from <ISO-ZDT> to <ISO-ZDT>",
        "  create event \"Subject\" on <YYYY-MM-DD>",
        "  create recurring \"Subject\" every <N> <DAYS|WEEKS|MONTHS>,",
        "    starting <YYYY-MM-DD> count <K>",
        "  edit event <property> \"Subject\" from <ISO-ZDT> to <ISO-ZDT>",
        "    with <value>",
        "  edit events <property> \"Subject\" from <ISO-ZDT> with <value>",
        "  edit series <property> \"Subject\" from <ISO-ZDT> with <value>",
        "  copy event \"Subject\" on <ISO-ZDT> --target <cal> to <ISO-ZDT>",
        "  copy events on <YYYY-MM-DD> --target <cal> to <YYYY-MM-DD>",
        "  copy events between <YYYY-MM-DD> and <YYYY-MM-DD>,",
        "    --target <cal> to <YYYY-MM-DD>",
        "  print events on <YYYY-MM-DD>",
        "  print events from <ISO-ZDT> to <ISO-ZDT>",
        "  show status on <ISO-ZDT>",
        "  export cal <filename.csv|filename.ics|filename.ical>",
        "  exit"));
  }

  /** Usage banner for invalid args. */
  private static void printUsage(final CalendarView view) {
    view.info(String.join(System.lineSeparator(),
        "Usage:",
        "  (no args)                  # Launch GUI",
        "  --mode gui                 # Launch GUI",
        "  --mode interactive         # Start interactive CLI (stdin)",
        "  --mode headless <file>     # Run commands from file",
        "  <file>                     # Shortcut for headless mode"));
  }

  /** Create and select a default calendar (system timezone) if none exists. */
  private static void ensureDefaultCalendar(final CalendarManager manager,
                                            final CalendarView view) {
    try {
      final List<String> names = manager.getCalendarNames();
      if (names == null || names.isEmpty()) {
        final String defName = "Default";
        final ZoneId tz = ZoneId.systemDefault();
        manager.createCalendar(defName, tz);
        manager.useCalendar(defName);
      }
    } catch (Exception e) {
      view.error(e.getMessage());
    }
  }
}
