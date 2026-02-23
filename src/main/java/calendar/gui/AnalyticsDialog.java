package calendar.gui;

import calendar.controller.GuiController;
import calendar.view.CalendarView;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Simple, modal dashboard dialog that lets the user pick a date range and
 * render analytics tex by calling {@link GuiController#computeAnalyticsText(LocalDate, LocalDate)}.
 * This class owns its UI and catches all exceptions to avoid leaking checked exceptions.
 */
public final class AnalyticsDialog extends JDialog {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  private final CalendarView messages;
  private final GuiController ctl;

  private final JTextField fromField = new JTextField(10);
  private final JTextField toField = new JTextField(10);
  private final JTextArea output = new JTextArea(18, 60);

  /**
   * Create the analytics dialog.
   *
   * @param owner    parent frame
   * @param messages sink for info/error messages
   * @param ctl      GUI controller used to compute analytics
   */
  public AnalyticsDialog(final Frame owner, final CalendarView messages, final GuiController ctl) {
    super(owner, "Calendar Dashboard", true);
    this.messages = messages;
    this.ctl = ctl;

    setLayout(new BorderLayout());
    add(buildTopBar(), BorderLayout.NORTH);
    add(buildOutput(), BorderLayout.CENTER);
    add(buildButtons(), BorderLayout.SOUTH);

    // Sensible defaults: today to today.
    final LocalDate today = LocalDate.now();
    fromField.setText(DATE_FMT.format(today));
    toField.setText(DATE_FMT.format(today));

    pack();
    setLocationRelativeTo(owner);
  }

  private JPanel buildTopBar() {
    final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
    top.add(new JLabel("From (yyyy-mm-dd):"));
    top.add(fromField);
    top.add(new JLabel("To (yyyy-mm-dd):"));
    top.add(toField);
    return top;
  }

  private JScrollPane buildOutput() {
    output.setEditable(false);
    output.setLineWrap(true);
    output.setWrapStyleWord(true);
    output.setMargin(new Insets(8, 8, 8, 8));
    return new JScrollPane(output);
  }

  private JPanel buildButtons() {
    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    final JButton show = new JButton("Show");
    final JButton close = new JButton("Close");

    show.addActionListener(this::handleShow);
    close.addActionListener(ev -> dispose());

    buttons.add(show);
    buttons.add(close);
    return buttons;
  }

  private void handleShow(final ActionEvent ev) {
    final LocalDate from;
    final LocalDate to;
    try {
      from = LocalDate.parse(fromField.getText().trim(), DATE_FMT);
      to = LocalDate.parse(toField.getText().trim(), DATE_FMT);
    } catch (DateTimeParseException ex) {
      messages.error("Invalid date. Please use yyyy-mm-dd.");
      return;
    }
    if (to.isBefore(from)) {
      messages.error("'To' date must be on/after 'From' date.");
      return;
    }

    try {
      final String text = ctl.computeAnalyticsText(from, to);
      output.setText(text == null ? "(no data)" : text);
      output.setCaretPosition(0);
      messages.info("Dashboard for " + DATE_FMT.format(from) + " to " + DATE_FMT.format(to) + ".");
    } catch (Exception ex) {
      messages.error("Unable to compute analytics: " + ex.getMessage());
    }
  }
}
