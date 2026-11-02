package timefit.storage;

import java.io.IOException;
import timefit.model.AppData;

/** In-memory state, changed only after successful disk persistence. Application services run on Swing's event thread. */
public final class StateStore {
    private final FileRepository repository;
    private AppData data;
    public StateStore(FileRepository repository) throws IOException { this.repository = repository; data = repository.loadData(); }
    public AppData data() { return data; }
    public FileRepository repository() { return repository; }
    /** @param next candidate snapshot
     * @throws IOException persistence failure, leaving memory unchanged
     */
    public void commit(AppData next) throws IOException { repository.saveData(next); data = next; }
}
