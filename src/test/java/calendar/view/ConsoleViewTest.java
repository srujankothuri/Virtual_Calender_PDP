package calendar.view;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;

/** ConsoleView should print to the streams provided. */
public class ConsoleViewTest {

  /** Verifies info and error outputs go to the right streams. */
  @Test
  public void printsToProvidedStreams() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    ConsoleView v = new ConsoleView(new PrintStream(out), new PrintStream(err));

    v.info("hello");
    v.error("oops");

    assertTrue(out.toString().contains("hello"));
    assertTrue(err.toString().contains("oops"));
  }
}
