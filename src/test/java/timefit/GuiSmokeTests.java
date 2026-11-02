package timefit;

import timefit.model.*;
import timefit.model.Schedule.*;
import timefit.service.*;
import timefit.storage.*;
import timefit.ui.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.nio.file.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

/** Real Swing/Robot smoke test and screenshots. Requires a graphical display (or xvfb-run on Linux). */
public final class GuiSmokeTests {
    private static MainFrame frame;
    private static ScheduleDialog dialog;
    private static ScheduleService schedules;
    private static PlanningService planning;
    private static SettingsService settings;
    private static final LocalDate TODAY=LocalDate.of(2026,11,2);
    private static int checks;
    private static final Robot ROBOT;
    static{try{ROBOT=new Robot();ROBOT.setAutoDelay(70);}catch(Exception e){throw new RuntimeException(e);}}
    private static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
    private static void edt(Runnable action)throws Exception{SwingUtilities.invokeAndWait(action);ROBOT.waitForIdle();}
    private static JButton find(Container c,String text){for(Component v:c.getComponents()){if(v instanceof JButton b&&text.equals(b.getText()))return b;if(v instanceof Container n){JButton b=find(n,text);if(b!=null)return b;}}return null;}
    private static void click(Container owner,String text)throws Exception{
        AtomicReference<Point> point=new AtomicReference<>();edt(()->{JButton b=find(owner,text);check(b!=null&&b.isShowing()&&b.isEnabled(),"button available: "+text);Point p=b.getLocationOnScreen();point.set(new Point(p.x+b.getWidth()/2,p.y+b.getHeight()/2));});
        Point p=point.get();ROBOT.mouseMove(p.x,p.y);ROBOT.mousePress(InputEvent.BUTTON1_DOWN_MASK);ROBOT.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);ROBOT.delay(450);
    }
    private static void capture(Window window,Path output)throws Exception{ROBOT.waitForIdle();ROBOT.delay(250);AtomicReference<Rectangle> rect=new AtomicReference<>();edt(()->rect.set(window.getBounds()));ImageIO.write(ROBOT.createScreenCapture(rect.get()),"png",output.toFile());}
    private static ScheduleInput input(String title,LocalDate date,int minutes){return new ScheduleInput(title,date,"연구",Importance.HIGH,Status.PLANNED,Kind.NORMAL,minutes,null,null);}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args.length==0?"build/screenshots":args[0]);Files.createDirectories(out);
        try{
            StateStore store=new StateStore(new FileRepository(Files.createTempDirectory("timefit-gui-").resolve("data.xml")));
            planning=new PlanningService(store,Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"),ZoneId.of("Asia/Seoul")));schedules=new ScheduleService(store,planning);settings=new SettingsService(store);
            schedules.saveSchedule(input("랩미팅 발표 준비",TODAY,180),null);Schedule b=schedules.saveSchedule(input("TimeFit 베타 구현",TODAY,120),null);
            schedules.updateScheduleProgress(b.id(),Status.IN_PROGRESS,null);Schedule read=schedules.saveSchedule(input("논문 읽기",TODAY,60),null);schedules.updateScheduleProgress(read.id(),Status.COMPLETED,null);
            Schedule overdue=schedules.saveSchedule(input("지난 일정 재계획 예시",TODAY.minusDays(1),60),null);
            schedules.saveSchedule(input("프로젝트 검토",TODAY.plusDays(2),120),null);
            edt(()->{Theme.install();frame=new MainFrame(store,schedules,planning,settings);frame.setVisible(true);});
            capture(frame,out.resolve("beta-01-calendar.png"));edt(()->frame.selectDate(TODAY));capture(frame,out.resolve("beta-02-selected.png"));
            click(frame,"+ 일정 추가");
            edt(()->{for(Window w:Window.getWindows())if(w instanceof ScheduleDialog d&&d.isShowing())dialog=d;check(dialog!=null,"editor opened");dialog.setDraft("생성형 AI 논문 읽기",TODAY.plusDays(1),60,TODAY.plusDays(8));});
            capture(dialog,out.resolve("beta-03-editor-recommend.png"));
            int before=store.data().schedules().size();click(dialog,"선택");check(store.data().schedules().size()==before,"candidate click never saves");
            click(dialog,"저장");check(store.data().schedules().size()==before+1,"editor save persists");
            click(frame,"+ 일정 추가");edt(()->{for(Window w:Window.getWindows())if(w instanceof ScheduleDialog d&&d.isShowing())dialog=d;dialog.setDraft("시간 초과 확인",TODAY,240,TODAY.plusDays(8));});
            edt(()->check(!find(dialog,"저장").isEnabled(),"conflict disables save"));capture(dialog,out.resolve("beta-04-conflict.png"));click(dialog,"취소");
            click(frame,"시간 설정");AtomicReference<SettingsDialog> settingsWindow=new AtomicReference<>();edt(()->{for(Window w:Window.getWindows())if(w instanceof SettingsDialog d&&d.isShowing())settingsWindow.set(d);});
            check(settingsWindow.get()!=null,"settings opened");capture(settingsWindow.get(),out.resolve("beta-05-settings.png"));click(settingsWindow.get(),"취소");
            edt(()->frame.selectDate(TODAY.minusDays(1)));click(frame,"재계획");edt(()->{for(Window w:Window.getWindows())if(w instanceof ScheduleDialog d&&d.isShowing())dialog=d;dialog.setDraft(overdue.title(),TODAY.plusDays(3),60,TODAY.plusDays(8));});
            capture(dialog,out.resolve("beta-06-replan.png"));click(dialog,"저장");check(schedules.get(overdue.id()).date().equals(TODAY.plusDays(3)),"replan via UI moves same ID");check(store.data().schedules().size()==before+1,"replan no duplicate");
            check(new FileRepository(store.repository().path()).loadData().equals(store.data()),"UI saved data reloads exactly");
            System.out.println("PASS: "+checks+" GUI assertions; 6 real Swing screenshots captured");
            edt(()->{for(Window w:Window.getWindows())w.dispose();});System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
