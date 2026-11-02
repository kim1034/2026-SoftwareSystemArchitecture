package timefit.model;

/** Personal time settings, represented in whole minutes.
 * @param dailyLimitMinutes daily capacity in 1..1440
 * @param defaultDurationMinutes positive fallback duration, allowed to exceed the daily limit
 */
public record UserSettings(int dailyLimitMinutes, int defaultDurationMinutes) {
    public UserSettings {
        if (dailyLimitMinutes < 1 || dailyLimitMinutes > 1440 || defaultDurationMinutes < 1)
            throw new IllegalArgumentException("하루 최대 시간은 1~1440분, 기본 시간은 1분 이상이어야 해요.");
    }
    public static UserSettings defaults() { return new UserSettings(480, 120); }
}
