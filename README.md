# Virtual Calendar

A Java calendar application with both a command-line interface (interactive + script/headless) and a Swing GUI.

This project supports:
- Multiple named calendars with independent time zones.
- Creating single and recurring events (timed and all-day).
- Editing single events, future events, or entire series.
- Copying events between calendars and across dates.
- Querying events and busy status.
- Exporting calendars to `.csv` and `.ical`/`.ics`.
- Calendar analytics dashboard for a selected date interval (CLI + GUI).

---

## Tech Stack

- **Language:** Java 11
- **Build:** Gradle
- **Testing:** JUnit 4 (with JaCoCo + PIT configured)
- **UI:** Java Swing

---

## Project Structure

```text
src/main/java/
  CalendarRunner.java                 # unified launcher (GUI / interactive / headless)
  calendar/
    controller/                       # command orchestration + services
      commands/                       # CLI command parsing and handlers
      services/                       # operation-level business logic
      export/                         # CSV/ICAL formatter layer
    model/                            # domain model + analytics
    view/                             # console view + read-only model projection for GUI
    gui/                              # Swing application and dialogs
    util/                             # date/day parsing utilities

src/test/java/                        # unit tests for controller/model/view/gui
res/                                  # sample command files, exports, and screenshots
```

---

## Build

From repository root:

```bash
./gradlew clean build
```

Create jar only:

```bash
./gradlew jar
```

The generated jar is in `build/libs/`.

---

## Run

`CalendarRunner` is the main entry point and supports multiple modes.

### 1) GUI (default)

```bash
java -jar build/libs/calendar-1.0.jar
```

or explicitly:

```bash
java -jar build/libs/calendar-1.0.jar --mode gui
```

### 2) Interactive CLI

```bash
java -jar build/libs/calendar-1.0.jar --mode interactive
```

### 3) Headless script mode

```bash
java -jar build/libs/calendar-1.0.jar --mode headless res/commands.txt
```

Shortcut form (single argument = script file):

```bash
java -jar build/libs/calendar-1.0.jar res/commands.txt
```

---

## CLI Command Reference

> Dates use `YYYY-MM-DD`.  
> Date-times generally use ISO local date-time (`YYYY-MM-DDTHH:mm`) and are interpreted in the active calendar timezone unless zone/offset is provided.

### Calendar management

```text
create calendar --name <name> --timezone <Area/Location>
use calendar --name <name>
edit calendar --name <name> --property name <newName>
edit calendar --name <name> --property timezone <Area/Location>
```

### Event creation

```text
create event "<subject>" from <startDateTime> to <endDateTime>
create event "<subject>" on <date>

create event "<subject>" from <startDateTime> to <endDateTime> repeats <dayCodes> for <N> times
create event "<subject>" from <startDateTime> to <endDateTime> repeats <dayCodes> until <date>

create event "<subject>" on <date> repeats <dayCodes> for <N> times
create event "<subject>" on <date> repeats <dayCodes> until <date>
```

Day codes supported by the parser include combinations of `M T W R F S U` (e.g. `MWR`, `T,R`, `M W F`).

### Event edit

```text
edit event <property> <subject> from <startDateTime> to <endDateTime> with <value>
edit events <property> <subject> from <startDateTime> with <value>
edit series <property> <subject> from <startDateTime> with <value>
```

### Copy

```text
copy event <subject> on <startDateTime> --target <calendarName> to <targetStartDateTime>
copy events on <sourceDate> --target <calendarName> to <targetDate>
copy events between <sourceStartDate> and <sourceEndDate> --target <calendarName> to <targetDate>
```

### Query + status

```text
print events on <date>
print events from <startDateTimeWithZone> to <endDateTimeWithZone>
show status on <dateTimeWithZone>
```

### Export

```text
export cal <output.csv>
export cal <output.ical>
export cal <output.ics>
```

### Dashboard analytics

```text
show calendar dashboard from <date> to <date>
```

This prints:
- total events
- totals by subject
- totals by weekday
- totals by week
- totals by month
- average events/day
- busiest + least busy day
- online vs non-online percentage

---

## GUI Notes

The Swing UI provides:
- Month navigation and timezone-aware header.
- Calendar switching/creation.
- Create/Edit dialogs for single + recurring events.
- Day-cell quick actions.
- Dashboard dialog for analytics over a selected interval.

The GUI mutates state via `GuiController` and reads data from `CalendarReadOnly`, keeping UI and business logic decoupled.

---

## Development

### Run tests

```bash
./gradlew test
```

### Checkstyle

```bash
./gradlew checkstyleMain checkstyleTest
```

### Coverage report

```bash
./gradlew jacocoTestReport
```

### Mutation testing (optional)

```bash
./gradlew pitest
```

---

## Known Notes

- If headless input ends without `exit`, the app prints an error and exits.
- `print events from ... to ...` and `show status on ...` expect zoned date-times when entered directly in CLI.
- For GUI launch, ensure a desktop-capable Java runtime is available in your environment.
