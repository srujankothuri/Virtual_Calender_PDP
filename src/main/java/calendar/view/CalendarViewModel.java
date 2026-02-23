package calendar.view;

import calendar.model.CalendarModel;
import calendar.model.IEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Adapter that exposes a read-only facade of {@link CalendarModel} to Views.
 * This prevents views from mutating the model directly (DIP-friendly).
 */
public final class CalendarViewModel implements CalendarReadOnly {

  private final CalendarModel model;

  /**
   * Create a view model around a {@link CalendarModel}.
   *
   * @param model the underlying model (not null)
   */
  public CalendarViewModel(final CalendarModel model) {
    this.model = Objects.requireNonNull(model);
  }

  @Override
  public String currentCalendarName() {
    return model.current();
  }

  @Override
  public ZoneId currentZone() {
    return model.currentZone();
  }

  @Override
  public List<IEvent> eventsOn(final LocalDate day) {
    return Collections.unmodifiableList(model.eventsOn(day));
  }
}
