package timefit.storage;

import timefit.model.*;
import timefit.model.Schedule.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** UTF-8 XML properties repository. Writes a complete temporary snapshot and atomically replaces the old file.
 * No Java object deserialization or third-party libraries are used.
 */
public class FileRepository {
    private final Path path;
    /** @param path writable destination file */
    public FileRepository(Path path) { this.path = path.toAbsolutePath(); }
    public Path path() { return path; }
    /** Loads and validates a complete snapshot. Missing files start empty; corrupt files are never silently replaced.
     * @return immutable saved data
     * @throws IOException invalid or unreadable file
     */
    public AppData loadData() throws IOException {
        if (!Files.exists(path)) return AppData.empty();
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.loadFromXML(in); }
        try {
            if (!"1".equals(p.getProperty("version"))) throw new IllegalArgumentException("지원하지 않는 파일 버전");
            UserSettings settings = new UserSettings(Integer.parseInt(p.getProperty("limit")), Integer.parseInt(p.getProperty("default")));
            int count = Integer.parseInt(p.getProperty("count"));
            if (count < 0 || count > 100000) throw new IllegalArgumentException("일정 개수 오류");
            List<Schedule> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String k = "s." + i + ".";
                String start = p.getProperty(k + "start", ""), end = p.getProperty(k + "end", "");
                list.add(new Schedule(Long.parseLong(p.getProperty(k + "id")), p.getProperty(k + "title"), LocalDate.parse(p.getProperty(k + "date")),
                    p.getProperty(k + "category"), Importance.valueOf(p.getProperty(k + "importance")), Status.valueOf(p.getProperty(k + "status")),
                    Kind.valueOf(p.getProperty(k + "kind")), Integer.parseInt(p.getProperty(k + "duration")),
                    start.isEmpty() ? null : LocalTime.parse(start), end.isEmpty() ? null : LocalTime.parse(end)));
            }
            return new AppData(settings, list, Long.parseLong(p.getProperty("nextId")));
        } catch (RuntimeException e) { throw new IOException("저장 파일 형식이 올바르지 않아요. 원본을 보존했어요.", e); }
    }
    /** Persists the snapshot before callers commit their memory state.
     * @param data proposed complete state
     * @throws IOException failed write or unsupported atomic replacement; the original remains unchanged
     */
    public void saveData(AppData data) throws IOException {
        Properties p = new Properties();
        p.setProperty("version", "1"); p.setProperty("limit", "" + data.settings().dailyLimitMinutes());
        p.setProperty("default", "" + data.settings().defaultDurationMinutes()); p.setProperty("nextId", "" + data.nextId());
        p.setProperty("count", "" + data.schedules().size());
        for (int i = 0; i < data.schedules().size(); i++) {
            Schedule s = data.schedules().get(i); String k = "s." + i + ".";
            p.setProperty(k + "id", "" + s.id()); p.setProperty(k + "title", s.title()); p.setProperty(k + "date", s.date().toString());
            p.setProperty(k + "category", s.category()); p.setProperty(k + "importance", s.importance().name());
            p.setProperty(k + "status", s.status().name()); p.setProperty(k + "kind", s.kind().name());
            p.setProperty(k + "duration", "" + s.durationMinutes());
            p.setProperty(k + "start", s.start() == null ? "" : s.start().toString()); p.setProperty(k + "end", s.end() == null ? "" : s.end().toString());
        }
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "timefit-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { p.storeToXML(out, "TimeFit data v1", "UTF-8"); }
            try (FileChannel c = FileChannel.open(temp, StandardOpenOption.WRITE)) { c.force(true); }
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
