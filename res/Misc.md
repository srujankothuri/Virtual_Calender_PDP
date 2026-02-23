# Misc

## 1) Design changes (with justifications)

- **GuiController Adapter (new)**  
  *What changed:* Introduced a `GuiController` interface as a thin, GUI-facing port. Swing code calls methods like `createTimedEvent(...)`, `editEventSubjectAtStart(...)`, `createRecurringByCount(...)`, etc.  
  *Why:* Keeps Swing strictly in the View role (no business rules, no parsing). The adapter translates GUI-friendly calls into backend operations (services/commands), which are easy to mock in tests.

- **Read-only Model Projection for the View (strengthened)**  
  *What changed:* The GUI now consumes `CalendarReadOnly` (and `IEvent`) as its data surface instead of concrete, mutable internals.  
  *Why:* This is an Adapter/Facade-style projection of the domain model that exposes only the queries the View needs (e.g., "events on day X," "current calendar," "time zone"), while hiding mutators. It prevents accidental mutation from the View and makes the UI far safer to evolve.

## 2) What works (feature checklist)

-  Create all-day and timed events (default to today or last clicked day).
-  Optional location on create; location displayed in month cells.
-  Create recurring (by count or until) using the existing controller ports.
-  Edit subject (single, following-by-subject, entire series).
-  Edit location (single, following-by-subject, entire series).
-  Move date (single via 4-arg API; series via day delta).
-  Move start time for single instance (duration preserved).
-  Calendar switching and timezone awareness.
-  Left-click day for quick actions; double-click still shows the textual day summary.
