package timefit.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Immutable saved schedule. Duration is in minutes; all-day duration is evaluated from current settings.
 * @param id stable identity
 * @param title nonblank title
 * @param date scheduled day
 * @param category activity category
 * @param importance user priority
 * @param status saved progress state
 * @param kind normal or all-day
 * @param durationMinutes stored normal duration (zero for all-day)
 * @param start optional start time
 * @param end optional end time
 */
public record Schedule(long id, String title, LocalDate date, String category,
        Importance importance, Status status, Kind kind, int durationMinutes,
        LocalTime start, LocalTime end) {
    public enum Kind { NORMAL, ALL_DAY }
    public enum Importance { LOW, NORMAL, HIGH }
    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }
    public Schedule {
        Objects.requireNonNull(title); Objects.requireNonNull(date); Objects.requireNonNull(category);
        Objects.requireNonNull(importance); Objects.requireNonNull(status); Objects.requireNonNull(kind);
        if (id < 1 || title.isBlank()) throw new IllegalArgumentException("제목과 일정 ID를 확인하세요.");
        if (kind == Kind.ALL_DAY) {
            if (start != null || end != null || durationMinutes != 0) throw new IllegalArgumentException("하루종일은 시간 입력을 사용하지 않아요.");
        } else {
            if (durationMinutes <= 0 || (start == null) != (end == null)) throw new IllegalArgumentException("시간은 양수이며 시작과 종료를 함께 입력해야 해요.");
            if (start != null && (!start.isBefore(end) || java.time.Duration.between(start, end).toMinutes() != durationMinutes))
                throw new IllegalArgumentException("시작·종료 시간과 소요 시간이 맞지 않아요.");
        }
    }
    /** Returns the effective duration under the current daily limit. */
    public int effectiveMinutes(UserSettings settings) { return kind == Kind.ALL_DAY ? settings.dailyLimitMinutes() : durationMinutes; }
}
