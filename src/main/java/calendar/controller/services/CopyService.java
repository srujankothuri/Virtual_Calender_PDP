package calendar.controller.services;

import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service for copying events between calendars.
 * After copy operations, the current calendar remains the SOURCE calendar.
 */
public final class CopyService {

  private final CalendarModel model;

  /**
   * Construct with a calendar model.
   *
   * @param model the calendar model
   */
  public CopyService(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
  }

  /**
   * Copy a single event to another calendar at a new time.
   *
   * @param subject the event subject
   * @param sourceStart the source event start time
   * @param targetCalendar the target calendar name
   * @param targetStart the new start time in target calendar
   * @return the copied event
   */
  public IEvent copySingleEvent(final String subject,
                                final ZonedDateTime sourceStart,
                                final String targetCalendar,
                                final ZonedDateTime targetStart) {
    // Save current calendar to restore later
    final String currentCal = model.current();

    // Find the source event in current calendar
    final IEvent source = model.findBySubjectAt(subject, sourceStart)
        .orElseThrow(() -> new IllegalArgumentException(
            "No event '" + subject + "' at " + sourceStart));

    // Calculate event duration
    final Duration duration = Duration.between(source.start(), source.end());

    try {
      // Switch to target calendar (will throw if target doesn't exist)
      model.useCalendar(targetCalendar);

      // Create the event copy in target calendar
      model.createSingle(source.subject(),
          targetStart,
          targetStart.plus(duration),
          source.description(),
          source.location(),
          source.status());

      // Return the newly created event
      return model.findBySubjectAt(source.subject(), targetStart)
          .orElseThrow(() -> new IllegalStateException("Failed to copy event"));

    } finally {
      // Always restore original calendar
      model.useCalendar(currentCal);
    }
  }

  /**
   * Copy all events on a specific date to another calendar.
   *
   * @param sourceDate the source date
   * @param targetCalendar the target calendar name
   * @param targetDate the target date
   * @return list of copied events
   */
  public List<IEvent> copyEventsOnDate(final LocalDate sourceDate,
                                       final String targetCalendar,
                                       final LocalDate targetDate) {
    // Save current calendar to restore later
    final String currentCal = model.current();

    // Get all events on the source date from current calendar
    final List<IEvent> sourceEvents = model.eventsOn(sourceDate);

    try {
      // Switch to target calendar (will throw if target doesn't exist)
      // This must happen even if there are no events to copy, to validate target exists
      model.useCalendar(targetCalendar);

      if (sourceEvents.isEmpty()) {
        return new ArrayList<>();
      }

      final ZoneId targetZone = model.currentZone();
      final List<IEvent> copiedEvents = new ArrayList<>();

      // Copy each event
      for (IEvent event : sourceEvents) {
        // Adjust the time to the target date and timezone
        final ZonedDateTime targetStart = adjustToTargetDate(
            event.start(), targetDate, targetZone);

        final Duration duration = Duration.between(event.start(), event.end());

        // Create the event in target calendar
        model.createSingle(event.subject(),
            targetStart,
            targetStart.plus(duration),
            event.description(),
            event.location(),
            event.status());

        // Add to result list
        final IEvent copiedEvent = model.findBySubjectAt(event.subject(), targetStart)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to copy event: " + event.subject()));
        copiedEvents.add(copiedEvent);
      }

      return copiedEvents;

    } finally {
      // Always restore original calendar
      model.useCalendar(currentCal);
    }
  }

  /**
   * Copy events between two dates to another calendar.
   * Per README: "The date string in the target calendar corresponds to the start of the interval."
   * This means targetStartDate maps to fromDate, preserving relative spacing.
   *
   * @param fromDate the start date (inclusive)
   * @param toDate the end date (inclusive)
   * @param targetCalendar the target calendar name
   * @param targetStartDate the starting date in target calendar
   * @return list of copied events
   */
  public List<IEvent> copyEventsBetween(final LocalDate fromDate,
                                        final LocalDate toDate,
                                        final String targetCalendar,
                                        final LocalDate targetStartDate) {
    // Save current calendar to restore later
    final String currentCal = model.current();

    // Get all events in the date range from current calendar
    final List<IEvent> sourceEvents = model.eventsBetween(fromDate, toDate);

    try {
      // Switch to target calendar (will throw if target doesn't exist)
      // This must happen even if there are no events to copy, to validate target exists
      model.useCalendar(targetCalendar);

      if (sourceEvents.isEmpty()) {
        return new ArrayList<>();
      }

      final ZoneId targetZone = model.currentZone();
      final List<IEvent> copiedEvents = new ArrayList<>();

      // Per README: targetStartDate corresponds to fromDate (start of the interval)
      // Copy each event, preserving relative date spacing from fromDate

      for (IEvent event : sourceEvents) {
        final LocalDate eventDate = event.start().toLocalDate();

        // Calculate days offset from the START of the interval (fromDate)
        final long daysFromStart = java.time.temporal.ChronoUnit.DAYS.between(
            fromDate, eventDate);

        // Apply same offset to target start date
        final LocalDate targetDate = targetStartDate.plusDays(daysFromStart);

        // Adjust the time to the target date and timezone
        final ZonedDateTime targetStart = adjustToTargetDate(
            event.start(), targetDate, targetZone);

        final Duration duration = Duration.between(event.start(), event.end());

        // Create the event in target calendar
        model.createSingle(event.subject(),
            targetStart,
            targetStart.plus(duration),
            event.description(),
            event.location(),
            event.status());

        // Add to result list
        final IEvent copiedEvent = model.findBySubjectAt(event.subject(), targetStart)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to copy event: " + event.subject()));
        copiedEvents.add(copiedEvent);
      }

      return copiedEvents;

    } finally {
      // Always restore original calendar
      model.useCalendar(currentCal);
    }
  }

  /**
   * Helper method to adjust a datetime to a target date and timezone.
   * Preserves the time of day but changes the date and timezone.
   */
  private ZonedDateTime adjustToTargetDate(final ZonedDateTime source,
                                           final LocalDate targetDate,
                                           final ZoneId targetZone) {
    return ZonedDateTime.of(
        targetDate.getYear(),
        targetDate.getMonthValue(),
        targetDate.getDayOfMonth(),
        source.getHour(),
        source.getMinute(),
        source.getSecond(),
        source.getNano(),
        targetZone);
  }
}