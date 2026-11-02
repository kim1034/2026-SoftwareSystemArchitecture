package timefit.service;

import timefit.model.*;
import timefit.storage.StateStore;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Personal settings transactions. Existing normal durations are never rewritten. */
public final class SettingsService {
    private final StateStore store;
    public SettingsService(StateStore store) { this.store = store; }
    public UserSettings current() { return store.data().settings(); }
    /** Validates, persists and applies settings; all-day totals use the new limit automatically.
     * @param dailyLimitMinutes capacity in 1..1440 minutes
     * @param defaultDurationMinutes positive fallback duration, may exceed capacity
     * @return dates that exceed the new limit, without deleting schedules
     * @throws IOException write failure, preserving old settings
     */
    public List<LocalDate> updateTimeSettings(int dailyLimitMinutes, int defaultDurationMinutes) throws IOException {
        UserSettings settings = new UserSettings(dailyLimitMinutes, defaultDurationMinutes);
        Map<LocalDate, Long> totals = new TreeMap<>();
        store.data().schedules().forEach(s -> totals.merge(s.date(), (long) s.effectiveMinutes(settings), Long::sum));
        store.commit(new AppData(settings, store.data().schedules(), store.data().nextId()));
        return totals.entrySet().stream().filter(e -> e.getValue() > dailyLimitMinutes).map(Map.Entry::getKey).toList();
    }
}
