package timefit;

import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import timefit.model.*;
import timefit.model.Schedule.*;
import timefit.storage.FileRepository;
import java.time.LocalDate;
import java.util.List;

/** Launches the packaged JAR twice from a fresh folder with real saved Korean data. Requires a graphical display. */
public final class JarSmokeTests {
    public static void main(String[] args)throws Exception{
        Path jar=Path.of(args.length==0?"dist/ssd_ip_cp4_2026f_224569_app.jar":args[0]).toAbsolutePath();
        Path temp=Files.createTempDirectory("timefit-jar-");Files.copy(jar,temp.resolve("app.jar"));
        Path data=temp.resolve("data.xml");AppData snapshot=new AppData(UserSettings.defaults(),List.of(new Schedule(1,"JAR 재실행 확인",LocalDate.now(),"과제",Importance.HIGH,Status.PLANNED,Kind.NORMAL,120,null,null)),2);
        new FileRepository(data).saveData(snapshot);
        for(int i=0;i<2;i++){
            Process process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Dtimefit.data="+data,"-jar","app.jar")
                    .directory(temp.toFile()).redirectErrorStream(true).redirectOutput(temp.resolve("run-"+i+".log").toFile()).start();
            try{if(process.waitFor(3,TimeUnit.SECONDS))throw new AssertionError("JAR exited early: "+Files.readString(temp.resolve("run-"+i+".log")));}
            finally{process.destroy();if(!process.waitFor(3,TimeUnit.SECONDS))process.destroyForcibly();}
            if(!new FileRepository(data).loadData().equals(snapshot))throw new AssertionError("Saved data changed during launch");
        }
        System.out.println("PASS: packaged JAR launched twice from a clean directory; saved data preserved");
    }
}
