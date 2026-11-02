package timefit.model;

import java.time.LocalDate;
import java.time.LocalTime;
import timefit.model.Schedule.*;

/** Editable request. Null estimated duration means use the personal default.
 * @param title title
 * @param date scheduled date
 * @param category activity category
 * @param importance priority
 * @param status progress
 * @param kind time classification
 * @param estimatedMinutes optional estimated duration
 * @param start optional start
 * @param end optional end
 */
public record ScheduleInput(String title, LocalDate date, String category, Importance importance,
        Status status, Kind kind, Integer estimatedMinutes, LocalTime start, LocalTime end) { }
