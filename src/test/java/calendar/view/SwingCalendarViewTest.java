package calendar.view;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Swing view should not show dialogs on CI. In true headless environments, calling the
 * dialog methods should either throw {@link java.awt.HeadlessException}, or on some JDK/CI
 * images, fail earlier with a Swing initialization error (e.g., missing native fonts).
 *
 * <p>This test therefore accepts either outcome to remain robust across runtimes.</p>
 */
public final class SwingCalendarViewTest {

  /** Both info and error should not open dialogs under headless CI. */
  @Test
  public void showDialogsOrHeadless() {
    System.setProperty("java.awt.headless", "true");
    final SwingCalendarView v = new SwingCalendarView();

    expectNoUi(() -> v.info("hello"));
    expectNoUi(() -> v.error("oops"));
  }

  /**
   * Expects that the action does not successfully open a Swing dialog in headless CI.
   * Acceptable exceptions:
   * <ul>
   *   <li>{@code java.awt.HeadlessException}</li>
   *   <li>{@code UnsatisfiedLinkError} (e.g., libfreetype missing on CI)</li>
   *   <li>{@code NoClassDefFoundError} from Swing font manager init</li>
   * </ul>
   */
  private static void expectNoUi(final Runnable action) {
    try {
      action.run();
      fail("Expected HeadlessException or Swing init error under headless CI");
    } catch (java.awt.HeadlessException expected) {
      // ok
    } catch (UnsatisfiedLinkError | NoClassDefFoundError expected) {
      // ok: Swing failed to initialize before throwing HeadlessException
    }
  }
}
