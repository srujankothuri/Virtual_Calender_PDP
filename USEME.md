# USEME

## Virtual Calendar — Developer Runbook

This document is the practical runbook for building, running, validating, and submitting the Virtual Calendar project.

> **Runtime baseline**: Java 11  
> **Primary launcher**: `calendar.CalendarRunner`  
> **GUI entry point**: `calendar.gui.SwingCalendarApp`

---

## 1) Build

From repository root:

```bash
./gradlew clean jar
```

Output artifact is generated under:

```text
build/libs/
```

If you only need incremental build output:

```bash
./gradlew jar
```

---

## 2) Run Modes

`CalendarRunner` supports GUI, interactive CLI, and headless CLI.

### GUI mode

```bash
java -jar build/libs/<JAR-NAME>.jar --mode gui
```

If your manifest already defaults to GUI, this also works:

```bash
java -jar build/libs/<JAR-NAME>.jar
```

### Interactive CLI mode

```bash
java -jar build/libs/<JAR-NAME>.jar --mode interactive
```

### Headless CLI mode

```bash
java -jar build/libs/<JAR-NAME>.jar --mode headless res/commands.txt
```

**Shortcut** (single argument as script path):

```bash
java -jar build/libs/<JAR-NAME>.jar res/commands.txt
```

---

## 3) CLI Script Example

Create `res/commands.txt` with one command per line:

```text
create calendar --name Work --timezone America/New_York
use calendar --name Work
create event "Sprint Planning" from 2025-11-03T10:00 to 2025-11-03T11:00
create event "Code Review" on 2025-11-04
create event "Standup" from 2025-11-05T09:00 to 2025-11-05T09:15 repeats MWF for 4 times
export cal res/hw4_calendar.csv
exit
```

Run it:

```bash
java -jar build/libs/<JAR-NAME>.jar --mode headless res/commands.txt
```

---

## 4) Developer Validation Checklist

Before submission, validate these paths:

- Build succeeds with no compilation failures.
- CLI interactive mode accepts and executes valid commands.
- Headless mode runs script end-to-end and exports output files.
- GUI launches, navigates months, and supports create/edit flows.
- Exported CSV imports into Google Calendar without schema errors.

Recommended checks:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
```

---

## 5) GUI Verification Guide

When smoke-testing the Swing app, verify:

- Header month includes timezone context.
- Prev/Next month navigation is stable.
- Create dialog supports timed and all-day events.
- Edit dialog updates single instance / future / series correctly.
- Subject/location edits persist after refresh.
- Day selection and toolbar pickers are consistent.

If `java -jar` opens CLI instead of GUI, launch GUI directly:

```bash
java -cp build/libs/<JAR-NAME>.jar calendar.gui.SwingCalendarApp
```

---

## 6) Google Calendar Import (for deliverables)

1. Open Google Calendar.
2. (Recommended) Create a dedicated calendar (e.g., `HW4 Calendar`).
3. Navigate to **Settings → Import & export → Import**.
4. Upload `res/hw4_calendar.csv`.
5. Import into your dedicated calendar.
6. Confirm event counts and timestamps.

**Timezone note**: set Google Calendar timezone to `Eastern Time (US & Canada)` to match project expectations for screenshots.

---

## 7) Required `res/` Submission Assets

Keep these files in `res/` for grading:

- `commands.txt` — valid script used for headless execution
- `invalid.txt` — intentionally invalid command set for error-path coverage
- `google-calendar-screenshot.png` — screenshot showing imported events
- `class-diagram.md` — architecture class diagram (Mermaid)
- `hw4_calendar.csv` *(optional but useful)* — exported CSV used in import

---

## 8) Troubleshooting

### Headless mode says “Not a file”
Ensure the script argument points to a file, not a directory.

### Process exits with code 2
Usually indicates invalid mode/arguments or missing required headless script path.

### CSV import fails
Re-export and verify CSV header/format is Google Calendar compatible.

### Duplicate imports in Google Calendar
Delete the temporary test calendar and re-import into a fresh calendar.

### GUI won’t launch from `java -jar`
Use classpath launch with `calendar.gui.SwingCalendarApp`.

---

## 9) Architecture Quick Notes

- **CLI orchestration**: `calendar.CalendarRunner`
- **GUI entry**: `calendar.gui.SwingCalendarApp`
- **GUI mutation interface**: `calendar.controller.GuiController`
- **Read-only projection for GUI**: `calendar.view.CalendarReadOnly`
- **Event projection**: `calendar.model.IEvent`
- **Console output layer**: `calendar.view.ConsoleView`

Design intent remains standard MVC: Swing handles presentation/input only, while controller + model own all business logic and state transitions.
