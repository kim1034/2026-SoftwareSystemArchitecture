package timefit.model;

import java.util.List;
import java.util.Objects;

/** Complete immutable snapshot written in one transaction.
 * @param settings personal settings
 * @param schedules saved schedules
 * @param nextId next unused schedule ID
 */
public record AppData(UserSettings settings, List<Schedule> schedules, long nextId) {
    public AppData {
        Objects.requireNonNull(settings); schedules = List.copyOf(schedules);
        if (nextId < 1 || schedules.stream().map(Schedule::id).distinct().count() != schedules.size()
                || schedules.stream().anyMatch(s -> s.id() >= nextId)) throw new IllegalArgumentException("일정 ID 데이터가 올바르지 않아요.");
    }
    public static AppData empty() { return new AppData(UserSettings.defaults(), List.of(), 1); }
}
