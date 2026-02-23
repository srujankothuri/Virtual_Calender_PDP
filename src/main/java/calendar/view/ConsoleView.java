package calendar.view;

import java.io.PrintStream;

/**
 * Console implementation of {@link CalendarView}.
 */
public final class ConsoleView implements CalendarView {

  private final PrintStream out;
  private final PrintStream err;

  /**
   * Creates a console view that writes to the given streams.
   *
   * @param outStream standard output
   * @param errStream standard error
   */
  public ConsoleView(final PrintStream outStream, final PrintStream errStream) {
    this.out = outStream;
    this.err = errStream;
  }

  @Override
  public void info(final String message) {
    this.out.println(message);
  }

  @Override
  public void error(final String message) {
    this.err.println(message);
  }
}
