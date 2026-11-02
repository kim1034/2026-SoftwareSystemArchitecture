package timefit.ui;

import timefit.service.SettingsService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/** Personal daily capacity and fallback duration editor. */
public final class SettingsDialog extends JDialog {
    public SettingsDialog(JFrame owner,SettingsService service,Runnable onSaved){
        super(owner,"개인 시간 기준 설정",true);setDefaultCloseOperation(DISPOSE_ON_CLOSE);setSize(650,480);setLocationRelativeTo(owner);
        JPanel root=new JPanel(new BorderLayout(0,24));root.setBorder(new EmptyBorder(28,30,24,30));setContentPane(root);root.add(Theme.label("나에게 맞는 시간 기준",24,true),BorderLayout.NORTH);
        JPanel form=Theme.card();form.setLayout(new GridLayout(0,1,0,10));
        JTextField limit=new JTextField(""+service.current().dailyLimitMinutes()),fallback=new JTextField(""+service.current().defaultDurationMinutes());
        form.add(new JLabel("하루 최대 활동 시간 (분 · 1~1440)"));form.add(limit);form.add(new JLabel("기본 소요 시간 (분 · 1 이상)"));form.add(fallback);
        form.add(new JLabel("<html>기본 시간은 하루 최대 시간보다 크게 설정할 수 있어요.<br>시간을 모두 생략한 새 일정은 하루종일로 적용해요.</html>"));
        form.add(new JLabel("<html>기존 일반 일정 시간은 그대로 유지해요.<br>하루종일은 바뀐 최대 시간으로 다시 계산해요.</html>"));root.add(form);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(Theme.button("취소",this::dispose));buttons.add(Theme.primary("저장",()->{
            try{List<LocalDate> exceeded=service.updateTimeSettings(Integer.parseInt(limit.getText().trim()),Integer.parseInt(fallback.getText().trim()));onSaved.run();dispose();
                if(!exceeded.isEmpty())JOptionPane.showMessageDialog(owner,"새 최대 시간을 넘는 날짜: "+exceeded+"\n기존 일정은 유지했어요.","시간 초과 안내",JOptionPane.WARNING_MESSAGE);
            }catch(NumberFormatException e){Theme.error(this,new IllegalArgumentException("시간은 정수 분으로 입력하세요."));}catch(Exception e){Theme.error(this,e);}
        }));root.add(buttons,BorderLayout.SOUTH);
    }
}
