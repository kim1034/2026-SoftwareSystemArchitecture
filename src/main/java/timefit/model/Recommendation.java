package timefit.model;

import java.time.LocalDate;

/** A date candidate; selecting it never saves an activity.
 * @param date candidate date
 * @param totalMinutes total after registration
 * @param remainingMinutes remaining capacity
 * @param reason explanation of sorting rules
 */
public record Recommendation(LocalDate date, long totalMinutes, long remainingMinutes, String reason) { }
