package timefit.service;

import timefit.model.*;
import timefit.model.Schedule.Kind;
import timefit.storage.StateStore;
import java.time.*;
import java.util.*;

/** Daily capacity checks and deterministic date recommendations. This service never writes data. */
public final class PlanningService {
    private final StateStore store;
    private final Clock clock;
    public PlanningService(StateStore store, Clock clock) { this.store = store; this.clock = clock; }
    public Clock clock() { return clock; }
    /** Checks capacity, all-day exclusivity and strict interval overlap, including completed schedules.
     * @param candidate proposed schedule
     * @param excludedScheduleId existing ID excluded for edits and replanning, or null for a new schedule
     * @return all detected conflict reasons and resulting totals
     */
    public ConflictResult checkConflicts(Schedule candidate, Long excludedScheduleId) {
        AppData d = store.data();
        List<Schedule> others = d.schedules().stream().filter(s -> s.date().equals(candidate.date())
                && (excludedScheduleId == null || s.id() != excludedScheduleId)).toList();
        long total = others.stream().mapToLong(s -> s.effectiveMinutes(d.settings())).sum() + candidate.effectiveMinutes(d.settings());
        List<String> reasons = new ArrayList<>(); List<Long> overlaps = new ArrayList<>();
        if (total > d.settings().dailyLimitMinutes()) reasons.add("하루 최대 시간을 " + (total - d.settings().dailyLimitMinutes()) + "분 초과해요.");
        if (!others.isEmpty() && (candidate.kind() == Kind.ALL_DAY || others.stream().anyMatch(s -> s.kind() == Kind.ALL_DAY)))
            reasons.add("하루종일 일정은 다른 일정과 같은 날에 둘 수 없어요.");
        if (candidate.start() != null) for (Schedule s : others) {
            if (s.start() != null && candidate.start().isBefore(s.end()) && s.start().isBefore(candidate.end())) overlaps.add(s.id());
        }
        if (!overlaps.isEmpty()) reasons.add("시간이 겹치는 일정 ID: " + overlaps);
        return new ConflictResult(total, d.settings().dailyLimitMinutes() - total, overlaps, reasons);
    }
    /** Generates feasible dates from today to the day before a future deadline; today's deadline includes today.
     * @param candidate normalized request whose date will be replaced for each candidate
     * @param deadline recommendation deadline
     * @param excludedScheduleId own ID for update/replan, or null
     * @return all feasible dates sorted by resulting load, weekend tie break, then date
     * @throws IllegalArgumentException past or missing deadline
     */
    public List<Recommendation> recommendDates(Schedule candidate, LocalDate deadline, Long excludedScheduleId) {
        LocalDate today = LocalDate.now(clock);
        if (deadline == null || deadline.isBefore(today)) throw new IllegalArgumentException("마감일은 오늘 이후로 입력하세요.");
        LocalDate last = deadline.equals(today) ? today : deadline.minusDays(1);
        // Avoid a frozen UI from accidentally typing a very distant year. Input stays explicit instead of truncating silently.
        if (java.time.temporal.ChronoUnit.DAYS.between(today, last) > 3660)
            throw new IllegalArgumentException("추천 마감일은 오늘부터 10년 이내로 입력하세요.");
        List<Recommendation> list = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(last); day = day.plusDays(1)) {
            if (day.equals(today) && candidate.start() != null && candidate.start().isBefore(LocalTime.now(clock))) continue;
            Schedule c = new Schedule(candidate.id(), candidate.title(), day, candidate.category(), candidate.importance(), candidate.status(),
                    candidate.kind(), candidate.durationMinutes(), candidate.start(), candidate.end());
            ConflictResult result = checkConflicts(c, excludedScheduleId);
            if (result.allowed()) list.add(new Recommendation(day, result.totalMinutes(), result.remainingMinutes(),
                    "등록 후 총시간이 적은 순 · 동률이면 토·일 · 가까운 날"));
        }
        list.sort(Comparator.comparingLong(Recommendation::totalMinutes).thenComparing(r -> !isWeekend(r.date())).thenComparing(Recommendation::date));
        return List.copyOf(list);
    }
    public static boolean isWeekend(LocalDate day) { return day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY; }
    public long getDailySummary(LocalDate date) {
        return store.data().schedules().stream().filter(s -> s.date().equals(date)).mapToLong(s -> s.effectiveMinutes(store.data().settings())).sum();
    }
    public Map<LocalDate, Long> getMonthScheduleCounts(YearMonth month) {
        Map<LocalDate, Long> result = new HashMap<>();
        store.data().schedules().stream().filter(s -> YearMonth.from(s.date()).equals(month)).forEach(s -> result.merge(s.date(), 1L, Long::sum));
        return Map.copyOf(result);
    }
    public boolean isOverdue(Schedule s) {
        if (s.status() == Schedule.Status.COMPLETED) return false;
        LocalDate today = LocalDate.now(clock);
        return s.date().isBefore(today) || (s.date().equals(today) && s.end() != null && s.end().isBefore(LocalTime.now(clock)));
    }
}
