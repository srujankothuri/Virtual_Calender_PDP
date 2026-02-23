package calendar.model;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Package-private immutable event implementation.
 * Built using {@link Builder}; exposed to clients via {@link IEvent}.
 */
final class Event implements IEvent {

  private final String subject;
  private final ZonedDateTime start;
  private final ZonedDateTime end;
  private final String description;
  private final String location;
  private final String status;

  /**
   * Builder for {@link Event}.
   */
  static final class Builder {
    private String subject;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private String description = "";
    private String location = "";
    private String status = "PUBLIC";

    Builder subject(final String subject) {
      this.subject = subject;
      return this;
    }

    Builder start(final ZonedDateTime start) {
      this.start = start;
      return this;
    }

    Builder end(final ZonedDateTime end) {
      this.end = end;
      return this;
    }

    Builder description(final String description) {
      this.description = (description == null) ? "" : description;
      return this;
    }

    Builder location(final String location) {
      this.location = (location == null) ? "" : location;
      return this;
    }

    Builder status(final String status) {
      this.status = (status == null) ? "PUBLIC" : status;
      return this;
    }

    Event build() {
      if (subject == null || subject.isBlank()) {
        throw new IllegalArgumentException("subject required");
      }
      if (start == null || end == null) {
        throw new IllegalArgumentException("start/end required");
      }
      if (end.isBefore(start) || end.equals(start)) {
        throw new IllegalArgumentException("end must be after start");
      }
      return new Event(this);
    }
  }

  private Event(final Builder b) {
    this.subject = b.subject;
    this.start = b.start;
    this.end = b.end;
    this.description = b.description;
    this.location = b.location;
    this.status = b.status;
  }

  @Override
  public String subject() {
    return subject;
  }

  @Override
  public ZonedDateTime start() {
    return start;
  }

  @Override
  public ZonedDateTime end() {
    return end;
  }

  @Override
  public String description() {
    return description;
  }

  @Override
  public String location() {
    return location;
  }

  @Override
  public String status() {
    return status;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Event)) {
      return false;
    }
    Event other = (Event) o;
    return subject.equals(other.subject) && start.equals(other.start);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, start);
  }

  @Override
  public String toString() {
    return "Event{" + subject + "@" + start + "" + end + "}";
  }
}
