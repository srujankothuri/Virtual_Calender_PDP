package calendar.gui;

import calendar.model.IEvent;
import calendar.view.CalendarReadOnly;
import calendar.view.CalendarView;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Dialog used by the month view to edit events.
 *
 * <p>The dialog stays open so the user can apply multiple changes.
 * All mutations are delegated to a {@link Callback} implemented by the host
 * view (which forwards to the controller). This keeps the dialog passive.</p>
 */
public final class EditEventDialog extends JDialog {

  /** Scope of an edit request. */
  public enum Scope { SINGLE, FOLL, SERIES }

  /** Date format used in text fields. */
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  /** Time format (24h). */
  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm");

  /** Adapter that performs the actual edits. */
  public interface Callback {

    /**
     * Rename events anchored on a given day.
     *
     * @param scope SINGLE/FOLL/SERIES scope
     *
     * @param subject existing subject to match
     *
     * @param anchorDay day that identifies the anchor instance
     *
     * @param newSubject replacement subject
     * @throws Exception forwarded from controller on failure
     */
    void editSubjectByDay(Scope scope, String subject, LocalDate anchorDay,
                          String newSubject) throws Exception;

    /**
     * Change location for events anchored on a given day.
     *
     * @param scope SINGLE/FOLL/SERIES scope
     *
     * @param subject existing subject to match
     *
     * @param anchorDay day that identifies the anchor instance
     *
     * @param newLocation new location text
     * @throws Exception forwarded from controller on failure
     */
    void editLocationByDay(Scope scope, String subject, LocalDate anchorDay,
                           String newLocation) throws Exception;

    /**
     * Move the anchored instance(s) to a different date, preserving duration.
     *
     * @param scope SINGLE/FOLL/SERIES scope
     *
     * @param subject existing subject to match
     *
     * @param anchorDay day that identifies the anchor instance
     *
     * @param newDay destination calendar day
     * @throws Exception forwarded from controller on failure
     */
    void editMoveDateByDay(Scope scope, String subject, LocalDate anchorDay,
                           LocalDate newDay) throws Exception;

    /**
     * Change the start time of the anchored instance(s).
     *
     * @param scope SINGLE/FOLL/SERIES scope
     *
     * @param subject existing subject to match
     *
     * @param anchorDay day that identifies the anchor instance
     *
     * @param newStart new start time (24h)
     * @throws Exception forwarded from controller on failure
     */
    void editMoveTimeByDay(Scope scope, String subject, LocalDate anchorDay,
                           LocalTime newStart) throws Exception;

    /**
     * Change only the end time of the anchored instance(s).
     *
     * @param scope SINGLE/FOLL/SERIES scope
     *
     * @param subject existing subject to match
     *
     * @param anchorDay day that identifies the anchor instance
     *
     * @param newEnd new end time (24h)
     * @throws Exception forwarded from controller on failure
     */
    void editChangeEndByDay(Scope scope, String subject, LocalDate anchorDay,
                            LocalTime newEnd) throws Exception;

    /** Apply start+end together atomically to avoid double-recreate races. */
    void editMoveStartEndByDay(Scope scope, String subject, LocalDate anchorDay,
                               LocalTime newStart, LocalTime newEnd) throws Exception;

    /** Convenience buttons so the user can create from the edit dialog. */
    void openCreateForDay(LocalDate dayToCreate);

    /**
     * Open the recurring-event creation flow prefilled for the given day.
     *
     * @param dayToCreate day to preselect in the create-recurring dialog
     */
    void openCreateRecurringForDay(LocalDate dayToCreate);
  }

  private final CalendarReadOnly vm;
  private final CalendarView messages;
  private final LocalDate initialDay;
  private final Callback cb;

  private final JComboBox<Scope> scopeBox = new JComboBox<>(Scope.values());
  private final JTextField dayField = new JTextField(10);
  private final JComboBox<String> subjectBox = new JComboBox<>();
  private final JTextField newSubjectField = new JTextField(18);
  private final JTextField newLocationField = new JTextField(18);
  private final JTextField newDateField = new JTextField(10);
  private final JTextField newStartField = new JTextField(6);
  private final JTextField newEndField = new JTextField(6);

  /**
   * Build the dialog.
   *
   * @param owner parent frame
   *
   * @param vm calendar (read-only)
   *
   * @param messages UI message sink
   *
   * @param anchorDay day to preselect
   *
   * @param prefill preselected subject or null
   *
   * @param cb callback that performs edits
   */
  public EditEventDialog(final Frame owner,
                         final CalendarReadOnly vm,
                         final CalendarView messages,
                         final LocalDate anchorDay,
                         final String prefill,
                         final Callback cb) {
    super(owner, "Edit Event", true);
    this.vm = Objects.requireNonNull(vm);
    this.messages = Objects.requireNonNull(messages);
    this.initialDay = Objects.requireNonNull(anchorDay);
    this.cb = Objects.requireNonNull(cb);

    setLayout(new BorderLayout());
    add(buildForm(), BorderLayout.CENTER);
    add(buildButtons(), BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(owner);
    refreshSubjects(initialDay, prefill);
  }

  /** Build the input form. */
  private JPanel buildForm() {
    final JPanel p = new JPanel(new GridBagLayout());
    p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    final GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 4, 4, 4);
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = 0;

    p.add(new JLabel("Scope:"), c);
    c.gridx = 1;
    scopeBox.setSelectedItem(Scope.SINGLE);
    p.add(scopeBox, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("Day (yyyy-mm-dd):"), c);
    c.gridx = 1;
    dayField.setText(DATE_FMT.format(initialDay));
    p.add(dayField, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("Subject:"), c);
    c.gridx = 1;
    subjectBox.setEditable(false);
    p.add(subjectBox, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("New subject:"), c);
    c.gridx = 1;
    p.add(newSubjectField, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("New location:"), c);
    c.gridx = 1;
    p.add(newLocationField, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("Move to date (yyyy-mm-dd):"), c);
    c.gridx = 1;
    p.add(newDateField, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("New start (HH:mm):"), c);
    c.gridx = 1;
    p.add(newStartField, c);

    c.gridx = 0;
    c.gridy++;
    p.add(new JLabel("New end (HH:mm):"), c);
    c.gridx = 1;
    p.add(newEndField, c);

    return p;
  }

  /** Build buttons row. */
  private JPanel buildButtons() {
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    final JButton refreshBtn = new JButton("Refresh day");
    refreshBtn.addActionListener(e -> {
      final LocalDate d = parseDayOrError(dayField.getText());
      if (d != null) {
        refreshSubjects(d, null);
      }
    });

    final JButton createBtn = new JButton("Create…");
    createBtn.addActionListener(e -> cb.openCreateForDay(parseDayOrDefault()));

    final JButton createRecBtn = new JButton("Create Recurring…");
    createRecBtn.addActionListener(e -> cb.openCreateRecurringForDay(parseDayOrDefault()));

    final JButton applyBtn = new JButton("Apply");
    applyBtn.addActionListener(e -> applyAll());

    final JButton closeBtn = new JButton("Close");
    closeBtn.addActionListener(e -> dispose());

    p.add(refreshBtn);
    p.add(createBtn);
    p.add(createRecBtn);
    p.add(applyBtn);
    p.add(closeBtn);
    return p;
  }

  /** Populate subjects for the given day. */
  private void refreshSubjects(final LocalDate day, final String prefill) {
    subjectBox.removeAllItems();
    final Set<String> unique = new LinkedHashSet<>();
    final List<IEvent> events = vm.eventsOn(day);
    for (IEvent ev : events) {
      unique.add(ev.subject());
    }
    for (String s : unique) {
      subjectBox.addItem(s);
    }
    if (prefill != null) {
      subjectBox.setSelectedItem(prefill);
    } else if (subjectBox.getItemCount() > 0) {
      subjectBox.setSelectedIndex(0);
    }
    if (subjectBox.getItemCount() == 0) {
      messages.info("No events on " + DATE_FMT.format(day) + ".");
    }
  }

  /** Apply whichever fields are filled. Start+end are applied atomically. */
  private void applyAll() {
    final LocalDate anchor = parseDayOrError(dayField.getText());
    if (anchor == null) {
      return;
    }
    final String subj = (String) subjectBox.getSelectedItem();
    if (subj == null || subj.trim().isEmpty()) {
      messages.error("Please select a Subject from the list.");
      return;
    }

    boolean did = false;

    final String ns = trimToNull(newSubjectField.getText());
    if (ns != null) {
      try {
        cb.editSubjectByDay(scope(), subj, anchor, ns);
        did = true;
        refreshSubjects(anchor, ns);
        messages.info("Edited subject of \"" + subj + "\" to \"" + ns + "\".");
      } catch (Exception ex) {
        messages.error(friendly(ex));
        return;
      }
    }

    final String nl = trimToNull(newLocationField.getText());
    if (nl != null) {
      try {
        cb.editLocationByDay(scope(), subj, anchor, nl);
        did = true;
        messages.info("Edited location of \"" + subj + "\".");
      } catch (Exception ex) {
        messages.error(friendly(ex));
        return;
      }
    }

    final LocalDate newDay = parseOptionalDay(newDateField.getText());
    if (newDay != null) {
      try {
        cb.editMoveDateByDay(scope(), subj, anchor, newDay);
        did = true;
        dayField.setText(DATE_FMT.format(newDay));
        refreshSubjects(newDay, subj);
        messages.info("Moved \"" + subj + "\" to " + DATE_FMT.format(newDay) + ".");
      } catch (Exception ex) {
        messages.error(friendly(ex));
        return;
      }
    }

    // Time edits: if both are present, do one atomic call.
    final LocalTime newStart = parseOptionalTime(newStartField.getText());
    final LocalTime newEnd = parseOptionalTime(newEndField.getText());
    try {
      if (newStart != null && newEnd != null) {
        cb.editMoveStartEndByDay(scope(), subj, anchor, newStart, newEnd);
        did = true;
        messages.info("Changed time of \"" + subj + "\" on " + DATE_FMT.format(anchor) + ".");
      } else if (newStart != null) {
        cb.editMoveTimeByDay(scope(), subj, anchor, newStart);
        did = true;
        messages.info("Changed start time of \"" + subj + "\" on " + DATE_FMT.format(anchor) + ".");
      } else if (newEnd != null) {
        cb.editChangeEndByDay(scope(), subj, anchor, newEnd);
        did = true;
        messages.info("Changed end time of \"" + subj + "\" on " + DATE_FMT.format(anchor) + ".");
      }
    } catch (Exception ex) {
      messages.error(friendly(ex));
      return;
    }

    if (!did) {
      messages.info("Nothing to change. Fill any field and press Apply.");
    }
  }

  private Scope scope() {
    final Object obj = scopeBox.getSelectedItem();
    return obj instanceof Scope ? (Scope) obj : Scope.SINGLE;
  }

  private LocalDate parseDayOrDefault() {
    final LocalDate d = parseOptionalDay(dayField.getText());
    return d != null ? d : initialDay;
  }

  private static String trimToNull(final String s) {
    if (s == null) {
      return null;
    }
    final String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private LocalDate parseOptionalDay(final String raw) {
    final String t = trimToNull(raw);
    if (t == null) {
      return null;
    }
    return parseDayOrError(t);
  }

  private LocalDate parseDayOrError(final String raw) {
    try {
      return LocalDate.parse(raw, DATE_FMT);
    } catch (DateTimeParseException ex) {
      messages.error("Invalid date. Please use yyyy-mm-dd.");
      return null;
    }
  }

  private static LocalTime parseOptionalTime(final String raw) {
    final String t = trimToNull(raw);
    if (t == null) {
      return null;
    }
    try {
      return LocalTime.parse(t, TIME_FMT);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private static String friendly(final Exception ex) {
    final String msg = ex == null ? null : ex.getMessage();
    return (msg == null || msg.trim().isEmpty())
        ? "Sorry, that did not work. Please check your inputs."
        : msg;
  }
}