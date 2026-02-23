package calendar.controller.services;

import calendar.model.CalendarModel;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;


/**
 * Service for calendar management operations.
 * Place this in: calendar/controller/services/
 */
public final class CalendarService {

  private final CalendarModel model;

  /**
   * Construct with a calendar model.
   *
   * @param model the calendar model
   */
  public CalendarService(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
  }

  /**
   * Create a new calendar.
   *
   * @param name the calendar name
   * @param zone the timezone
   */
  public void create(final String name, final ZoneId zone) {
    model.createCalendar(name, zone);
  }

  /**
   * Rename an existing calendar.
   *
   * @param oldName the current name
   * @param newName the new name
   */
  public void rename(final String oldName, final String newName) {
    model.renameCalendar(oldName, newName);
  }

  /**
   * Set timezone for a calendar.
   *
   * @param name the calendar name
   * @param zone the new timezone
   */
  public void setTimezone(final String name, final ZoneId zone) {
    model.setTimezone(name, zone);
  }

  /**
   * Switch to using a specific calendar.
   *
   * @param name the calendar name to use
   */
  public void use(final String name) {
    model.useCalendar(name);
  }

  /**
   * Get the current calendar name.
   *
   * @return the current calendar name
   */
  public String getCurrentName() {
    return model.current();
  }

  /**
   * Get the current calendar's timezone.
   *
   * @return the current timezone
   */
  public ZoneId getCurrentZone() {
    return model.currentZone();
  }
  /**
   * Get the current calendar names.
   *
   * @return the calendar names
   */

  public List<String> getCalendarNames() {
    return model.getCalendarNames();
  }
}