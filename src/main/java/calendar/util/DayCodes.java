package calendar.util;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Locale;

/**
 * Utilities for parsing weekday selections from user input.
 *
 * <p>Accepted forms include:
 * <ul>
 *   <li>Compact codes: {@code MTWRFSU}, {@code MTWR}, {@code MF}</li>
 *   <li>Separated codes: {@code M T W R F}, {@code M, W, F}</li>
 *   <li>Names/abbreviations: {@code Mon Tue Thu}, {@code Monday, Wed, Fri}</li>
 * </ul>
 *
 * <p>Code mapping follows the common convention:
 * M=Mon, T=Tue, W=Wed, R=Thu, F=Fri, S=Sat, U=Sun.
 */
public final class DayCodes {

  /**
   * No instances.
   */
  private DayCodes() {}

  /**
   * Parse a weekday selection into an {@link EnumSet} of {@link DayOfWeek}.
   *
   * <p>The parser accepts both compact codes (e.g., {@code MTWR}) and tokens
   * separated by commas or whitespace (e.g., {@code M, W, F} or
   * {@code Mon Wed Fri}). Names may be full or abbreviated.
   *
   * @param input raw user input
   * @return set of selected days (never {@code null})
   * @throws IllegalArgumentException if input is blank or contains an unknown code
   */
  public static EnumSet<DayOfWeek> parse(final String input) {
    if (input == null) {
      throw new IllegalArgumentException("Select at least one weekday.");
    }

    final String trimmed = input.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Select at least one weekday.");
    }

    final EnumSet<DayOfWeek> out = EnumSet.noneOf(DayOfWeek.class);

    // Case 1: compact code like "MTWRFSU" (only letters from the code alphabet)
    final String compact = trimmed.replace(",", "").replace(" ", "");
    if (compact.matches("(?i)^[MTWRFSU]+$")) {
      for (int i = 0; i < compact.length(); i += 1) {
        final char ch = Character.toUpperCase(compact.charAt(i));
        addCodeChar(out, ch);
      }
    } else {
      // Case 2: tokenized by commas/whitespace; accept codes and names.
      final String normalized = trimmed.replace(',', ' ');
      final String[] tokens = normalized.split("\\s+");
      for (final String tok : tokens) {
        if (tok.isEmpty()) {
          continue;
        }
        addWord(out, tok);
      }
    }

    if (out.isEmpty()) {
      throw new IllegalArgumentException("Select at least one weekday.");
    }

    return out;
  }

  /**
   * Add a single-letter compact code to the set.
   *
   * @param out destination set
   * @param ch  code character (upper-case expected)
   */
  private static void addCodeChar(final EnumSet<DayOfWeek> out, final char ch) {
    switch (ch) {
      case 'M':
        out.add(DayOfWeek.MONDAY);
        break;
      case 'T':
        out.add(DayOfWeek.TUESDAY);
        break;
      case 'W':
        out.add(DayOfWeek.WEDNESDAY);
        break;
      case 'R':
        out.add(DayOfWeek.THURSDAY);
        break;
      case 'F':
        out.add(DayOfWeek.FRIDAY);
        break;
      case 'S':
        out.add(DayOfWeek.SATURDAY);
        break;
      case 'U':
        out.add(DayOfWeek.SUNDAY);
        break;
      default:
        throw new IllegalArgumentException("Unknown day code: " + ch);
    }
  }

  /**
   * Add a token that may be a code or a weekday name/abbreviation.
   *
   * @param out destination set
   * @param token user token (any case)
   */
  private static void addWord(final EnumSet<DayOfWeek> out, final String token) {
    final String up = token.toUpperCase(Locale.ROOT);

    // Single-letter compact codes.
    if (up.length() == 1 && "MTWRFSU".indexOf(up.charAt(0)) >= 0) {
      addCodeChar(out, up.charAt(0));
      return;
    }

    // Names / common abbreviations.
    // We allow 2-full length: MO/MON/MONDAY, TU/TUE/TUESDAY, etc.
    if (up.startsWith("MO")) {
      out.add(DayOfWeek.MONDAY);
      return;
    }
    if (up.startsWith("TU") || up.startsWith("TUE")) {
      out.add(DayOfWeek.TUESDAY);
      return;
    }
    if (up.startsWith("WE") || up.startsWith("WED")) {
      out.add(DayOfWeek.WEDNESDAY);
      return;
    }
    if (up.startsWith("TH") || up.startsWith("THU")) {
      out.add(DayOfWeek.THURSDAY);
      return;
    }
    if (up.startsWith("FR") || up.startsWith("FRI")) {
      out.add(DayOfWeek.FRIDAY);
      return;
    }
    if (up.startsWith("SA") || up.startsWith("SAT")) {
      out.add(DayOfWeek.SATURDAY);
      return;
    }
    if (up.startsWith("SU") || up.startsWith("SUN")) {
      out.add(DayOfWeek.SUNDAY);
      return;
    }

    throw new IllegalArgumentException("Unknown day code: " + token);
  }
}
