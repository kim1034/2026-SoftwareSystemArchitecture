package timefit.model;

import java.util.List;

/** Non-mutating planning result. All reasons are preserved if multiple conflicts exist.
 * @param totalMinutes resulting daily total
 * @param remainingMinutes remaining capacity (negative means over capacity)
 * @param overlappingIds schedules with overlapping specified times
 * @param reasons validation reasons
 */
public record ConflictResult(long totalMinutes, long remainingMinutes, List<Long> overlappingIds, List<String> reasons) {
    public ConflictResult { overlappingIds = List.copyOf(overlappingIds); reasons = List.copyOf(reasons); }
    public boolean allowed() { return reasons.isEmpty(); }
}
