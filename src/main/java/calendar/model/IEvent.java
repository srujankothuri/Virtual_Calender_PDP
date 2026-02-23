package calendar.model;

import java.time.ZonedDateTime;

// CHECKSTYLE:OFF: AbbreviationAsWordInName
/**
 * Read-only view of an event.
 */
public interface IEvent extends Comparable<IEvent> {

  /**
   * Subject or title of the event.
   *
   * @return subject/title
   */
  String subject();

  /**
   * Start time of the event.
   *
   * @return start time
   */
  ZonedDateTime start();

  /**
   * End time of the event.
   *
   * @return end time
   */
  ZonedDateTime end();

  /**
   * Description text, which may be empty.
   *
   * @return description text (may be empty)
   */
  String description();

  /**
   * Location of the event, or an empty string if none.
   *
   * @return optional location or empty string
   */
  String location();

  /**
   * Status label for the event.
   *
   * @return status label, e.g., PUBLIC or PRIVATE
   */
  String status();

  /** Compare by start time then subject for stable ordering. */
  @Override
  default int compareTo(final IEvent other) {
    int t = this.start().compareTo(other.start());
    return (t != 0) ? t : this.subject().compareToIgnoreCase(other.subject());
  }
}
// CHECKSTYLE:ON: AbbreviationAsWordInName
