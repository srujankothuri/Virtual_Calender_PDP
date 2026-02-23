package calendar.controller.services;

import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Service for event management operations.
 * Place this in: calendar/controller/services/
 */
public final class EventService {

  private final CalendarModel model;

  /**
   * Construct with a calendar model.
   *
   * @param model the calendar model
   */
  public EventService(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
  }

  // ========== EVENT CREATION ==========

  /**
   * Create a single event.
   *
   * @param subject the event subject
   * @param start the start time
   * @param end the end time
   * @param description the event description
   * @param location the event location
   * @param status the event status
   */
  public void createSingle(final String subject,
                           final ZonedDateTime start,
                           final ZonedDateTime end,
                           final String description,
                           final String location,
                           final String status) {
    model.createSingle(subject, start, end,
        description == null ? "" : description,
        location == null ? "" : location,
        status == null ? "PUBLIC" : status);
  }

  /**
   * Create an all-day event.
   *
   * @param subject the event subject
   * @param day the date
   */
  public void createAllDay(final String subject, final LocalDate day) {
    model.createAllDay(subject, day, "", "", "PUBLIC");
  }

  /**
   * Create an all-day event with full details.
   *
   * @param subject the event subject
   * @param day the date
   * @param description the event description
   * @param location the event location
   * @param status the event status
   */
  public void createAllDay(final String subject,
                           final LocalDate day,
                           final String description,
                           final String location,
                           final String status) {
    model.createAllDay(subject, day, description, location, status);
  }

  /**
   * Create a recurring series with times.
   *
   * @param subject the event subject
   * @param start the start time
   * @param end the end time
   * @param days the days of week
   * @param count the number of occurrences (null if using untilDate)
   * @param untilDate the end date (null if using count)
   * @param description the event description
   * @param location the event location
   * @param status the event status
   */
  public void createRecurringSeries(final String subject,
                                    final ZonedDateTime start,
                                    final ZonedDateTime end,
                                    final EnumSet<DayOfWeek> days,
                                    final Integer count,
                                    final LocalDate untilDate,
                                    final String description,
                                    final String location,
                                    final String status) {
    if (count != null) {
      model.createRecurringSeriesCount(subject, start, end, days, count,
          description == null ? "" : description,
          location == null ? "" : location,
          status == null ? "PUBLIC" : status);
    } else if (untilDate != null) {
      model.createRecurringSeriesUntil(subject, start, end, days, untilDate,
          description == null ? "" : description,
          location == null ? "" : location,
          status == null ? "PUBLIC" : status);
    } else {
      throw new IllegalArgumentException(
          "Either count or untilDate must be specified for recurring series");
    }
  }

  /**
   * Create a recurring all-day series.
   *
   * @param subject the event subject
   * @param startDate the start date
   * @param days the days of week
   * @param count the number of occurrences (null if using untilDate)
   * @param untilDate the end date (null if using count)
   * @param description the event description
   * @param location the event location
   * @param status the event status
   */
  public void createRecurringAllDaySeries(final String subject,
                                          final LocalDate startDate,
                                          final EnumSet<DayOfWeek> days,
                                          final Integer count,
                                          final LocalDate untilDate,
                                          final String description,
                                          final String location,
                                          final String status) {
    if (count != null) {
      model.createRecurringAllDaySeriesCount(subject, startDate, days, count,
          description == null ? "" : description,
          location == null ? "" : location,
          status == null ? "PUBLIC" : status);
    } else if (untilDate != null) {
      model.createRecurringAllDaySeriesUntil(subject, startDate, days, untilDate,
          description == null ? "" : description,
          location == null ? "" : location,
          status == null ? "PUBLIC" : status);
    } else {
      throw new IllegalArgumentException(
          "Either count or untilDate must be specified for recurring all-day series");
    }
  }

  // ========== EVENT EDITING ==========

  /**
   * Edit a single event instance.
   *
   * @param property the property to edit
   * @param subject the event subject
   * @param start the event start time
   * @param value the new value
   */
  public void editSingleInstance(final String property,
                                 final String subject,
                                 final ZonedDateTime start,
                                 final String value) {
    final IEvent event = model.findBySubjectAt(subject, start)
        .orElseThrow(() -> new IllegalArgumentException(
            "No event '" + subject + "' at " + start));

    // Calculate duration before deleting
    final Duration originalDuration = Duration.between(event.start(), event.end());

    // Delete the old event
    model.deleteBySubjectAt(subject, start);

    // Create new event with updated property
    switch (property.toLowerCase()) {
      case "subject":
        model.createSingle(value, event.start(), event.end(),
            event.description(), event.location(), event.status());
        break;
      case "start":
        final ZonedDateTime newStart = ZonedDateTime.parse(value);
        // Preserve duration when changing start time
        final ZonedDateTime newEnd = newStart.plus(originalDuration);
        model.createSingle(subject, newStart, newEnd,
            event.description(), event.location(), event.status());
        break;
      case "end":
        final ZonedDateTime newEndTime = ZonedDateTime.parse(value);
        model.createSingle(subject, event.start(), newEndTime,
            event.description(), event.location(), event.status());
        break;
      case "description":
        model.createSingle(subject, event.start(), event.end(),
            value, event.location(), event.status());
        break;
      case "location":
        model.createSingle(subject, event.start(), event.end(),
            event.description(), value, event.status());
        break;
      case "status":
        model.createSingle(subject, event.start(), event.end(),
            event.description(), event.location(), value);
        break;
      default:
        // Recreate the original event if unknown property
        model.createSingle(subject, event.start(), event.end(),
            event.description(), event.location(), event.status());
        throw new IllegalArgumentException("Unknown property: " + property);
    }
  }

  /**
   * Edit events from a specific time onwards.
   *
   * @param property the property to edit
   * @param subject the event subject
   * @param fromTime the starting time
   * @param value the new value
   */
  public void editEventsFrom(final String property,
                             final String subject,
                             final ZonedDateTime fromTime,
                             final String value) {
    final List<IEvent> events = model.findSeriesFrom(subject, fromTime);

    if (events.isEmpty()) {
      throw new IllegalArgumentException(
          "No events found for '" + subject + "' from " + fromTime);
    }

    for (IEvent event : events) {
      if (!event.start().isBefore(fromTime)) {
        try {
          editSingleInstance(property, event.subject(), event.start(), value);
        } catch (Exception e) {
          // Log error but continue with other events
          System.err.println("Failed to edit event at " + event.start() + ": " + e.getMessage());
        }
      }
    }
  }

  /**
   * Edit all events in a series.
   *
   * @param property the property to edit
   * @param subject the event subject
   * @param anyTime any time in the series
   * @param value the new value
   */
  public void editEntireSeries(final String property,
                               final String subject,
                               final ZonedDateTime anyTime,
                               final String value) {
    final List<IEvent> series = model.findEntireSeries(subject, anyTime);

    if (series.isEmpty()) {
      throw new IllegalArgumentException(
          "No series found for '" + subject + "' at " + anyTime);
    }

    for (IEvent event : series) {
      try {
        editSingleInstance(property, event.subject(), event.start(), value);
      } catch (Exception e) {
        // Log error but continue with other events
        System.err.println("Failed to edit event at " + event.start() + ": " + e.getMessage());
      }
    }
  }
}