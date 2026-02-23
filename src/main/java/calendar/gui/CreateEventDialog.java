package calendar.gui;

import calendar.view.CalendarReadOnly;
import calendar.view.CalendarView;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog to create a single all-day or timed event.
 *
 * <p>The dialog gathers subject, optional location, anchor date and (for timed
 * events) start/end times in {@code HH:mm}. It reports the user's choice back
 * to the caller through {@link Callback} and does not mutate the model
 * directly.</p>
 */
public final class  CreateEventDialog extends JDialog {

  private final CalendarReadOnly vm;
  private final CalendarView messages;
  private final Callback cb;

  private final JTextField subjectField = new JTextField(18);
  private final JTextField locationField = new JTextField(18);
  private final JTextField dayField = new JTextField(10);
  private final JCheckBox allDay = new JCheckBox("All day");
  private final JTextField startField = new JTextField(6);
  private final JTextField endField = new JTextField(6);

  /**
   * Contract used by the view to consume the user's create action.
   */
  public interface Callback {
    /**
     * Create an all-day event.
     *
     * @param subject  subject text.
     * @param day      local date of the event.
     * @param location optional location string (may be empty).
     * @throws Exception if the create operation fails.
     */
    void createAllDay(String subject, LocalDate day, String location) throws Exception;

    /**
     * Create a timed event.
     *
     * @param subject  subject text.
     * @param start    start time with zone.
     * @param end      end time with zone.
     * @param location optional location string (may be empty).
     * @throws Exception if the create operation fails.
     */
    void createTimed(String subject, ZonedDateTime start,
                     ZonedDateTime end, String location) throws Exception;
  }

  /**
   * Construct the dialog.
   *
   * @param owner       parent frame for modality.
   * @param vm          read-only model for current zone.
   * @param messages    message sink for friendly errors.
   * @param defaultDay  pre-selected date for convenience.
   * @param cb          callback to execute the create request.
   */
  public CreateEventDialog(final Frame owner,
                           final CalendarReadOnly vm,
                           final CalendarView messages,
                           final LocalDate defaultDay,
                           final Callback cb) {
    super(owner, "Create Event", true);
    this.vm = vm;
    this.messages = messages;
    this.cb = cb;

    // Defaults.
    this.dayField.setText(defaultDay.toString());
    this.startField.setText("09:00");
    this.endField.setText("10:00");
    this.allDay.setSelected(false);

    final JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
    form.add(new JLabel("Subject:"));
    form.add(subjectField);
    form.add(new JLabel("Location (optional):"));
    form.add(locationField);
    form.add(new JLabel("Date (yyyy-MM-dd):"));
    form.add(dayField);
    form.add(new JLabel("All day:"));
    form.add(allDay);
    form.add(new JLabel("Start (HH:mm):"));
    form.add(startField);
    form.add(new JLabel("End (HH:mm):"));
    form.add(endField);

    final JPanel buttons = new JPanel();
    final JButton createBtn = new JButton("Create");
    final JButton closeBtn = new JButton("Close");
    buttons.add(createBtn);
    buttons.add(closeBtn);

    setLayout(new BorderLayout(8, 8));
    add(form, BorderLayout.CENTER);
    add(buttons, BorderLayout.SOUTH);

    // Disable/enable time fields when all-day toggles.
    allDay.addActionListener(e -> {
      final boolean timed = !allDay.isSelected();
      startField.setEnabled(timed);
      endField.setEnabled(timed);
    });
    allDay.getActionListeners()[0].actionPerformed(null); // initialize enabled state

    createBtn.addActionListener(e -> onCreate());
    closeBtn.addActionListener(e -> dispose());

    pack();
    setLocationRelativeTo(owner);
  }

  private void onCreate() {
    final String subject = trim(subjectField.getText());
    if (subject.isEmpty()) {
      messages.error("Please enter a subject.");
      return;
    }

    final String loc = trim(locationField.getText());
    final LocalDate day;
    try {
      day = LocalDate.parse(trim(dayField.getText()));
    } catch (DateTimeParseException dtpe) {
      messages.error("Please enter date as yyyy-MM-dd.");
      return;
    }

    try {
      if (allDay.isSelected()) {
        cb.createAllDay(subject, day, loc);
      } else {
        final LocalTime st = parseTime(startField.getText());
        final LocalTime en = parseTime(endField.getText());
        final ZonedDateTime zs = ZonedDateTime.of(day, st, vm.currentZone());
        final ZonedDateTime ze = ZonedDateTime.of(day, en, vm.currentZone());
        cb.createTimed(subject, zs, ze, loc);
      }
      dispose();
    } catch (Exception ex) {
      messages.error(friendly(ex));
    }
  }

  private static LocalTime parseTime(final String s) {
    return LocalTime.parse(trim(s));
  }

  private static String trim(final String s) {
    return s == null ? "" : s.trim();
  }

  private static String friendly(final Exception ex) {
    final String msg = ex == null ? null : ex.getMessage();
    return (msg == null || msg.trim().isEmpty())
        ? "Sorry, that did not work. Please check your inputs."
        : msg;
  }
}
