# USEME

Virtual Calendar — Run & Evaluate Guide
=======================================

This guide shows you how to **build**, **run**, and **grade** the calendar app in both *interactive* and *headless* modes, plus how to **launch the Swing GUI**, and how to **import the exported CSV into Google Calendar** for the required screenshot.

> **Java version:** 11  
> **CLI Main class:** `calendar.CalendarRunner`  
> **GUI Main class:** `calendar.gui.SwingCalendarApp`  
> **CLI modes:** `--mode interactive` or `--mode headless <script-file>`  
> **Default time zone:** The assignment assumes **EST**. Set your Google Calendar to EST before taking screenshots.

---

## 1) Build the JAR

From the project root:

```bash
./gradlew jar
```

The JAR will be written to:

```
build/libs/<JAR-NAME>.jar
```

(Example: `build/libs/calendar-1.0.jar` — your exact name may differ.)

---

## 2) Run in Interactive Mode (CLI)

Interactive mode reads commands from the terminal until you type `exit`.

```bash
java -jar build/libs/<JAR-NAME>.jar --mode interactive
```

Then type commands like these (one per line):

```
create event "AI Class" from 2025-11-03T10:00 to 2025-11-03T11:15
create event "Flight to NYC" on 2025-11-24
create series "Study" from 2025-11-05T09:00 to 2025-11-05T10:00 repeats mon,wed for 3
export cal res/hw4_calendar.csv
exit
```

If `res/` does not exist, create it first so the export path works:

```bash
mkdir -p res
```

---

## 3) Run in Headless Mode (CLI) — *Recommended for grading*

Prepare a script file (example: `res/commands.txt`) with one command per line:

```
create event "AI Class" from 2025-11-03T10:00 to 2025-11-03T11:15
create event "Flight to NYC" on 2025-11-24
create series "Study" from 2025-11-05T09:00 to 2025-11-05T10:00 repeats mon,wed for 3
export cal res/hw4_calendar.csv
exit
```

Run the app with that script:

```bash
java -jar build/libs/<JAR-NAME>.jar --mode headless res/commands.txt
```

Expected console output includes messages like `Created.`, `Series created (...)`, `Edited (...)`, `Exported ...`, and *(script ended without 'exit')* if the script intentionally does not end with `exit`.

> The program exits with code **2** and prints usage if `--mode` is missing, and it exits with code **2** for invalid headless paths (as per spec).

---

## 4) Run the **Swing GUI**

There are two typical ways to launch the GUI, depending on how your JAR manifest is configured:

### A) Run by class name on the classpath (works regardless of manifest)
```bash
java -cp build/libs/<JAR-NAME>.jar calendar.gui.SwingCalendarApp
```

### B) If your JAR manifest sets `Main-Class: calendar.gui.SwingCalendarApp`
```bash
java -jar build/libs/<JAR-NAME>.jar
```

### What to verify (GUI behaviors)
- **Month header shows time zone** (e.g., `Nov 2025 (America/New_York)`), and Prev/Next buttons page months correctly.
- **Left-click day actions:** a small chooser appears: **Edit… / Create…** (or Create if the day is empty).
- **Create dialog** accepts **subject**, optional **location**, and (for timed events) start/end times.
- **Edit dialog** allows editing **subject** and **location** for: single instance, following-by-subject, or entire series.
- **Time-anchored single edits:** the GUI resolves the exact instance (start/end) before delegating to the controller.
- **Move date/time (single instance)** uses explicit controller signatures, preserving duration for time moves.
- **Quick pickers:** Edit from the toolbar can prompt for a day (pre-selects the last clicked day), load that day’s subjects into a combo box, and echo the weekday next to the date (e.g., `2025-11-12 (WED)`).

> The GUI talks only to the `GuiController` adapter and reads data via `CalendarReadOnly`/`IEvent`, preserving MVC and separation of concerns.

---

## 5) Import the CSV into Google Calendar

1. Go to **https://calendar.google.com** and sign in.
2. (Optional but recommended) Create a new calendar to keep imports separate:  
   **Left sidebar → Other calendars → + → Create new calendar** (e.g., “HW4 Calendar”).
3. **Settings (gear) → Import & export → Import**.
4. Choose the exported file `res/hw4_calendar.csv`.
5. Under “Add to calendar”, pick **HW4 Calendar** (or your target calendar).
6. Click **Import** and confirm the number of events imported.

> If times look off, set Google Calendar’s time zone to **Eastern Time (US & Canada)** (EST) and re-check.

---

## 6) Required `res/` Folder Contents

Place the following in `res/` before submitting:

- `google-calendar-screenshot.png` — Screenshot showing **your imported events** in Google Calendar.
    - Use *Week* view and ensure only your calendar is toggled on.
- `commands.txt` — The headless script you used (valid commands).
- `invalid.txt` — A few invalid commands that exercise error handling, e.g.:
  ```
  create event "Missing To" from 2025-11-03T10:00
  create series "Both" from 2025-11-05T09:00 to 2025-11-05T10:00 repeats thu for 2 until 2025-11-30
  nonsense command here
  ```
- `class-diagram.md` — Class diagram (Mermaid) describing architecture (see next file).
- (Optional) `hw4_calendar.csv` — The CSV you imported into Google Calendar.

---

## 7) Troubleshooting

- **“Not a file” in headless mode** — Make sure the third argument is a path to an actual file (not a directory), e.g. `res/commands.txt`.
- **Usage printed & exit code 2** — You likely missed `--mode` or provided an unsupported mode value.
- **CSV doesn’t import** — Ensure your exporter writes a Google-Calendar-compatible header and values. Re-run export and try import again.
- **Duplicate events** — Delete the test calendar you created and re-import into a fresh calendar.
- **GUI won’t launch** — Use the classpath form (`-cp … calendar.gui.SwingCalendarApp`) if the manifest’s `Main-Class` is set to the CLI runner. If you want `java -jar` to open the GUI by default, rebuild the JAR with the GUI main in the manifest.

---

## 8) Developer Notes

- **CLI Main:** `calendar.CalendarRunner`
- **GUI Main:** `calendar.gui.SwingCalendarApp`
- **GUI Controller Port:** `calendar.controller.GuiController` (adapter used by Swing)
- **Model (read side exposed to GUI):** `calendar.view.CalendarReadOnly` + `calendar.model.IEvent`
- **CLI View:** `calendar.view.ConsoleView`
- **Domain/Services:** `calendar.model.CalendarService`, utilities like `calendar.util.DateTimes`

The GUI remains thin (layout + input) and delegates **all** mutations through the `GuiController`. The read path is adapter/facade-style via `CalendarReadOnly`, keeping Swing safe and testable.

---

## 9) Quick Reference (copy/paste)

```bash
# Build
./gradlew jar

# CLI interactive
java -jar build/libs/<JAR-NAME>.jar --mode interactive

# CLI headless
java -jar build/libs/<JAR-NAME>.jar --mode headless res/commands.txt

# GUI (classpath form)
java -cp build/libs/<JAR-NAME>.jar calendar.gui.SwingCalendarApp
```
