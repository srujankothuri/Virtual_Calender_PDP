package calendar.controller.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import calendar.controller.CalendarManager;
import calendar.model.IEvent;
import calendar.view.CalendarView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.Before;
import org.junit.Test;

/**
 * CLI flow tests for {@link EditEventCommands}.
 * Ensures parsing delegates to manager and changes are visible in the model.
 */
public final class EditEventCommandsTest {

  /** Spy view used to capture info/error lines. */
  private static final class ViewSpy implements CalendarView {
    private final List<String> info = new CopyOnWriteArrayList<>();
    private final List<String> errors = new CopyOnWriteArrayList<>();

    @Override
    public void info(final String line) {
      info.add(line);
    }

    @Override
    public void error(final String line) {
      errors.add(line);
    }

    List<String> infos() {
      return info;
    }

    List<String> errors() {
      return errors;
    }
  }

  private CalendarManager manager;
  private ViewSpy view;
  private EditEventCommands cmd;
  private ZoneId zone;
  private ZonedDateTime start;
  private ZonedDateTime end;

  /** Builds a calendar and a seed event for edit flows. */
  @Before
  public void setUp() {
    manager = new CalendarManager();
    view = new ViewSpy();
    cmd = new EditEventCommands();

    zone = ZoneId.of("America/New_York");
    manager.createCalendar("Main", zone);
    manager.useCalendar("Main");

    start = ZonedDateTime.of(2024, 3, 21, 9, 0, 0, 0, zone);
    end = start.plusHours(1);
    manager.addSingle("Meeting", start, end, "", "PUBLIC");
  }

  /** Edits the subject of a single instance via CLI. */
  @Test
  public void editSingle_subject_changesSubject() {
    final String startLocal = "2024-03-21T09:00";
    final String endLocal = "2024-03-21T10:00";

    final String line = "edit event subject Meeting from "
        + startLocal + " to " + endLocal + " with \"Team Sync\"";

    final boolean handled = cmd.tryRun(line, manager, view);
    assertTrue(handled);

    final IEvent newEv = manager.findBySubjectAt("Team Sync", start);
    assertEquals("Team Sync", newEv.subject());
    // Prove info line was emitted (kills mutation removing view.info).
    assertTrue(view.infos().stream().anyMatch(s -> s.contains("Edited subject")));
  }

  /** Ensures quoted values for location are parsed correctly. */
  @Test
  public void editSingle_location_parsesQuotedValue() {
    final String startLocal = "2024-03-21T09:00";
    final String endLocal = "2024-03-21T10:00";

    final String line = "edit event location Meeting from "
        + startLocal + " to " + endLocal + " with \"Room 101\"";

    final boolean handled = cmd.tryRun(line, manager, view);
    assertTrue(handled);

    final IEvent ev = manager.findBySubjectAt("Meeting", start);
    assertEquals("Room 101", ev.location());
  }

  /** Quote-trim boundary: with "" the stored description must be empty, not \"\". */
  @Test
  public void editSingle_description_emptyQuotes_becomesEmptyString() {
    final String s = "2024-03-21T09:00";
    final String e = "2024-03-21T10:00";
    final String line = "edit event description Meeting from "
        + s + " to " + e + " with \"\"";

    assertTrue(cmd.tryRun(line, manager, view));
    final IEvent ev = manager.findBySubjectAt("Meeting", start);
    assertEquals("", ev.description());
  }

  /** When no series exists, events-from falls back to single edit. */
  @Test
  public void editEventsFrom_status_fallsBackToSingleWhenNoSeries() {
    final String startLocal = "2024-03-21T09:00";
    final String line = "edit events status Meeting from " + startLocal + " with PUBLIC";

    final boolean handled = cmd.tryRun(line, manager, view);
    assertTrue(handled);

    final IEvent ev = manager.findBySubjectAt("Meeting", start);
    assertEquals("PUBLIC", ev.status());
    assertTrue(view.infos().stream().anyMatch(s -> s.contains("Edited status")));
  }

  /** Unknown edit property should be reported as an error. */
  @Test
  public void badProperty_isReportedAndHandled() {
    final String s = "2024-03-21T09:00";
    final String e = "2024-03-21T10:00";
    final String line = "edit event bogus Meeting from " + s + " to " + e + " with X";

    final boolean handled = cmd.tryRun(line, manager, view);
    assertTrue(handled);

    assertTrue(view.errors().stream().anyMatch(t -> t.contains("Unknown property")));
  }

  /** Try-run returns false when 'edit' is present but syntax matches no pattern. */
  @Test
  public void tryRun_returnsFalseForUnmatchedEditSyntax() {
    final boolean handled = cmd.tryRun("edit something invalid", manager, view);
    assertFalse(handled);
  }

  /** Early return when no active calendar (zone null). */
  @Test
  public void tryRun_returnsTrueWhenNoActiveCalendar() {
    final CalendarManager m2 = new CalendarManager(); // no calendar, no zone
    final boolean handled = cmd.tryRun(
        "edit event subject X from 2024-03-21T09:00 to 2024-03-21T10:00 with Y",
        m2, view);
    assertTrue(handled);
  }

  /** Events-from not-found branch: emits error and returns. */
  @Test
  public void editEventsFrom_noEventAtTime_emitsError() {
    final String line = "edit events status Unknown from 2024-03-21T09:00 with PRIVATE";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(s -> s.contains("No event 'Unknown'")));
  }

  /** Events-from multi-edit branch: edits all following instances. */
  @Test
  public void editEventsFrom_editsAllFollowingInstances() {
    // Create a second "Meeting" one week later.
    final ZonedDateTime later = start.plusWeeks(1);
    manager.addSingle("Meeting", later, later.plusHours(1), "", "PUBLIC");

    final String line = "edit events description Meeting from 2024-03-21T09:00 with Updated";
    assertTrue(cmd.tryRun(line, manager, view));

    // Both instances at/after 'from' must be updated.
    final IEvent ev1 = manager.findBySubjectAt("Meeting", start);
    final IEvent ev2 = manager.findBySubjectAt("Meeting", later);
    assertEquals("Updated", ev1.description());
    assertEquals("Updated", ev2.description());
    assertTrue(view.infos().stream().anyMatch(s -> s.contains("Edited description of events")));
  }

  /** Series-edit not-found branch: emits error and returns. */
  @Test
  public void editSeries_noEventAtTime_emitsError() {
    final String line = "edit series status Unknown from 2024-03-21T09:00 with PRIVATE";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(s -> s.contains("No event 'Unknown'")));
  }

  /** Series-edit with single instance falls back to single-instance edit. */
  @Test
  public void editSeries_singleInstance_fallsBackToSingle() {
    final String line = "edit series location Meeting from 2024-03-21T09:00 with Room 202";
    assertTrue(cmd.tryRun(line, manager, view));
    final IEvent ev = manager.findBySubjectAt("Meeting", start);
    assertEquals("Room 202", ev.location());
    assertTrue(view.infos().stream().anyMatch(s -> s.contains("of event \"Meeting\"")));
  }

  /** Series-edit with multiple instances updates the entire series. */
  @Test
  public void editSeries_multipleInstances_updatesEntireSeries() {
    // Add another Meeting before/after to ensure size > 1.
    final ZonedDateTime other = start.plusDays(2);
    manager.addSingle("Meeting", other, other.plusHours(1), "", "PUBLIC");

    final String line = "edit series status Meeting from 2024-03-21T09:00 with PRIVATE";
    assertTrue(cmd.tryRun(line, manager, view));

    final IEvent ev1 = manager.findBySubjectAt("Meeting", start);
    final IEvent ev2 = manager.findBySubjectAt("Meeting", other);
    assertEquals("PRIVATE", ev1.status());
    assertEquals("PRIVATE", ev2.status());
    assertTrue(view.infos().stream()
        .anyMatch(s -> s.contains("of entire series \"Meeting\"")));
  }

  /** Catch path in single-edit: bad start token causes parse failure. */
  @Test
  public void editSingle_badStartToken_isHandledAsError() {
    final String line = "edit event subject Meeting from BAD to BAD with X";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(s -> s.startsWith("Failed to edit event")));
  }

  /** Catch path in events-edit: bad from-time token causes parse failure. */
  @Test
  public void editEvents_badFromToken_isHandledAsError() {
    final String line = "edit events status Meeting from NOT_A_TIME with PUBLIC";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(s -> s.startsWith("Failed to edit events")));
  }

  /** Catch path in series-edit: bad time token causes parse failure. */
  @Test
  public void editSeries_badAnyToken_isHandledAsError() {
    final String line = "edit series status Meeting from BAD_TIME with PRIVATE";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(s -> s.startsWith("Failed to edit series")));
  }

  /** validateEdit: subject must not be empty. */
  @Test
  public void validate_subjectEmpty_isError() {
    final String s = "2024-03-21T09:00";
    final String e = "2024-03-21T10:00";
    final String line = "edit event subject Meeting from " + s + " to " + e + " with \"\"";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(t -> t.contains("Subject cannot be empty")));
    // unchanged subject
    assertEquals("Meeting", manager.findBySubjectAt("Meeting", start).subject());
  }

  /** validateEdit: invalid start date/time string is rejected. */
  @Test
  public void validate_startInvalid_isError_andNoChange() {
    final String s = "2024-03-21T09:00";
    final String e = "2024-03-21T10:00";
    final String line = "edit event start Meeting from " + s + " to " + e + " with BAD";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(t -> t.contains("Invalid date/time for start")));
    // start remains unchanged
    assertEquals(start, manager.findBySubjectAt("Meeting", start).start());
  }

  /** validateEdit: invalid end date/time string is rejected. */
  @Test
  public void validate_endInvalid_isError_andNoChange() {
    final String s = "2024-03-21T09:00";
    final String e = "2024-03-21T10:00";
    final String line = "edit event end Meeting from " + s + " to " + e + " with BAD_END";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream().anyMatch(t -> t.contains("Invalid date/time for end")));
    // end remains unchanged
    assertEquals(end, manager.findBySubjectAt("Meeting", start).end());
  }

  /** validateEdit: status must be PUBLIC or PRIVATE. */
  @Test
  public void validate_statusInvalid_isError_andNoChange() {
    final String s = "2024-03-21T09:00";
    final String line = "edit events status Meeting from " + s + " with SHARED";
    assertTrue(cmd.tryRun(line, manager, view));
    assertTrue(view.errors().stream()
        .anyMatch(t -> t.contains("Status must be PUBLIC or PRIVATE")));
    // still the original status
    assertEquals("PUBLIC", manager.findBySubjectAt("Meeting", start).status());
  }

  // --- helpers for local window creation (used by all-day additions) ---

  private static ZonedDateTime atStartOfDay(final LocalDate d, final ZoneId z) {
    return d.atTime(LocalTime.MIN).atZone(z);
  }
}
