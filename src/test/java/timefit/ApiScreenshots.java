package timefit;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.nio.file.*;
import timefit.ui.Theme;

/** Opens generated local Javadoc HTML in Swing's HTML viewer and captures the real window. */
public final class ApiScreenshots {
    public static void main(String[] args)throws Exception{
        Path base=Path.of("api-manual").toAbsolutePath(),out=Path.of("build/screenshots");Files.createDirectories(out);
        Robot robot=new Robot();
        for(String[] item:new String[][]{{"overview-summary.html","api-01-overview.png"},{"timefit/service/PlanningService.html","api-02-planning.png"}}){
            JFrame[] frame=new JFrame[1];JEditorPane[] editor=new JEditorPane[1];
            SwingUtilities.invokeAndWait(()->{
                Theme.install();frame[0]=new JFrame("TimeFit Javadoc HTML Viewer");frame[0].setSize(1300,950);frame[0].setLocation(30,20);
                JLabel path=new JLabel("Javadoc HTML · "+base.resolve(item[0]));path.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));frame[0].add(path,BorderLayout.NORTH);
                editor[0]=new JEditorPane();editor[0].setEditable(false);editor[0].putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,Boolean.TRUE);
                try{editor[0].setPage(base.resolve(item[0]).toUri().toURL());}catch(Exception e){throw new RuntimeException(e);}
                frame[0].add(new JScrollPane(editor[0]));frame[0].setVisible(true);
            });
            robot.delay(1200);robot.waitForIdle();
            if(item[0].contains("Planning")){SwingUtilities.invokeAndWait(()->editor[0].scrollToReference("recommendDates(timefit.model.Schedule,java.time.LocalDate,java.lang.Long)"));robot.delay(300);}
            ImageIO.write(robot.createScreenCapture(frame[0].getBounds()),"png",out.resolve(item[1]).toFile());SwingUtilities.invokeAndWait(()->frame[0].dispose());
        }
        System.out.println("Captured generated Javadoc HTML in an actual Swing HTML viewer");System.exit(0);
    }
}
