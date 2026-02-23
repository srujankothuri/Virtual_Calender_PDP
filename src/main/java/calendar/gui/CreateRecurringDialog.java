package calendar.gui;

import calendar.view.CalendarReadOnly;
import calendar.view.CalendarView;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * Dialog to create recurring events (all-day or timed), either by count or until a date.
 *
 * <p>Validations mirror {@code CreateEventDialog} and also check:
 * <ul>
 *   <li>At least one weekday checked.</li>
 *   <li>Exactly one of count or until is provided (UI ensures this via radio buttons).</li>
 * </ul>
 * </p>
 */
public final class CreateRecurringDialog extends JDialog {

  /**
   * Callback for recurring creation.
   */
  public interface Callback {
    /**
     * Create by count.
     *
     * @param subject subject.
     * @param allDay  all-day if true.
     * @param day     anchor day.
     * @param start   local start (ignored if all-day).
     * @param end     local end (ignored if all-day).
     * @param days    weekdays mask.
     * @param count   number of occurrences.
     * @throws Exception on failure.
     */
    void createRecurringByCount(String subject, boolean allDay, LocalDate day,
                                LocalTime start, LocalTime end,
                                EnumSet<DayOfWeek> days, int count) throws Exception;

    /**
     * Create by until date.
     *
     * @param subject subject.
     * @param allDay  all-day if true.
     * @param day     anchor day.
     * @param start   local start (ignored if all-day).
     * @param end     local end (ignored if all-day).
     * @param days    weekdays mask.
     * @param until   last day in the rule.
     * @throws Exception on failure.
     */
    void createRecurringByUntil(String subject, boolean allDay, LocalDate day,
                                LocalTime start, LocalTime end,
                                EnumSet<DayOfWeek> days, LocalDate until) throws Exception;
  }

  private final CalendarReadOnly vm;
  private final CalendarView messages;
  private final Callback cb;

  private final JTextField subjectField;
  private final JTextField dayField;
  private final JCheckBox allDayBox;
  private final JTextField startField;
  private final JTextField endField;

  // Weekday selectors (renamed to satisfy Checkstyle member-name pattern).
  private final JCheckBox monBox;
  private final JCheckBox tueBox;
  private final JCheckBox wedBox;
  private final JCheckBox thuBox;
  private final JCheckBox friBox;
  private final JCheckBox satBox;
  private final JCheckBox sunBox;

  private final JRadioButton byCount;
  private final JRadioButton byUntil;
  private final JTextField countField;
  private final JTextField untilField;

  /**
   * Construct the dialog.
   *
   * @param owner      host frame.
   * @param vm         read-only model.
   * @param messages   message sink.
   * @param defaultDay default day.
   * @param cb         callback.
   */
  public CreateRecurringDialog(final Frame owner,
                               final CalendarReadOnly vm,
                               final CalendarView messages,
                               final LocalDate defaultDay,
                               final Callback cb) {
    super(owner, "Create Recurring", true);
    this.vm = vm;
    this.messages = messages;
    this.cb = cb;

    subjectField = new JTextField(18);
    dayField = new JTextField(10);
    allDayBox = new JCheckBox("All-day");
    startField = new JTextField(6);
    endField = new JTextField(6);

    dayField.setText(defaultDay.toString());
    startField.setText("09:00");
    endField.setText("10:00");

    monBox = new JCheckBox("M");
    tueBox = new JCheckBox("T");
    wedBox = new JCheckBox("W");
    thuBox = new JCheckBox("R");
    friBox = new JCheckBox("F");
    satBox = new JCheckBox("S");
    sunBox = new JCheckBox("U");

    byCount = new JRadioButton("Repeat for N times");
    byUntil = new JRadioButton("Repeat until date");
    final ButtonGroup group = new ButtonGroup();
    group.add(byCount);
    group.add(byUntil);
    byCount.setSelected(true);

    countField = new JTextField("5", 5);
    untilField = new JTextField(10);

    final JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));

    form.add(new JLabel("Subject:"));
    form.add(subjectField);

    form.add(new JLabel("Anchor day (yyyy-MM-dd):"));
    form.add(dayField);

    form.add(new JLabel("All-day?"));
    form.add(allDayBox);

    form.add(new JLabel("Start (HH:mm):"));
    form.add(startField);

    form.add(new JLabel("End (HH:mm):"));
    form.add(endField);

    form.add(new JLabel("Days:"));
    final JPanel daysPanel = new JPanel();
    daysPanel.add(monBox);
    daysPanel.add(tueBox);
    daysPanel.add(wedBox);
    daysPanel.add(thuBox);
    daysPanel.add(friBox);
    daysPanel.add(satBox);
    daysPanel.add(sunBox);
    form.add(daysPanel);

    form.add(byCount);
    form.add(new JLabel("Count:"));
    form.add(countField);

    form.add(byUntil);
    form.add(new JLabel("Until (yyyy-MM-dd):"));
    form.add(untilField);

    final JPanel buttons = new JPanel();
    final JButton ok = new JButton("Create");
    final JButton cancel = new JButton("Cancel");
    ok.addActionListener(e -> onCreate());
    cancel.addActionListener(e -> dispose());
    buttons.add(ok);
    buttons.add(cancel);

    getContentPane().setLayout(new BorderLayout(8, 8));
    getContentPane().add(form, BorderLayout.CENTER);
    getContentPane().add(buttons, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(owner);
  }

  private void onCreate() {
    final String subject = trimmed(subjectField.getText());
    if (subject.isEmpty()) {
      messages.error("Please enter a subject.");
      return;
    }

    final LocalDate day = parseDay(dayField.getText());
    if (day == null) {
      messages.error("Please enter a valid anchor date (yyyy-MM-dd).");
      return;
    }

    if (hasSubjectOn(vm, day, subject)) {
      messages.error("An event named '" + subject + "' already exists on "
          + day + ". Use a different subject or edit the existing one.");
      return;
    }

    final EnumSet<DayOfWeek> mask = toMask();
    if (mask.isEmpty()) {
      messages.error("Please choose at least one weekday.");
      return;
    }

    final boolean allDay = allDayBox.isSelected();
    LocalTime start = null;
    LocalTime end = null;

    if (!allDay) {
      start = parseTime(startField.getText());
      end = parseTime(endField.getText());
      if (start == null || end == null) {
        messages.error("Please enter times as HH:mm (e.g., 09:00).");
        return;
      }
      if (!end.isAfter(start)) {
        messages.error("End time must be after start time.");
        return;
      }
    }

    try {
      if (byCount.isSelected()) {
        final Integer n = parseCount(countField.getText());
        if (n == null || n.intValue() <= 0) {
          messages.error("Please enter a positive repeat count.");
          return;
        }
        cb.createRecurringByCount(subject, allDay, day, start, end, mask, n.intValue());
      } else {
        final LocalDate until = parseDay(untilField.getText());
        if (until == null) {
          messages.error("Please enter a valid until date (yyyy-MM-dd).");
          return;
        }
        cb.createRecurringByUntil(subject, allDay, day, start, end, mask, until);
      }
      dispose();
    } catch (Exception ex) {
      messages.error(friendly(ex));
    }
  }

  private EnumSet<DayOfWeek> toMask() {
    final EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    if (monBox.isSelected()) {
      days.add(DayOfWeek.MONDAY);
    }
    if (tueBox.isSelected()) {
      days.add(DayOfWeek.TUESDAY);
    }
    if (wedBox.isSelected()) {
      days.add(DayOfWeek.WEDNESDAY);
    }
    if (thuBox.isSelected()) {
      days.add(DayOfWeek.THURSDAY);
    }
    if (friBox.isSelected()) {
      days.add(DayOfWeek.FRIDAY);
    }
    if (satBox.isSelected()) {
      days.add(DayOfWeek.SATURDAY);
    }
    if (sunBox.isSelected()) {
      days.add(DayOfWeek.SUNDAY);
    }
    return days;
  }

  // ---------- local helpers (no external util dependency) ----------

  private static String trimmed(final String s) {
    return s == null ? "" : s.trim();
  }

  private static LocalDate parseDay(final String raw) {
    try {
      return LocalDate.parse(trimmed(raw));
    } catch (Exception ex) {
      return null;
    }
  }

  private static LocalTime parseTime(final String raw) {
    try {
      return LocalTime.parse(trimmed(raw));
    } catch (Exception ex) {
      return null;
    }
  }

  private static Integer parseCount(final String raw) {
    try {
      return Integer.valueOf(trimmed(raw));
    } catch (Exception ex) {
      return null;
    }
  }

  private static boolean hasSubjectOn(final CalendarReadOnly vm,
                                      final LocalDate day,
                                      final String subject) {
    for (calendar.model.IEvent e : vm.eventsOn(day)) {
      if (subject.equals(e.subject())) {
        return true;
      }
    }
    return false;
  }

  private static String friendly(final Exception ex) {
    final String msg = ex == null ? null : ex.getMessage();
    if (msg == null || msg.trim().isEmpty()) {
      return "Sorry, that did not work. Please check your inputs.";
    }
    final String low = msg.toLowerCase();
    if (low.contains("duplicate")) {
      return "That would create a duplicate event. Try a different subject or time.";
    }
    return msg;
  }
}
