package timefit;

import timefit.storage.*;
import timefit.service.*;
import timefit.ui.*;
import javax.swing.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.time.Clock;

/** TimeFit beta entry point. Uses an OS lock to prevent two instances from overwriting the same data. */
public final class App {
    private App() { }
    private static FileChannel lockChannel;
    private static FileLock lock;
    /** Launches the GUI. Use -Dtimefit.data=/path/data.xml before -jar to override the default data file.
     * @param args no arguments are required
     */
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->{
            Theme.install();
            try{
                Path data=Path.of(System.getProperty("timefit.data",Path.of(System.getProperty("user.home"),".timefit","data.xml").toString())).toAbsolutePath();
                Files.createDirectories(data.getParent());lockChannel=FileChannel.open(data.resolveSibling(data.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);lock=lockChannel.tryLock();
                if(lock==null)throw new IllegalStateException("같은 데이터 파일을 사용하는 TimeFit이 이미 실행 중이에요.");
                Runtime.getRuntime().addShutdownHook(new Thread(()->{try{if(lock!=null)lock.release();lockChannel.close();}catch(Exception ignored){}}));
                StateStore store=new StateStore(new FileRepository(data));PlanningService planning=new PlanningService(store,Clock.systemDefaultZone());
                new MainFrame(store,new ScheduleService(store,planning),planning,new SettingsService(store)).setVisible(true);
            }catch(Exception e){Theme.error(null,e);System.exit(1);}
        });
    }
}
