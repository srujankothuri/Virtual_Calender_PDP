package calendar.gui;

import calendar.controller.GuiController;
import calendar.model.IEvent;
import calendar.view.CalendarReadOnly;
import calendar.view.CalendarView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Swing month view that reads from {@link CalendarReadOnly} and mutates the
 * model only through {@link GuiController}. All GUI logic is kept here; the
 * dialog classes are passive.
 */
public final class SwingCalendarApp {

  /** ISO yyyy-mm-dd format for messages. */
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  private final Frame frame;
  private final CalendarReadOnly vm;
  private final CalendarView messages;
  private final GuiController ctl;

  private LocalDate lastClickedDay;
  private String lastClickedSubject;
  private YearMonth page;
  private JPanel grid;
  private JComboBox<String> calendarBox;
  private JComboBox<String> tzBox;
  private JLabel monthLbl;
  private JLabel statusLbl;
  private final DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("MMM uuuu");

  /** Instances we hide in the UI when the backend cannot delete them. */
  private final Set<String> suppressed = new LinkedHashSet<>();

  /** Construct the app. */
  public SwingCalendarApp(final Frame frame,
                          final CalendarReadOnly vm,
                          final CalendarView messages,
                          final GuiController ctl) {
    this.frame = frame;
    this.vm = vm;
    this.messages = messages;
    this.ctl = ctl;
    this.page = YearMonth.now(vm.currentZone());
  }

  /** Show the window and render the current month. */
  public void show() {
    final JPanel root = new JPanel(new BorderLayout());

    final JPanel top = new JPanel(new BorderLayout());
    top.add(buildLeftToolbar(), BorderLayout.WEST);
    top.add(buildRightToolbar(), BorderLayout.EAST);
    root.add(top, BorderLayout.NORTH);

    final JPanel center = new JPanel(new BorderLayout());
    center.add(buildMonthHeader(), BorderLayout.NORTH);

    this.grid = new JPanel(new GridLayout(0, 7, 8, 8));
    center.add(new JScrollPane(grid), BorderLayout.CENTER);
    root.add(center, BorderLayout.CENTER);

    final JPanel status = new JPanel(new BorderLayout());
    statusLbl = new JLabel(" ");
    statusLbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    status.add(statusLbl, BorderLayout.CENTER);
    root.add(status, BorderLayout.SOUTH);

    repaintMonth();

    frame.setTitle("Virtual Calendar");
    frame.add(root);
    frame.setPreferredSize(new Dimension(980, 700));
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  /* -------------------- toolbars & header -------------------- */

  private JPanel buildLeftToolbar() {
    final JPanel left = new JPanel();

    calendarBox = new JComboBox<>();
    try {
      final List<String> names = ctl.listCalendars();
      for (String c : names) {
        calendarBox.addItem(c);
      }
      calendarBox.setSelectedItem(ctl.currentCalendar());
    } catch (Exception ex) {
      error(friendly(ex));
    }
    calendarBox.addActionListener((ActionEvent e) -> {
      try {
        final String name = (String) calendarBox.getSelectedItem();
        if (name != null) {
          ctl.useCalendar(name);
          refreshCalendarBox();
          refreshHeaderLabel();
          repaintMonth();
        }
      } catch (Exception ex) {
        error(friendly(ex));
      }
    });
    left.add(calendarBox);

    final JButton newCal = new JButton("New Calendar...");
    newCal.addActionListener(ev -> openCreateCalendar());
    left.add(newCal);

    final List<String> zones = new ArrayList<>(ZoneId.getAvailableZoneIds());
    Collections.sort(zones);
    tzBox = new JComboBox<>();
    for (String z : zones) {
      tzBox.addItem(z);
    }
    tzBox.setSelectedItem(vm.currentZone().getId());
    tzBox.setPrototypeDisplayValue("America/Los_Angeles");
    tzBox.addActionListener(ae -> {
      final Object sel = tzBox.getSelectedItem();
      if (sel == null) {
        return;
      }
      final ZoneId zone = ZoneId.of(sel.toString());

      final boolean ok = invokeIfPresent(
          "setTimezone", new Class<?>[]{ZoneId.class}, new Object[]{zone});
      if (!ok) {
        invokeIfPresent(
            "setCalendarTimezone",
            new Class<?>[]{String.class, ZoneId.class},
            new Object[]{ctlCurrentCalendarSafe(), zone}
        );
      }
      refreshHeaderLabel();
      repaintMonth();
      info("Timezone set to " + zone.getId() + ".");
    });
    left.add(new JLabel("TZ:"));
    left.add(tzBox);

    return left;
  }

  private JPanel buildMonthHeader() {
    final JPanel header = new JPanel();

    final JButton prev = new JButton("<");
    monthLbl = new JLabel();
    final JButton next = new JButton(">");

    refreshHeaderLabel();

    prev.addActionListener(ae -> {
      page = page.minusMonths(1);
      refreshHeaderLabel();
      repaintMonth();
    });
    next.addActionListener(ae -> {
      page = page.plusMonths(1);
      refreshHeaderLabel();
      repaintMonth();
    });

    monthLbl.setHorizontalAlignment(SwingConstants.CENTER);
    monthLbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

    header.add(prev);
    header.add(monthLbl);
    header.add(next);
    return header;
  }

  private JPanel buildRightToolbar() {
    final JPanel right = new JPanel();

    final JButton createChooser = new JButton("Create...");
    createChooser.addActionListener(ae -> openCreateChooser());
    right.add(createChooser);

    final JButton editChooser = new JButton("Edit...");
    editChooser.addActionListener(ae -> openEditChooser());
    right.add(editChooser);

    final JButton dashboardBtn = new JButton("Dashboard...");
    dashboardBtn.addActionListener(ae -> openAnalyticsDialog());
    right.add(dashboardBtn);

    return right;
  }

  private void refreshHeaderLabel() {
    final String label = page.format(ymFmt) + " (" + vm.currentZone().getId() + ")";
    if (monthLbl != null) {
      monthLbl.setText(label);
    }
  }

  private String ctlCurrentCalendarSafe() {
    try {
      return ctl.currentCalendar();
    } catch (Exception ex) {
      return null;
    }
  }

  private void refreshCalendarBox() {
    calendarBox.removeAllItems();
    try {
      final List<String> names = ctl.listCalendars();
      for (String c : names) {
        calendarBox.addItem(c);
      }
      calendarBox.setSelectedItem(ctl.currentCalendar());
    } catch (Exception ex) {
      error(friendly(ex));
    }
  }

  private void openCreateCalendar() {
    final String name = JOptionPane.showInputDialog(
        frame, "Name for new calendar:", "New Calendar",
        JOptionPane.QUESTION_MESSAGE);
    if (name == null || name.trim().isEmpty()) {
      return;
    }
    try {
      ctl.createCalendar(name.trim(), vm.currentZone());
      ctl.useCalendar(name.trim());
      refreshCalendarBox();
      refreshHeaderLabel();
      repaintMonth();
      info("Created calendar \"" + name.trim() + "\".");
    } catch (Exception ex) {
      error(friendly(ex));
    }
  }

  /* -------------------- chooser dialogs -------------------- */

  /** Top-bar Create chooser (uses current/last anchor day). */
  private void openCreateChooser() {
    openCreateChooser(defaultAnchorDay());
  }

  /** Day-anchored Create chooser (Create Event + Create Recurring). */
  private void openCreateChooser(final LocalDate anchorDay) {
    final JDialog d = new JDialog(frame, "Create", true);
    d.setLayout(new BorderLayout());

    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    final JButton createEvent = new JButton("Create Event…");
    final JButton createRecurring = new JButton("Create Recurring…");
    final JButton close = new JButton("Close");

    createEvent.addActionListener(ev -> {
      d.dispose();
      openCreateDialog(anchorDay);
    });
    createRecurring.addActionListener(ev -> {
      d.dispose();
      openRecurringDialog(anchorDay);
    });
    close.addActionListener(ev -> d.dispose());

    buttons.add(createEvent);
    buttons.add(createRecurring);
    buttons.add(close);

    d.add(buttons, BorderLayout.CENTER);
    d.pack();
    d.setLocationRelativeTo(frame);
    d.setVisible(true);
  }

  private void openEditChooser() {
    final JDialog d = new JDialog(frame, "Edit", true);
    d.setLayout(new BorderLayout());

    final JPanel form = new JPanel(new GridBagLayout());
    form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    final GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 4, 4, 4);
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;

    final JButton editByDayBtn = new JButton("Edit by Day…");
    editByDayBtn.addActionListener(ev -> {
      d.dispose();
      openEditDialog(defaultAnchorDay(), lastClickedSubject);
    });

    c.gridx = 0;
    c.gridy = 0;
    form.add(new JLabel("Subject:"), c);
    c.gridx = 1;
    final JTextField subjField = new JTextField(18);
    form.add(subjField, c);

    c.gridx = 0;
    c.gridy = 1;
    form.add(new JLabel("From date:"), c);
    c.gridx = 1;
    final JComboBox<String> fromDateBox = new JComboBox<>();
    fromDateBox.setEditable(true);
    form.add(fromDateBox, c);

    c.gridx = 2;
    final JButton findBtn = new JButton("Find dates");
    findBtn.addActionListener(ev -> {
      final String subj = trimToNull(subjField.getText());
      if (subj == null) {
        error("Please enter a subject first.");
        return;
      }
      final List<LocalDate> dates = findSubjectDates(subj, 12, 12);
      fromDateBox.removeAllItems();
      for (LocalDate ld : dates) {
        fromDateBox.addItem(DATE_FMT.format(ld));
      }
      if (fromDateBox.getItemCount() == 0) {
        info("No dates found for \"" + subj + "\" in the past/next 12 months.");
      }
    });
    form.add(findBtn, c);

    final JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    final JButton openBySubject = new JButton("Edit by Subject…");
    final JButton close = new JButton("Close");

    openBySubject.addActionListener(ev -> {
      final String subj = trimToNull(subjField.getText());
      if (subj == null) {
        error("Please enter a subject.");
        return;
      }
      final Object rawSel = fromDateBox.isEditable()
          ? fromDateBox.getEditor().getItem()
          : fromDateBox.getSelectedItem();
      LocalDate anchor = defaultAnchorDay();
      final String raw = trimToNull(rawSel == null ? null : rawSel.toString());
      if (raw != null) {
        try {
          anchor = LocalDate.parse(raw, DATE_FMT);
        } catch (DateTimeParseException ex) {
          error("Invalid date. Please use yyyy-mm-dd.");
          return;
        }
      }
      d.dispose();
      openBulkEditBySubject(subj, anchor);
    });

    close.addActionListener(ev -> d.dispose());

    final JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
    topButtons.add(editByDayBtn);

    bottom.add(openBySubject);
    bottom.add(close);

    d.add(topButtons, BorderLayout.NORTH);
    d.add(form, BorderLayout.CENTER);
    d.add(bottom, BorderLayout.SOUTH);
    d.pack();
    d.setLocationRelativeTo(frame);
    d.setVisible(true);
  }

  private void openBulkEditBySubject(final String subject, final LocalDate fromDay) {
    final JDialog d = new JDialog(frame, "Bulk Edit: \"" + subject + "\"", true);
    d.setLayout(new BorderLayout());

    final JPanel form = new JPanel(new GridBagLayout());
    form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    final GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 4, 4, 4);
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = 0;

    form.add(new JLabel("Editing \"" + subject + "\" from " + DATE_FMT.format(fromDay)), c);

    c.gridy++;
    form.add(new JLabel("Scope:"), c);
    c.gridx = 1;
    final JComboBox<String> scopeBox = new JComboBox<>(
        new String[]{"FOLL (from date onward)", "SERIES (entire series)"});
    form.add(scopeBox, c);

    c.gridx = 0;
    c.gridy++;
    form.add(new JLabel("New subject (optional):"), c);
    c.gridx = 1;
    final JTextField newSubj = new JTextField(18);
    form.add(newSubj, c);

    c.gridx = 0;
    c.gridy++;
    form.add(new JLabel("New location (optional):"), c);
    c.gridx = 1;
    final JTextField newLoc = new JTextField(18);
    form.add(newLoc, c);

    c.gridx = 0;
    c.gridy++;
    form.add(new JLabel("Move to date (optional, yyyy-mm-dd):"), c);
    c.gridx = 1;
    final JTextField moveTo = new JTextField(12);
    form.add(moveTo, c);

    c.gridx = 0;
    c.gridy++;
    form.add(new JLabel("New start (optional HH:mm):"), c);
    c.gridx = 1;
    final JTextField newStart = new JTextField(6);
    form.add(newStart, c);

    c.gridx = 0;
    c.gridy++;
    form.add(new JLabel("New end (optional HH:mm):"), c);
    c.gridx = 1;
    final JTextField newEnd = new JTextField(6);
    form.add(newEnd, c);

    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    final JButton apply = new JButton("Apply");
    final JButton close = new JButton("Close");

    apply.addActionListener(ev -> {
      final String ns = trimToNull(newSubj.getText());
      final String nl = trimToNull(newLoc.getText());
      final String mv = trimToNull(moveTo.getText());
      final LocalTime sTime = parseOptionalTime(newStart.getText());
      final LocalTime eTime = parseOptionalTime(newEnd.getText());
      final boolean series = scopeBox.getSelectedIndex() == 1;

      boolean did = false;
      final ZonedDateTime fromZ = fromDay.atStartOfDay(vm.currentZone());

      try {
        if (ns != null) {
          if (series) {
            try {
              ctl.editSeriesSubjectFrom(subject, fromZ, ns);
            } catch (Exception ignore) { /* best-effort */
            }
            bulkRenameSeriesFallback(subject, fromDay, ns, 18, 18);
          } else {
            ctl.editEventsSubjectFrom(subject, fromZ, ns);
            bulkRenameForwardFallback(subject, fromDay, ns, 12);
          }
          did = true;
        }

        if (nl != null) {
          if (series) {
            try {
              ctl.editSeriesLocationFrom(subject, fromZ, nl);
            } catch (Exception ignore) { /* best-effort */
            }
          } else {
            ctl.editEventsLocationFrom(subject, fromZ, nl);
          }
          did = true;
        }

        if (mv != null) {
          final LocalDate target;
          try {
            target = LocalDate.parse(mv, DATE_FMT);
          } catch (DateTimeParseException ex) {
            error("Invalid move date. Please use yyyy-mm-dd.");
            return;
          }
          final int delta = (int) (target.toEpochDay() - fromDay.toEpochDay());
          ctl.moveSeriesFromDay(subject, fromDay, delta);
          did = true;
        }

        if (sTime != null || eTime != null) {
          if (series) {
            bulkMoveTimesAcrossSeries(subject, fromDay, sTime, eTime, 18, 18);
          } else {
            bulkMoveTimesForward(subject, fromDay, sTime, eTime, 12);
          }
          did = true;
        }
      } catch (Exception ex) {
        error(friendly(ex));
        return;
      }

      if (!did) {
        info("Nothing to change. Fill any field and press Apply.");
        return;
      }

      d.dispose();
      repaintMonth();
    });

    close.addActionListener(ev -> d.dispose());

    buttons.add(apply);
    buttons.add(close);

    d.add(form, BorderLayout.CENTER);
    d.add(buttons, BorderLayout.SOUTH);
    d.pack();
    d.setLocationRelativeTo(frame);
    d.setVisible(true);
  }

  private LocalDate defaultAnchorDay() {
    return (lastClickedDay != null)
        ? lastClickedDay : LocalDate.now(vm.currentZone());
  }

  /* -------------------- create dialogs -------------------- */

  private void openCreateDialog(final LocalDate defaultDay) {
    final CreateEventDialog dialog = new CreateEventDialog(
        frame, vm, messages, defaultDay,
        new CreateEventDialog.Callback() {
          @Override
          public void createAllDay(final String subject,
                                   final LocalDate day,
                                   final String location) {
            try {
              ctl.createAllDayEvent(subject, day);
              final String loc = safe(location);
              if (!loc.isEmpty()) {
                try {
                  ctl.editEventLocationOnDay(subject, day, loc);
                } catch (UnsupportedOperationException uoe) {
                  final ZonedDateTime s = ZonedDateTime.of(
                      day, LocalTime.MIDNIGHT, vm.currentZone());
                  final ZonedDateTime e = s.plusHours(24);
                  ctl.editEventLocationAtStart(subject, s, e, loc);
                }
              }
              info("Created all-day event: \"" + subject + "\" on " + DATE_FMT.format(day) + ".");
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void createTimed(final String subject,
                                  final ZonedDateTime start,
                                  final ZonedDateTime end,
                                  final String location) {
            try {
              ctl.createTimedEvent(subject, start, end);
              final String loc = safe(location);
              if (!loc.isEmpty()) {
                ctl.editEventLocationAtStart(subject, start, end, loc);
              }
              info("Created event: \"" + subject + "\" "
                  + start.toLocalDate() + " @ " + start.toLocalTime()
                  + "–" + end.toLocalTime() + ".");
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }
        }
    );
    dialog.setVisible(true);
    refreshHeaderLabel();
    repaintMonth();
  }

  private void openRecurringDialog(final LocalDate defaultDay) {
    final CreateRecurringDialog dialog = new CreateRecurringDialog(
        frame, vm, messages, defaultDay,
        new CreateRecurringDialog.Callback() {
          @Override
          public void createRecurringByCount(final String subject,
                                             final boolean allDay,
                                             final LocalDate day,
                                             final LocalTime start,
                                             final LocalTime end,
                                             final EnumSet<DayOfWeek> days,
                                             final int count) {
            try {
              ctl.createRecurringByCount(
                  subject, allDay, day, start, end, days, count);
              info("Created recurring (count=" + count + ") for \"" + subject + "\".");
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void createRecurringByUntil(final String subject,
                                             final boolean allDay,
                                             final LocalDate day,
                                             final LocalTime start,
                                             final LocalTime end,
                                             final EnumSet<DayOfWeek> days,
                                             final LocalDate until) {
            try {
              ctl.createRecurringByUntil(
                  subject, allDay, day, start, end, days, until);
              info("Created recurring (until " + DATE_FMT.format(until)
                  + ") for \"" + subject + "\".");
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }
        }
    );

    dialog.setVisible(true);
    refreshHeaderLabel();
    repaintMonth();
  }

  /* -------------------- month grid -------------------- */

  private static final class UniversalClickAdapter
      extends java.awt.event.MouseAdapter {

    private final Runnable action;

    UniversalClickAdapter(final Runnable action) {
      this.action = action;
    }

    @Override
    public void mousePressed(final java.awt.event.MouseEvent e) {
      action.run();
    }

    @Override
    public void mouseClicked(final java.awt.event.MouseEvent e) {
      action.run();
    }

    @Override
    public void mouseReleased(final java.awt.event.MouseEvent e) {
      action.run();
    }
  }

  private void repaintMonth() {
    grid.removeAll();

    final String[] headers = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    for (String h : headers) {
      final JLabel hl = new JLabel(h, SwingConstants.CENTER);
      hl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      grid.add(hl);
    }

    final LocalDate first = page.atDay(1);
    final int firstCol = (first.getDayOfWeek().getValue() + 6) % 7; // Monday=0
    final int len = page.lengthOfMonth();

    int cellIndex = 0;
    for (int r = 0; r < 6; r++) {
      for (int c = 0; c < 7; c++) {
        final JPanel cell = new JPanel(new BorderLayout());
        if (cellIndex >= firstCol && (cellIndex - firstCol) < len) {
          final int dayNum = (cellIndex - firstCol) + 1;
          final LocalDate day = page.atDay(dayNum);

          final JLabel d = new JLabel(Integer.toString(dayNum));
          d.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
          cell.add(d, BorderLayout.NORTH);

          final JPanel list = new JPanel(new GridLayout(0, 1));
          final Set<String> seen = new LinkedHashSet<>();
          for (IEvent e : vm.eventsOn(day)) {
            if (suppressed.contains(sig(e.subject(), e.start(), e.end()))) {
              continue;
            }
            if (seen.add(e.subject())) {
              list.add(buildEventLabel(day, e));
            }
          }
          cell.add(list, BorderLayout.CENTER);
          cell.setBorder(BorderFactory.createEtchedBorder());

          cell.addMouseListener(new UniversalClickAdapter(() -> {
            lastClickedDay = day;
            lastClickedSubject = null;
            handleDayClick(day);
          }));
        }
        grid.add(cell);
        cellIndex++;
      }
    }

    grid.revalidate();
    grid.repaint();
  }

  /** Empty day: open day-anchored Create chooser; else open Edit dialog. */
  private void handleDayClick(final LocalDate day) {
    final List<IEvent> list = vm.eventsOn(day);
    if (list.isEmpty()) {
      lastClickedDay = day;
      info("No events on " + DATE_FMT.format(day) + ".");
      openCreateChooser(day); // includes "Create Recurring…"
    } else {
      openEditDialog(day, null);
    }
  }

  private JLabel buildEventLabel(final LocalDate day, final IEvent e) {
    final String timeText = isAllDay(e)
        ? "(all-day)"
        : ("@ " + e.start().toLocalTime() + "–" + e.end().toLocalTime());
    final String loc = safe(e.location());
    final String withLoc = loc.isEmpty() ? "" : (" - " + loc);
    final String text = "- " + e.subject() + " " + timeText + withLoc;

    final String wrapped = wrapHtmlWidth(text, 120);

    final JLabel lb = new JLabel(wrapped);
    lb.addMouseListener(new UniversalClickAdapter(() -> {
      lastClickedDay = day;
      lastClickedSubject = e.subject();
      openEditDialog(day, e.subject());
    }));
    return lb;
  }

  /* -------------------- edit wiring -------------------- */

  private void openEditDialog(final LocalDate day, final String prefillSubject) {
    final List<IEvent> events = vm.eventsOn(day);
    String prefill = prefillSubject;
    if (prefill == null) {
      final List<String> subjects = new ArrayList<>();
      for (IEvent e : events) {
        if (!subjects.contains(e.subject())) {
          subjects.add(e.subject());
        }
      }
      prefill = subjects.isEmpty() ? null : subjects.get(0);
    }

    final EditEventDialog dialog = new EditEventDialog(
        frame, vm, messages, day, prefill,
        new EditEventDialog.Callback() {
          @Override
          public void editSubjectByDay(final EditEventDialog.Scope scope,
                                       final String subject,
                                       final LocalDate anchorDay,
                                       final String newSubject) {
            try {
              if (scope == EditEventDialog.Scope.SINGLE) {
                editSingleSubjectByDay(subject, anchorDay, newSubject);
              } else if (scope == EditEventDialog.Scope.FOLL) {
                try {
                  ctl.editEventsSubjectFrom(
                      subject, anchorDay.atStartOfDay(vm.currentZone()), newSubject);
                } catch (Exception ignore) { /* best-effort */
                }
                bulkRenameForwardFallback(subject, anchorDay, newSubject, 12);
              } else {
                try {
                  ctl.editSeriesSubjectFrom(
                      subject, anchorDay.atStartOfDay(vm.currentZone()), newSubject);
                } catch (Exception ignore) { /* best-effort */
                }
                bulkRenameSeriesFallback(subject, anchorDay, newSubject, 18, 18);
              }
              afterEdit();
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void editLocationByDay(final EditEventDialog.Scope scope,
                                        final String subject,
                                        final LocalDate anchorDay,
                                        final String newLocation) {
            try {
              handleEditLocation(scope, subject, anchorDay, newLocation);
              afterEdit();
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void editMoveDateByDay(final EditEventDialog.Scope scope,
                                        final String subject,
                                        final LocalDate anchorDay,
                                        final LocalDate newDay) {
            try {
              handleMoveDate(scope, subject, anchorDay, newDay);
              afterEdit();
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void editMoveTimeByDay(final EditEventDialog.Scope scope,
                                        final String subject,
                                        final LocalDate anchorDay,
                                        final LocalTime newStart) {
            try {
              handleMoveTime(scope, subject, anchorDay, newStart);
              afterEdit();
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void editChangeEndByDay(final EditEventDialog.Scope scope,
                                         final String subject,
                                         final LocalDate anchorDay,
                                         final LocalTime newEnd) {
            try {
              handleChangeEnd(scope, subject, anchorDay, newEnd);
              afterEdit();
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void editMoveStartEndByDay(final EditEventDialog.Scope scope,
                                            final String subject,
                                            final LocalDate anchorDay,
                                            final LocalTime newStart,
                                            final LocalTime newEnd) {
            try {
              handleMoveStartEnd(scope, subject, anchorDay, newStart, newEnd);
              afterEdit();
            } catch (Exception ex) {
              error(friendly(ex));
            }
          }

          @Override
          public void openCreateForDay(final LocalDate dayToCreate) {
            openCreateDialog(dayToCreate);
            afterEdit();
          }

          @Override
          public void openCreateRecurringForDay(final LocalDate dayToCreate) {
            openRecurringDialog(dayToCreate);
            afterEdit();
          }
        }
    );
    dialog.setVisible(true);
    refreshHeaderLabel();
    repaintMonth();
  }

  private void afterEdit() {
    refreshHeaderLabel();
    repaintMonth();
  }

  private IEvent selectInstanceOnDay(final LocalDate day, final String subject) {
    for (IEvent e : vm.eventsOn(day)) {
      if (suppressed.contains(sig(e.subject(), e.start(), e.end()))) {
        continue;
      }
      if (e.subject().equals(subject)) {
        return e;
      }
    }
    error("No event \"" + subject + "\" on " + DATE_FMT.format(day) + ".");
    return null;
  }

  private boolean anyInstanceExists(final String subject,
                                    final ZonedDateTime start,
                                    final ZonedDateTime end) {
    final LocalDate d = start.toLocalDate();
    for (IEvent e : vm.eventsOn(d)) {
      if (e.subject().equals(subject)
          && e.start().equals(start)
          && e.end().equals(end)) {
        return true;
      }
    }
    return false;
  }

  private void safeRenameSingle(final IEvent inst, final String oldSubject,
                                final String newSubject) throws Exception {
    final ZonedDateTime s = inst.start();
    final ZonedDateTime e = inst.end();
    final String targetSig = sig(newSubject, s, e);

    if (anyInstanceExists(newSubject, s, e)) {
      suppressed.remove(targetSig); // make visible
    } else {
      recreateInstanceWithNewSubject(inst, newSubject);
    }

    final boolean deleted = bestEffortDelete(oldSubject, s);
    if (!deleted) {
      suppressed.add(sig(oldSubject, s, e));
    }
  }

  private void editSingleSubjectByDay(final String subject,
                                      final LocalDate anchorDay,
                                      final String newSubject) throws Exception {
    if (invokeIfPresent("editEventSubjectOnDay",
        new Class<?>[]{String.class, LocalDate.class, String.class},
        new Object[]{subject, anchorDay, newSubject})) {
      return;
    }

    final IEvent inst = selectInstanceOnDay(anchorDay, subject);
    if (inst == null) {
      return;
    }
    safeRenameSingle(inst, subject, newSubject);
  }

  private void handleEditLocation(final EditEventDialog.Scope scope,
                                  final String subject,
                                  final LocalDate anchorDay,
                                  final String newLocation) throws Exception {
    if (scope == EditEventDialog.Scope.SINGLE) {
      try {
        ctl.editEventLocationOnDay(subject, anchorDay, newLocation);
        return;
      } catch (UnsupportedOperationException ignore) {
        // fall through to start-based edit
      }
      final IEvent inst = selectInstanceOnDay(anchorDay, subject);
      if (inst == null) {
        return;
      }
      ctl.editEventLocationAtStart(subject, inst.start(), inst.end(), newLocation);
    } else if (scope == EditEventDialog.Scope.FOLL) {
      ctl.editEventsLocationFrom(
          subject, anchorDay.atStartOfDay(vm.currentZone()), newLocation);
    } else {
      ctl.editSeriesLocationFrom(
          subject, anchorDay.atStartOfDay(vm.currentZone()), newLocation);
    }
  }

  private void handleMoveDate(final EditEventDialog.Scope scope,
                              final String subject,
                              final LocalDate anchorDay,
                              final LocalDate newDay) throws Exception {
    if (scope == EditEventDialog.Scope.SINGLE) {
      if (invokeIfPresent("moveEventToDateOnDay",
          new Class<?>[]{String.class, LocalDate.class, LocalDate.class},
          new Object[]{subject, anchorDay, newDay})) {
        return;
      }
      final IEvent inst = selectInstanceOnDay(anchorDay, subject);
      if (inst == null) {
        return;
      }
      final Duration dur = Duration.between(inst.start(), inst.end());
      final ZonedDateTime newStart = ZonedDateTime.of(
          newDay, inst.start().toLocalTime(), vm.currentZone());
      final ZonedDateTime newEnd = newStart.plus(dur);
      recreateTimed(subject, inst, newStart, newEnd);
      final boolean deleted = bestEffortDelete(subject, inst.start());
      if (!deleted) {
        suppressed.add(sig(subject, inst.start(), inst.end()));
      }
    } else {
      final int delta = (int) (newDay.toEpochDay() - anchorDay.toEpochDay());
      ctl.moveSeriesFromDay(subject, anchorDay, delta);
    }
  }

  /** Allow FOLL/SERIES edits; match anchor times to operate on that segment only. */
  private void handleMoveTime(final EditEventDialog.Scope scope,
                              final String subject,
                              final LocalDate anchorDay,
                              final LocalTime newStartLocal) throws Exception {
    if (scope == EditEventDialog.Scope.SINGLE) {
      final IEvent inst = selectInstanceOnDay(anchorDay, subject);
      if (inst == null) {
        return;
      }
      final Duration d = Duration.between(inst.start(), inst.end());
      final ZonedDateTime newStart =
          ZonedDateTime.of(anchorDay, newStartLocal, vm.currentZone());
      final ZonedDateTime newEnd = newStart.plus(d);
      recreateTimed(subject, inst, newStart, newEnd);
      final boolean deleted = bestEffortDelete(subject, inst.start());
      if (!deleted) {
        suppressed.add(sig(subject, inst.start(), inst.end()));
      }
      return;
    }

    // FOLL / SERIES: operate only on the segment that currently has the anchor times
    final IEvent anchorInst = selectInstanceOnDay(anchorDay, subject);
    if (anchorInst == null) {
      return;
    }
    final LocalTime anchorStart = anchorInst.start().toLocalTime();
    final LocalTime anchorEnd = anchorInst.end().toLocalTime();

    if (scope == EditEventDialog.Scope.FOLL) {
      bulkMoveTimesForwardMatching(subject, anchorDay, anchorStart, anchorEnd,
          newStartLocal, null, 12);
    } else {
      bulkMoveTimesAcrossSeriesMatching(subject, anchorDay, anchorStart, anchorEnd,
          newStartLocal, null, 18, 18);
    }
  }

  private void handleChangeEnd(final EditEventDialog.Scope scope,
                               final String subject,
                               final LocalDate anchorDay,
                               final LocalTime newEndLocal) throws Exception {
    if (scope == EditEventDialog.Scope.SINGLE) {
      final IEvent inst = selectInstanceOnDay(anchorDay, subject);
      if (inst == null) {
        return;
      }
      if (isAllDay(inst)) {
        error("Cannot change end time for an all-day event.");
        return;
      }
      final ZonedDateTime newEnd =
          ZonedDateTime.of(anchorDay, newEndLocal, vm.currentZone());
      if (!newEnd.isAfter(inst.start())) {
        error("End time must be after start time.");
        return;
      }
      recreateTimed(subject, inst, inst.start(), newEnd);
      final boolean deleted = bestEffortDelete(subject, inst.start());
      if (!deleted) {
        suppressed.add(sig(subject, inst.start(), inst.end()));
      }
      return;
    }

    // FOLL / SERIES with anchor-time matching
    final IEvent anchorInst = selectInstanceOnDay(anchorDay, subject);
    if (anchorInst == null) {
      return;
    }
    final LocalTime anchorStart = anchorInst.start().toLocalTime();
    final LocalTime anchorEnd = anchorInst.end().toLocalTime();

    if (scope == EditEventDialog.Scope.FOLL) {
      bulkMoveTimesForwardMatching(subject, anchorDay, anchorStart, anchorEnd,
          null, newEndLocal, 12);
    } else {
      bulkMoveTimesAcrossSeriesMatching(subject, anchorDay, anchorStart, anchorEnd,
          null, newEndLocal, 18, 18);
    }
  }

  private void handleMoveStartEnd(final EditEventDialog.Scope scope,
                                  final String subject,
                                  final LocalDate anchorDay,
                                  final LocalTime newStartLocal,
                                  final LocalTime newEndLocal) throws Exception {
    if (newStartLocal == null && newEndLocal == null) {
      return;
    }

    if (scope == EditEventDialog.Scope.SINGLE) {
      final IEvent inst = selectInstanceOnDay(anchorDay, subject);
      if (inst == null) {
        return;
      }
      final ZonedDateTime newStart =
          ZonedDateTime.of(anchorDay, newStartLocal != null
              ? newStartLocal : inst.start().toLocalTime(), vm.currentZone());
      final ZonedDateTime newEnd =
          ZonedDateTime.of(anchorDay, newEndLocal != null
              ? newEndLocal : inst.end().toLocalTime(), vm.currentZone());
      if (!newEnd.isAfter(newStart)) {
        error("End time must be after start time.");
        return;
      }
      recreateTimed(subject, inst, newStart, newEnd);
      final boolean deleted = bestEffortDelete(subject, inst.start());
      if (!deleted) {
        suppressed.add(sig(subject, inst.start(), inst.end()));
      }
      return;
    }

    // FOLL / SERIES (match to anchor's current times so "segments" behave like split series)
    final IEvent anchorInst = selectInstanceOnDay(anchorDay, subject);
    if (anchorInst == null) {
      return;
    }
    final LocalTime anchorStart = anchorInst.start().toLocalTime();
    final LocalTime anchorEnd = anchorInst.end().toLocalTime();

    if (scope == EditEventDialog.Scope.FOLL) {
      bulkMoveTimesForwardMatching(subject, anchorDay, anchorStart, anchorEnd,
          newStartLocal, newEndLocal, 12);
    } else {
      bulkMoveTimesAcrossSeriesMatching(subject, anchorDay, anchorStart, anchorEnd,
          newStartLocal, newEndLocal, 18, 18);
    }
  }

  /* -------------------- bulk helpers -------------------- */

  private List<LocalDate> findSubjectDates(final String subject,
                                           final int monthsBack,
                                           final int monthsFwd) {
    final LocalDate start = page.minusMonths(monthsBack).atDay(1);
    final LocalDate end = page.plusMonths(monthsFwd).atEndOfMonth();
    final LinkedHashSet<LocalDate> out = new LinkedHashSet<>();
    LocalDate d = start;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (e.subject().equals(subject)) {
          out.add(d);
          break;
        }
      }
      d = d.plusDays(1);
    }
    return new ArrayList<>(out);
  }

  /** Rename all instances from a day forward (UI-safe). */
  private void bulkRenameForwardFallback(final String oldSubj,
                                         final LocalDate fromDay,
                                         final String newSubj,
                                         final int monthsFwd) throws Exception {
    final LocalDate end = fromDay.plusMonths(monthsFwd)
        .withDayOfMonth(fromDay.plusMonths(monthsFwd).lengthOfMonth());
    LocalDate d = fromDay;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (!e.subject().equals(oldSubj)) {
          continue;
        }
        safeRenameSingle(e, oldSubj, newSubj);
      }
      d = d.plusDays(1);
    }
  }

  /** Rename the entire series around an anchor (UI-safe). */
  private void bulkRenameSeriesFallback(final String oldSubj,
                                        final LocalDate anchor,
                                        final String newSubj,
                                        final int monthsBack,
                                        final int monthsFwd) throws Exception {
    final LocalDate start = anchor.minusMonths(monthsBack).withDayOfMonth(1);
    final LocalDate end = anchor.plusMonths(monthsFwd)
        .withDayOfMonth(anchor.plusMonths(monthsFwd).lengthOfMonth());
    LocalDate d = start;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (!e.subject().equals(oldSubj)) {
          continue;
        }
        safeRenameSingle(e, oldSubj, newSubj);
      }
      d = d.plusDays(1);
    }
  }

  /** Change times for all following instances starting at a day (subject-only). */
  private void bulkMoveTimesForward(final String subject,
                                    final LocalDate fromDay,
                                    final LocalTime newStartLocal,
                                    final LocalTime newEndLocal,
                                    final int monthsFwd) throws Exception {
    final LocalDate end = fromDay.plusMonths(monthsFwd)
        .withDayOfMonth(fromDay.plusMonths(monthsFwd).lengthOfMonth());
    LocalDate d = fromDay;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (!e.subject().equals(subject)) {
          continue;
        }
        final LocalTime startT = newStartLocal != null
            ? newStartLocal : e.start().toLocalTime();
        final LocalTime endT = newEndLocal != null
            ? newEndLocal : e.end().toLocalTime();
        final ZonedDateTime ns = ZonedDateTime.of(d, startT, vm.currentZone());
        final ZonedDateTime ne = ZonedDateTime.of(d, endT, vm.currentZone());

        if (ns.equals(e.start()) && ne.equals(e.end())) {
          continue; // already desired
        }
        if (!ne.isAfter(ns)) {
          continue;
        }
        if (anyInstanceExists(subject, ns, ne)) {
          suppressed.remove(sig(subject, ns, ne));
        } else {
          ctl.createTimedEvent(subject, ns, ne);
        }
        final boolean deleted = bestEffortDelete(subject, e.start());
        if (!deleted) {
          suppressed.add(sig(subject, e.start(), e.end()));
        }
      }
      d = d.plusDays(1);
    }
  }

  /** Change times across whole series window (subject-only). */
  private void bulkMoveTimesAcrossSeries(final String subject,
                                         final LocalDate anchor,
                                         final LocalTime newStartLocal,
                                         final LocalTime newEndLocal,
                                         final int monthsBack,
                                         final int monthsFwd) throws Exception {
    final LocalDate start = anchor.minusMonths(monthsBack).withDayOfMonth(1);
    final LocalDate end = anchor.plusMonths(monthsFwd)
        .withDayOfMonth(anchor.plusMonths(monthsFwd).lengthOfMonth());
    LocalDate d = start;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (!e.subject().equals(subject)) {
          continue;
        }
        final LocalTime startT = newStartLocal != null
            ? newStartLocal : e.start().toLocalTime();
        final LocalTime endT = newEndLocal != null
            ? newEndLocal : e.end().toLocalTime();
        final ZonedDateTime ns = ZonedDateTime.of(d, startT, vm.currentZone());
        final ZonedDateTime ne = ZonedDateTime.of(d, endT, vm.currentZone());

        if (ns.equals(e.start()) && ne.equals(e.end())) {
          continue;
        }
        if (!ne.isAfter(ns)) {
          continue;
        }
        if (anyInstanceExists(subject, ns, ne)) {
          suppressed.remove(sig(subject, ns, ne));
        } else {
          ctl.createTimedEvent(subject, ns, ne);
        }
        final boolean deleted = bestEffortDelete(subject, e.start());
        if (!deleted) {
          suppressed.add(sig(subject, e.start(), e.end()));
        }
      }
      d = d.plusDays(1);
    }
  }

  /* === NEW: time-matching bulk editors (treat “segments” like split series) === */

  private void bulkMoveTimesForwardMatching(final String subject,
                                            final LocalDate fromDay,
                                            final LocalTime anchorStart,
                                            final LocalTime anchorEnd,
                                            final LocalTime newStartLocal,
                                            final LocalTime newEndLocal,
                                            final int monthsFwd) throws Exception {
    final LocalDate end = fromDay.plusMonths(monthsFwd)
        .withDayOfMonth(fromDay.plusMonths(monthsFwd).lengthOfMonth());
    LocalDate d = fromDay;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (!e.subject().equals(subject)) {
          continue;
        }
        if (!e.start().toLocalTime().equals(anchorStart)
            || !e.end().toLocalTime().equals(anchorEnd)) {
          continue;
        }
        final LocalTime startT = newStartLocal != null ? newStartLocal : anchorStart;
        final LocalTime endT = newEndLocal != null ? newEndLocal : anchorEnd;
        final ZonedDateTime ns = ZonedDateTime.of(d, startT, vm.currentZone());
        final ZonedDateTime ne = ZonedDateTime.of(d, endT, vm.currentZone());

        if (ns.equals(e.start()) && ne.equals(e.end())) {
          continue;
        }
        if (!ne.isAfter(ns)) {
          continue;
        }
        if (anyInstanceExists(subject, ns, ne)) {
          suppressed.remove(sig(subject, ns, ne));
        } else {
          ctl.createTimedEvent(subject, ns, ne);
        }
        final boolean deleted = bestEffortDelete(subject, e.start());
        if (!deleted) {
          suppressed.add(sig(subject, e.start(), e.end()));
        }
      }
      d = d.plusDays(1);
    }
  }

  private void bulkMoveTimesAcrossSeriesMatching(final String subject,
                                                 final LocalDate anchor,
                                                 final LocalTime anchorStart,
                                                 final LocalTime anchorEnd,
                                                 final LocalTime newStartLocal,
                                                 final LocalTime newEndLocal,
                                                 final int monthsBack,
                                                 final int monthsFwd) throws Exception {
    final LocalDate start = anchor.minusMonths(monthsBack).withDayOfMonth(1);
    final LocalDate end = anchor.plusMonths(monthsFwd)
        .withDayOfMonth(anchor.plusMonths(monthsFwd).lengthOfMonth());
    LocalDate d = start;
    while (!d.isAfter(end)) {
      for (IEvent e : vm.eventsOn(d)) {
        if (!e.subject().equals(subject)) {
          continue;
        }
        if (!e.start().toLocalTime().equals(anchorStart)
            || !e.end().toLocalTime().equals(anchorEnd)) {
          continue;
        }
        final LocalTime startT = newStartLocal != null ? newStartLocal : anchorStart;
        final LocalTime endT = newEndLocal != null ? newEndLocal : anchorEnd;
        final ZonedDateTime ns = ZonedDateTime.of(d, startT, vm.currentZone());
        final ZonedDateTime ne = ZonedDateTime.of(d, endT, vm.currentZone());

        if (ns.equals(e.start()) && ne.equals(e.end())) {
          continue;
        }
        if (!ne.isAfter(ns)) {
          continue;
        }
        if (anyInstanceExists(subject, ns, ne)) {
          suppressed.remove(sig(subject, ns, ne));
        } else {
          ctl.createTimedEvent(subject, ns, ne);
        }
        final boolean deleted = bestEffortDelete(subject, e.start());
        if (!deleted) {
          suppressed.add(sig(subject, e.start(), e.end()));
        }
      }
      d = d.plusDays(1);
    }
  }

  /* -------------------- fallback helpers & misc -------------------- */

  private void recreateInstanceWithNewSubject(final IEvent inst,
                                              final String newSubject)
      throws Exception {
    if (isAllDay(inst)) {
      final LocalDate day = inst.start().toLocalDate();
      ctl.createAllDayEvent(newSubject, day);
    } else {
      ctl.createTimedEvent(newSubject, inst.start(), inst.end());
    }
    final String loc = safe(inst.location());
    if (!loc.isEmpty()) {
      ctl.editEventLocationAtStart(newSubject, inst.start(), inst.end(), loc);
    }
  }

  private void recreateTimed(final String subject,
                             final IEvent inst,
                             final ZonedDateTime newStart,
                             final ZonedDateTime newEnd) throws Exception {
    ctl.createTimedEvent(subject, newStart, newEnd);
    final String loc = safe(inst.location());
    if (!loc.isEmpty()) {
      ctl.editEventLocationAtStart(subject, newStart, newEnd, loc);
    }
  }

  private boolean bestEffortDelete(final String subject,
                                   final ZonedDateTime start) {
    try {
      final Method m = ctl.getClass().getMethod(
          "deleteBySubjectAt", String.class, ZonedDateTime.class);
      m.invoke(ctl, subject, start);
      return true;
    } catch (ReflectiveOperationException | SecurityException ignored) {
      return false;
    }
  }

  private boolean invokeIfPresent(final String name,
                                  final Class<?>[] paramTypes,
                                  final Object[] args) {
    try {
      final Method m = ctl.getClass().getMethod(name, paramTypes);
      m.invoke(ctl, args);
      return true;
    } catch (ReflectiveOperationException | SecurityException ex) {
      return false;
    }
  }

  private static String sig(final String subject,
                            final ZonedDateTime start,
                            final ZonedDateTime end) {
    return subject + "|" + start.toInstant().toEpochMilli()
        + "|" + end.toInstant().toEpochMilli();
  }

  private static String safe(final String s) {
    return (s == null) ? "" : s.trim();
  }

  private static boolean isAllDay(final IEvent e) {
    return e.start().toLocalTime().equals(LocalTime.MIDNIGHT)
        && e.end().toLocalTime().equals(LocalTime.MIDNIGHT);
  }

  private static String wrapHtmlWidth(final String s, final int px) {
    final String esc = s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
    return "<html><div style='width:" + px + "px'>" + esc + "</div></html>";
  }

  private static String trimToNull(final String s) {
    if (s == null) {
      return null;
    }
    final String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static LocalTime parseOptionalTime(final String raw) {
    final String t = trimToNull(raw);
    if (t == null) {
      return null;
    }
    try {
      return LocalTime.parse(t);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private void info(final String s) {
    messages.info(s);
    if (statusLbl != null) {
      statusLbl.setText(s);
    }
  }

  private void error(final String s) {
    messages.error(s);
    if (statusLbl != null) {
      statusLbl.setText(s);
    }
  }

  /** Human-friendly error remapper for common backend messages. */
  private String friendly(final Exception ex) {
    final String msg = (ex == null || ex.getMessage() == null)
        ? "" : ex.getMessage();
    final String low = msg.toLowerCase();
    if (low.contains("duplicate")) {
      return "That would create a duplicate event. Try a different subject or time.";
    }
    if (msg.isEmpty()) {
      return "That action could not be performed. Please check your inputs.";
    }
    return msg;
  }

  /** Opens the analytics dashboard dialog. */
  private void openAnalyticsDialog() {
    final AnalyticsDialog dlg = new AnalyticsDialog(frame, messages, ctl);
    dlg.setVisible(true);
  }
}
