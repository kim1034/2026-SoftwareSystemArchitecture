package timefit.service;

import timefit.model.*;
import timefit.model.Schedule.*;
import timefit.storage.StateStore;
import java.io.IOException;
import java.time.*;
import java.util.*;

/** Schedule CRUD and progress transactions. All save paths recheck planning constraints. */
public final class ScheduleService {
    private final StateStore store;
    private final PlanningService planning;
    public ScheduleService(StateStore store, PlanningService planning) { this.store = store; this.planning = planning; }
    public Schedule get(long id) {
        return store.data().schedules().stream().filter(s -> s.id() == id).findFirst().orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없어요."));
    }
    /** Validates input and resolves time precedence without saving.
     * @param input values from the editor
     * @param id own ID, or null for a new schedule
     * @return normalized schedule; new default above capacity becomes all-day
     */
    public Schedule prepare(ScheduleInput input, Long id) {
        if (input.title() == null || input.title().isBlank() || input.date() == null) throw new IllegalArgumentException("제목과 날짜를 입력하세요.");
        if (input.title().length() > 200) throw new IllegalArgumentException("제목은 200자 이내로 입력하세요.");
        if (input.kind() == null || input.importance() == null || input.status() == null || input.category() == null || input.category().isBlank())
            throw new IllegalArgumentException("일정 종류와 상태를 확인하세요.");
        Schedule old = id == null ? null : get(id);
        Kind kind = input.kind(); int duration = 0;
        LocalTime start = input.start(), end = input.end();
        if (kind == Kind.ALL_DAY) { start = null; end = null; }
        else {
            if ((start == null) != (end == null)) throw new IllegalArgumentException("시작과 종료 시간을 함께 입력하거나 모두 비우세요.");
            if (start != null) {
                if (!start.isBefore(end)) throw new IllegalArgumentException("시작 시간은 종료 시간보다 빨라야 해요. 자정 넘김은 지원하지 않아요.");
                duration = Math.toIntExact(Duration.between(start, end).toMinutes());
            } else if (input.estimatedMinutes() != null) duration = input.estimatedMinutes();
            else if (old != null && old.kind() == Kind.NORMAL) duration = old.durationMinutes();
            else {
                duration = store.data().settings().defaultDurationMinutes();
                if (id == null && duration > store.data().settings().dailyLimitMinutes()) { kind = Kind.ALL_DAY; duration = 0; }
            }
        }
        return new Schedule(id == null ? store.data().nextId() : id, input.title().trim(), input.date(), input.category().trim(),
                input.importance(), input.status(), kind, duration, start, end);
    }
    /** Saves a new or edited schedule after fresh validation and conflict checking.
     * @param input editor values
     * @param scheduleId null for creation or existing ID
     * @return persisted schedule
     * @throws IOException disk failure; original file and memory remain unchanged
     * @throws IllegalArgumentException invalid input or planning conflict
     */
    public Schedule saveSchedule(ScheduleInput input, Long scheduleId) throws IOException {
        Schedule candidate = prepare(input, scheduleId);
        ensureAllowed(candidate, scheduleId);
        List<Schedule> list = new ArrayList<>(store.data().schedules());
        if (scheduleId != null) list.removeIf(s -> s.id() == scheduleId);
        list.add(candidate);
        store.commit(new AppData(store.data().settings(), list, scheduleId == null ? Math.addExact(store.data().nextId(), 1) : store.data().nextId()));
        return candidate;
    }
    private void ensureAllowed(Schedule candidate, Long id) {
        ConflictResult conflict = planning.checkConflicts(candidate, id);
        if (!conflict.allowed()) throw new IllegalArgumentException(String.join("\n", conflict.reasons()));
    }
    public void deleteSchedule(long id) throws IOException {
        get(id); List<Schedule> list = new ArrayList<>(store.data().schedules()); list.removeIf(s -> s.id() == id);
        store.commit(new AppData(store.data().settings(), list, store.data().nextId()));
    }
    /** Changes only status, or moves an overdue schedule while preserving ID, kind and normal duration.
     * @param id existing ID
     * @param status target status for status-only operations
     * @param replanDate null for status-only, otherwise a new date; timed fields are cleared on replan
     * @return persisted updated schedule
     * @throws IOException failed persistence
     */
    public Schedule updateScheduleProgress(long id, Status status, LocalDate replanDate) throws IOException {
        Schedule old = get(id);
        if (replanDate != null && !planning.isOverdue(old)) throw new IllegalArgumentException("미완료 일정만 재계획할 수 있어요.");
        Schedule next = new Schedule(id, old.title(), replanDate == null ? old.date() : replanDate, old.category(), old.importance(),
                replanDate == null ? status : Status.PLANNED, old.kind(), old.durationMinutes(), replanDate == null ? old.start() : null, replanDate == null ? old.end() : null);
        if (replanDate != null) {
            if (replanDate.isBefore(LocalDate.now(planning.clock()))) throw new IllegalArgumentException("재계획 날짜는 오늘 이후로 선택하세요.");
            ensureAllowed(next, id);
        }
        List<Schedule> list = new ArrayList<>(store.data().schedules()); list.replaceAll(s -> s.id() == id ? next : s);
        store.commit(new AppData(store.data().settings(), list, store.data().nextId())); return next;
    }
    public List<Schedule> searchSchedules(String query, LocalDate date, String category, Importance importance, Status status) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return store.data().schedules().stream().filter(s -> s.title().toLowerCase(Locale.ROOT).contains(q))
                .filter(s -> date == null || s.date().equals(date)).filter(s -> category == null || s.category().equals(category))
                .filter(s -> importance == null || s.importance() == importance).filter(s -> status == null || s.status() == status)
                .sorted(Comparator.comparing(Schedule::date).thenComparing(s -> s.start() == null ? LocalTime.MAX : s.start()).thenComparingLong(Schedule::id)).toList();
    }
}
