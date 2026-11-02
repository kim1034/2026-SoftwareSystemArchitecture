package timefit;

import timefit.model.*;
import timefit.model.Schedule.*;
import timefit.service.*;
import timefit.storage.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Dependency-free executable regression suite for rules and persistence. Run with scripts/test.ps1 or test.sh. */
public final class CoreTests {
    private static int checks;
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"),ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY=LocalDate.now(CLOCK);
    private static final class Fixture {
        final StateStore store;final PlanningService planning;final ScheduleService schedules;final SettingsService settings;
        Fixture() throws Exception { this(new FileRepository(Files.createTempDirectory("timefit-test-").resolve("data.xml"))); }
        Fixture(FileRepository repo) throws Exception {store=new StateStore(repo);planning=new PlanningService(store,CLOCK);schedules=new ScheduleService(store,planning);settings=new SettingsService(store);}
    }
    @FunctionalInterface interface Action {void run() throws Exception;}
    private static void check(boolean result,String label){checks++;if(!result)throw new AssertionError(label);}
    private static void fails(Action action,String label) throws Exception {try{action.run();}catch(IllegalArgumentException|IOException e){check(true,label);return;}throw new AssertionError("Expected rejection: "+label);}
    private static ScheduleInput in(String title,LocalDate day,Integer minutes,LocalTime start,LocalTime end,Kind kind){return new ScheduleInput(title,day,"학습",Importance.NORMAL,Status.PLANNED,kind,minutes,start,end);}
    private static ScheduleInput in(LocalDate day,Integer minutes){return in("일정",day,minutes,null,null,Kind.NORMAL);}
    public static void main(String[] args) throws Exception {
        defaultsAndValidation();conflicts();recommendations();progress();persistence();
        System.out.println("PASS: "+checks+" assertions · time, conflicts, recommendations, progress and persistence");
    }
    private static void defaultsAndValidation() throws Exception {
        Fixture f=new Fixture();check(f.settings.current().equals(new UserSettings(480,120)),"initial settings");
        Schedule a=f.schedules.saveSchedule(in(TODAY,null),null);check(a.durationMinutes()==120,"fallback 2h");
        check(f.planning.getDailySummary(TODAY)==120,"day summary");
        f.settings.updateTimeSettings(480,60);check(f.schedules.get(a.id()).durationMinutes()==120,"default change keeps saved duration");
        Schedule b=f.schedules.saveSchedule(in(TODAY,null),null);check(b.durationMinutes()==60,"new default used");
        Schedule timed=f.schedules.prepare(in("시간 우선",TODAY,300,LocalTime.of(13,0),LocalTime.of(14,0),Kind.NORMAL),null);
        check(timed.durationMinutes()==60,"explicit times override estimate");
        fails(()->f.schedules.prepare(in("",TODAY,30,null,null,Kind.NORMAL),null),"blank title");
        fails(()->f.schedules.prepare(in("x",null,30,null,null,Kind.NORMAL),null),"missing date");
        fails(()->f.schedules.prepare(in(TODAY,0),null),"zero duration");fails(()->f.schedules.prepare(in(TODAY,-1),null),"negative duration");
        fails(()->f.schedules.prepare(in("x",TODAY,null,LocalTime.of(10,0),null,Kind.NORMAL),null),"one time");
        fails(()->f.schedules.prepare(in("x",TODAY,null,LocalTime.of(10,0),LocalTime.of(10,0),Kind.NORMAL),null),"equal time");
        fails(()->f.schedules.prepare(in("x",TODAY,null,LocalTime.of(23,0),LocalTime.of(1,0),Kind.NORMAL),null),"cross midnight");
        fails(()->f.settings.updateTimeSettings(0,120),"zero daily max");fails(()->f.settings.updateTimeSettings(1441,120),"daily max above 24h");
        fails(()->f.settings.updateTimeSettings(480,0),"zero default");
        f.settings.updateTimeSettings(480,600);Schedule auto=f.schedules.saveSchedule(in(TODAY.plusDays(1),null),null);
        check(auto.kind()==Kind.ALL_DAY&&auto.durationMinutes()==0,"large default auto all-day");
        check(auto.effectiveMinutes(f.settings.current())==480,"all-day effective limit");
        fails(()->f.schedules.saveSchedule(in(TODAY.plusDays(2),600),null),"explicit over-limit not auto converted");
        f.settings.updateTimeSettings(360,600);check(f.schedules.get(auto.id()).effectiveMinutes(f.settings.current())==360,"all-day follows changed limit");
        check(f.schedules.get(a.id()).durationMinutes()==120,"ordinary follows no limit change");
        Schedule manual=f.schedules.saveSchedule(in("직접 하루종일",TODAY.plusDays(2),25,LocalTime.NOON,LocalTime.of(13,0),Kind.ALL_DAY),null);
        check(manual.start()==null&&manual.durationMinutes()==0,"manual all-day clears times");
        f.schedules.saveSchedule(in(TODAY.minusDays(10),30),null);check(f.planning.getDailySummary(TODAY.minusDays(10))==30,"past direct allowed");
        f.schedules.saveSchedule(in(TODAY,null),a.id());check(f.schedules.get(a.id()).durationMinutes()==120,"edit missing duration preserves old");
    }
    private static void conflicts() throws Exception {
        Fixture f=new Fixture();Schedule a=f.schedules.saveSchedule(in("a",TODAY,300,LocalTime.of(9,0),LocalTime.of(12,0),Kind.NORMAL),null);
        Schedule b=f.schedules.saveSchedule(in("b",TODAY,300,LocalTime.of(12,0),LocalTime.of(17,0),Kind.NORMAL),null);
        check(f.planning.getDailySummary(TODAY)==480,"exact maximum and touching times allowed");
        fails(()->f.schedules.saveSchedule(in(TODAY,1),null),"one minute above max");
        check(f.planning.checkConflicts(a,a.id()).allowed(),"edit excludes itself");
        Schedule c=f.schedules.prepare(in("c",TODAY,1,LocalTime.of(11,0),LocalTime.of(13,0),Kind.NORMAL),null);
        ConflictResult conflict=f.planning.checkConflicts(c,null);check(!conflict.allowed()&&conflict.overlappingIds().size()==2,"two overlapping intervals detected");
        check(conflict.reasons().size()==2,"capacity and overlap both returned");
        f.schedules.updateScheduleProgress(b.id(),Status.COMPLETED,null);check(f.planning.getDailySummary(TODAY)==480,"completed included");
        f.schedules.deleteSchedule(a.id());check(f.planning.getDailySummary(TODAY)==300,"delete updates totals");
        fails(()->f.schedules.saveSchedule(in("all",TODAY,null,null,null,Kind.ALL_DAY),null),"all-day excludes completed too");
        f.schedules.deleteSchedule(b.id());Schedule all=f.schedules.saveSchedule(in("all",TODAY,null,null,null,Kind.ALL_DAY),null);
        check(f.planning.checkConflicts(all,all.id()).allowed(),"all-day editing self allowed");
        fails(()->f.schedules.saveSchedule(in(TODAY,1),null),"normal rejected on all-day");
        fails(()->f.schedules.saveSchedule(in("all2",TODAY,null,null,null,Kind.ALL_DAY),null),"two all-days rejected");
        Fixture g=new Fixture();g.schedules.saveSchedule(in(TODAY,400),null);List<LocalDate> exceeded=g.settings.updateTimeSettings(300,120);
        check(exceeded.equals(List.of(TODAY)),"limit decrease reports exceeded day");check(g.store.data().schedules().size()==1,"limit change deletes nothing");
        check(g.schedules.searchSchedules("일",TODAY,"학습",Importance.NORMAL,Status.PLANNED).size()==1,"search and filters");
        check(g.schedules.searchSchedules("absent",null,null,null,null).isEmpty(),"no search match");
        check(g.planning.getMonthScheduleCounts(YearMonth.from(TODAY)).get(TODAY)==1,"calendar count");
    }
    private static void recommendations() throws Exception {
        Fixture f=new Fixture();Schedule c=f.schedules.prepare(in(TODAY,60),null);
        List<Recommendation> list=f.planning.recommendDates(c,TODAY.plusDays(8),null);
        check(list.size()==8,"deadline excluded");check(list.get(0).date().equals(LocalDate.of(2026,11,7)),"Saturday tie priority");
        check(list.get(1).date().equals(LocalDate.of(2026,11,8)),"Sunday tie priority");check(list.get(2).date().equals(TODAY),"near weekdays first");
        check(list.stream().noneMatch(r->r.date().equals(TODAY.plusDays(8))),"no deadline day");
        check(f.planning.recommendDates(c,TODAY,null).size()==1,"today deadline includes today");
        fails(()->f.planning.recommendDates(c,TODAY.minusDays(1),null),"past deadline");
        fails(()->f.planning.recommendDates(c,null,null),"missing deadline");
        fails(()->f.planning.recommendDates(c,TODAY.plusYears(20),null),"explicit beta horizon bound");
        f.schedules.saveSchedule(in(LocalDate.of(2026,11,7),120),null);
        list=f.planning.recommendDates(c,TODAY.plusDays(8),null);check(list.get(0).date().equals(LocalDate.of(2026,11,8)),"lower load outranks loaded Saturday");
        check(list.get(list.size()-1).date().equals(LocalDate.of(2026,11,7)),"loaded weekend after empty weekdays");
        Schedule timed=f.schedules.prepare(in("time",TODAY,60,LocalTime.of(11,0),LocalTime.of(12,0),Kind.NORMAL),null);
        check(f.planning.recommendDates(timed,TODAY,null).isEmpty(),"today passed start excluded");
        check(!f.planning.recommendDates(c,TODAY,null).isEmpty(),"today untimed allowed after noon");
        Schedule all=f.schedules.prepare(in("all",TODAY,null,null,null,Kind.ALL_DAY),null);
        list=f.planning.recommendDates(all,TODAY.plusDays(8),null);check(list.size()==7,"all-day only empty dates");
        check(list.stream().noneMatch(r->r.date().equals(LocalDate.of(2026,11,7))),"all-day skips occupied date");
        check(f.store.data().schedules().size()==1,"recommend never persists");
        Fixture g=new Fixture();Schedule busy=g.schedules.saveSchedule(in("busy",TODAY,null,LocalTime.of(13,0),LocalTime.of(14,0),Kind.NORMAL),null);
        Schedule overlap=g.schedules.prepare(in("new",TODAY,null,LocalTime.of(13,30),LocalTime.of(14,30),Kind.NORMAL),null);
        check(g.planning.recommendDates(overlap,TODAY,null).isEmpty(),"recommend checks interval overlap");
        check(!g.planning.recommendDates(busy,TODAY,busy.id()).isEmpty(),"recommend update excludes own ID");
    }
    private static void progress() throws Exception {
        Fixture f=new Fixture();LocalDate past=TODAY.minusDays(1);Schedule a=f.schedules.saveSchedule(in("미완료",past,20,LocalTime.of(9,0),LocalTime.of(11,0),Kind.NORMAL),null);
        check(f.planning.isOverdue(a),"past noncompleted overdue");
        Schedule moved=f.schedules.updateScheduleProgress(a.id(),Status.PLANNED,TODAY);
        check(moved.id()==a.id()&&f.store.data().schedules().size()==1,"replan same ID no duplicate");
        check(moved.durationMinutes()==120&&moved.start()==null&&moved.end()==null,"replan duration kept times cleared");
        check(moved.status()==Status.PLANNED&&!f.planning.isOverdue(moved),"replan planned status");
        check(f.planning.getDailySummary(past)==0&&f.planning.getDailySummary(TODAY)==120,"both day totals update");
        fails(()->f.schedules.updateScheduleProgress(a.id(),Status.PLANNED,TODAY.plusDays(1)),"non-overdue replan rejected");
        Schedule done=f.schedules.updateScheduleProgress(a.id(),Status.COMPLETED,null);check(!f.planning.isOverdue(done),"completed never overdue");
        Schedule timed=f.schedules.saveSchedule(in("today time",TODAY,null,LocalTime.of(9,0),LocalTime.of(10,0),Kind.NORMAL),null);check(f.planning.isOverdue(timed),"past end today overdue");
        Schedule all=f.schedules.saveSchedule(in("past all",past,null,null,null,Kind.ALL_DAY),null);check(f.planning.isOverdue(all),"past all-day overdue");
        fails(()->f.schedules.updateScheduleProgress(all.id(),Status.PLANNED,TODAY),"all-day replan to occupied rejected");
        check(f.schedules.get(all.id()).date().equals(past),"conflict replan keeps old");
        Schedule movedAll=f.schedules.updateScheduleProgress(all.id(),Status.PLANNED,TODAY.plusDays(1));check(movedAll.kind()==Kind.ALL_DAY,"all-day replan retains kind");
        fails(()->f.schedules.deleteSchedule(99999),"unknown ID deletion");
    }
    private static void persistence() throws Exception {
        Fixture f=new Fixture();Schedule a=f.schedules.saveSchedule(in("한글 & <보존> \"제목\"",TODAY,120,null,null,Kind.NORMAL),null);
        AppData read=new FileRepository(f.store.repository().path()).loadData();check(read.equals(f.store.data()),"roundtrip Korean and XML escaping");
        Fixture reopened=new Fixture(new FileRepository(f.store.repository().path()));Schedule b=reopened.schedules.saveSchedule(in(TODAY.plusDays(1),120),null);check(b.id()>a.id(),"ID persists across restart");
        byte[] saved=Files.readAllBytes(f.store.repository().path());
        class Failing extends FileRepository {Failing(Path p){super(p);}@Override public void saveData(AppData data)throws IOException{throw new IOException("simulated full disk");}}
        Fixture broken=new Fixture(new Failing(f.store.repository().path()));AppData before=broken.store.data();
        fails(()->broken.schedules.saveSchedule(in(TODAY.plusDays(3),60),null),"failed new write");check(broken.store.data().equals(before),"failed new keeps memory");
        fails(()->broken.schedules.deleteSchedule(a.id()),"failed delete");check(broken.store.data().equals(before),"failed delete keeps memory");
        fails(()->broken.settings.updateTimeSettings(360,60),"failed settings");check(broken.settings.current().equals(before.settings()),"failed settings keeps settings");
        fails(()->broken.schedules.updateScheduleProgress(a.id(),Status.COMPLETED,null),"failed progress");check(broken.store.data().equals(before),"failed status keeps old");
        fails(()->broken.schedules.saveSchedule(in(TODAY,90),a.id()),"failed update");check(broken.schedules.get(a.id()).equals(a),"failed update preserves saved activity");
        check(Arrays.equals(saved,Files.readAllBytes(f.store.repository().path())),"all failed writes keep original bytes");
        Path corrupt=Files.createTempDirectory("timefit-corrupt-").resolve("data.xml");Files.writeString(corrupt,"broken XML");
        fails(()->new StateStore(new FileRepository(corrupt)),"corrupt startup rejected");check(Files.readString(corrupt).equals("broken XML"),"corrupt file unchanged");
        Path directory=Files.createTempDirectory("timefit-write-");FileRepository real=new FileRepository(directory.resolve("as-directory"));Files.createDirectory(real.path());
        Files.writeString(real.path().resolve("keep.txt"),"keep");fails(()->real.saveData(AppData.empty()),"real atomic replacement failure");check(Files.readString(real.path().resolve("keep.txt")).equals("keep"),"real target preserved");
        try(var files=Files.list(directory)){check(files.noneMatch(p->p.getFileName().toString().endsWith(".tmp")),"temp file cleaned on failure");}
    }
}
