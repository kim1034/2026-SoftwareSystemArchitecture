package timefit.ui;

import timefit.model.*;
import timefit.model.Schedule.*;
import timefit.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.time.*;
import java.time.format.*;
import java.util.List;

/** Integrated editor and always-visible live recommendations. Candidate selection changes only the date. */
public final class ScheduleDialog extends JDialog {
    private final ScheduleService schedules;
    private final PlanningService planning;
    private final SettingsService settings;
    private final Schedule old;
    private final boolean replan;
    private final Runnable onSaved;
    private final JTextField title=new JTextField(), date=new JTextField(), duration=new JTextField(), start=new JTextField(),end=new JTextField(), deadline=new JTextField();
    private final JComboBox<String> category=new JComboBox<>(new String[]{"학습","과제","연구","개인","기타"});
    private final JComboBox<Importance> importance=Theme.combo(Importance.values());
    private final JComboBox<Status> status=Theme.combo(Status.values());
    private final JComboBox<Kind> kind=Theme.combo(Kind.values());
    private final JLabel feedback=new JLabel(" "), effective=new JLabel(" ");
    private final JPanel recommendations=new JPanel();
    private final JButton save=Theme.primary("저장",this::save);
    private final JButton more=Theme.button("더 보기",()->{expanded=true;refreshPreview();});
    private final Timer debounce;
    private boolean expanded;
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public ScheduleDialog(JFrame owner,ScheduleService schedules,PlanningService planning,SettingsService settings,Schedule old,boolean replan,LocalDate selected,Runnable onSaved) {
        super(owner,replan?"미완료 일정 재계획":old==null?"새 일정 등록":"일정 수정",true);
        this.schedules=schedules;this.planning=planning;this.settings=settings;this.old=old;this.replan=replan;this.onSaved=onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);setSize(1040,760);setMinimumSize(new Dimension(980,710));setLocationRelativeTo(owner);
        JPanel root=new JPanel(new BorderLayout(16,16));root.setBorder(new EmptyBorder(22,22,20,22));setContentPane(root);
        root.add(Theme.label(replan?"새 날짜로 다시 계획해요":old==null?"새 일정을 계획해요":"일정을 수정해요",24,true),BorderLayout.NORTH);
        JPanel columns=new JPanel(new GridLayout(1,2,18,0));JPanel form=Theme.card();form.setLayout(new GridBagLayout());int row=0;
        row=field(form,row,"제목 *",title);row=field(form,row,"일정 날짜 * (yyyy-MM-dd)",date);
        row=field(form,row,"시간 구분",kind);row=field(form,row,"예상 소요 시간 (분 · 비우면 기본값)",duration);
        row=field(form,row,"시작 시간 (HH:mm · 선택)",start);row=field(form,row,"종료 시간 (HH:mm · 선택)",end);
        row=field(form,row,"일정 종류",category);row=field(form,row,"중요도",importance);row=field(form,row,"상태",status);
        row=field(form,row,"추천용 마감일 (yyyy-MM-dd · 선택)",deadline);field(form,row,"적용 시간",effective);columns.add(form);
        JPanel rec=Theme.card();rec.setLayout(new BorderLayout(0,14));JPanel recHeader=new JPanel(new GridLayout(2,1,0,8));recHeader.setOpaque(false);
        recHeader.add(Theme.label("추천 날짜",21,true));JLabel rule=new JLabel("덜 바쁜 날 → 동률이면 토·일 → 가까운 날");rule.setFont(rule.getFont().deriveFont(12f));recHeader.add(rule);rec.add(recHeader,BorderLayout.NORTH);
        recommendations.setLayout(new BoxLayout(recommendations,BoxLayout.Y_AXIS));recommendations.setBackground(Color.WHITE);JScrollPane scroll=new JScrollPane(recommendations);scroll.setBorder(null);scroll.getVerticalScrollBar().setUnitIncrement(16);rec.add(scroll);
        JPanel recFoot=new JPanel(new BorderLayout(0,10));recFoot.setOpaque(false);recFoot.add(more,BorderLayout.NORTH);recFoot.add(new JLabel("<html>후보 선택은 날짜만 바꿔요.<br>저장 버튼을 눌러야 일정이 저장돼요.</html>"));rec.add(recFoot,BorderLayout.SOUTH);columns.add(rec);root.add(columns);
        JPanel foot=new JPanel(new BorderLayout(12,0));feedback.setPreferredSize(new Dimension(720,65));foot.add(feedback);JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(Theme.button("취소",this::dispose));buttons.add(save);foot.add(buttons,BorderLayout.EAST);root.add(foot,BorderLayout.SOUTH);
        date.setText((old!=null&&!replan?old.date():selected).toString());
        importance.setSelectedItem(old==null?Importance.NORMAL:old.importance());
        if(old!=null){title.setText(old.title());category.setSelectedItem(old.category());kind.setSelectedItem(old.kind());status.setSelectedItem(replan?Status.PLANNED:old.status());
            if(old.kind()==Kind.NORMAL)duration.setText(""+old.durationMinutes());if(!replan&&old.start()!=null){start.setText(old.start().toString());end.setText(old.end().toString());}}
        if(replan){title.setEditable(false);category.setEnabled(false);importance.setEnabled(false);kind.setEnabled(false);status.setEnabled(false);duration.setEditable(false);start.setEnabled(false);end.setEnabled(false);}
        debounce=new Timer(180,e->{expanded=false;refreshPreview();});debounce.setRepeats(false);
        DocumentListener listener=new DocumentListener(){public void insertUpdate(DocumentEvent e){invalidatePreview();}public void removeUpdate(DocumentEvent e){invalidatePreview();}public void changedUpdate(DocumentEvent e){invalidatePreview();}};
        for(JTextField f:new JTextField[]{title,date,duration,start,end,deadline})f.getDocument().addDocumentListener(listener);
        kind.addActionListener(e->{if(kind.getSelectedItem()==Kind.ALL_DAY){duration.setText("");start.setText("");end.setText("");}updateTimeInputs();invalidatePreview();});
        category.addActionListener(e->invalidatePreview());importance.addActionListener(e->invalidatePreview());status.addActionListener(e->invalidatePreview());
        updateTimeInputs();refreshPreview();
    }
    private int field(JPanel p,int row,String label,JComponent component) {
        GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=row;c.weightx=0.0;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(4,0,4,12);p.add(new JLabel(label),c);
        c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(4,0,4,0);component.setPreferredSize(new Dimension(150,29));p.add(component,c);return row+1;
    }
    private void updateTimeInputs(){boolean enabled=!replan&&kind.getSelectedItem()==Kind.NORMAL;duration.setEnabled(enabled);start.setEnabled(enabled);end.setEnabled(enabled);}
    private void invalidatePreview(){save.setEnabled(false);more.setEnabled(false);recommendations.removeAll();recommendations.add(new JLabel("입력을 확인하고 있어요…"));recommendations.revalidate();recommendations.repaint();debounce.restart();}
    public void setDraft(String draftTitle,LocalDate draftDate,Integer minutes,LocalDate draftDeadline) {
        title.setText(draftTitle);date.setText(draftDate.toString());duration.setText(minutes==null?"":minutes.toString());deadline.setText(draftDeadline==null?"":draftDeadline.toString());debounce.stop();refreshPreview();
    }
    private ScheduleInput input() {
        try {
            boolean all=kind.getSelectedItem()==Kind.ALL_DAY;
            Integer estimated=all||duration.getText().isBlank()?null:Integer.valueOf(duration.getText().trim());
            if(estimated!=null&&estimated<=0)throw new IllegalArgumentException("예상 소요 시간은 1분 이상으로 입력하세요.");
            return new ScheduleInput(title.getText(),LocalDate.parse(date.getText().trim(),DATE),(String)category.getSelectedItem(),(Importance)importance.getSelectedItem(),
                    (Status)status.getSelectedItem(),(Kind)kind.getSelectedItem(),estimated,all||start.getText().isBlank()?null:LocalTime.parse(start.getText().trim(),TIME),all||end.getText().isBlank()?null:LocalTime.parse(end.getText().trim(),TIME));
        } catch(DateTimeParseException e){throw new IllegalArgumentException("날짜는 yyyy-MM-dd, 시간은 HH:mm으로 입력하세요.");}
          catch(NumberFormatException e){throw new IllegalArgumentException("예상 소요 시간은 정수 분으로 입력하세요.");}
    }
    public void refreshPreview() {
        recommendations.removeAll();more.setVisible(false);more.setEnabled(false);
        try {
            ScheduleInput input=input();Schedule candidate=schedules.prepare(input,old==null?null:old.id());
            ConflictResult conflict=planning.checkConflicts(candidate,old==null?null:old.id());
            boolean auto=candidate.kind()==Kind.ALL_DAY&&input.kind()==Kind.NORMAL;
            effective.setText(Theme.display(candidate.kind())+" · "+Theme.duration(candidate.effectiveMinutes(settings.current())));
            feedback.setForeground(conflict.allowed()?new Color(22,101,52):new Color(185,28,28));
            feedback.setText("<html>"+(auto?"기본 시간이 최대 시간을 넘어 하루종일로 적용해요.<br>":"")+(conflict.allowed()?"선택 날짜에 등록 가능 · 등록 후 "+Theme.duration(conflict.totalMinutes()):Theme.html(String.join(" / ",conflict.reasons())))+"</html>");
            save.setEnabled(conflict.allowed());
            if(deadline.getText().isBlank()){recommendations.add(new JLabel("마감일을 입력하면 날짜를 추천합니다."));}
            else {
                LocalDate limit;
                try {limit=LocalDate.parse(deadline.getText().trim(),DATE);}catch(DateTimeParseException e){throw new IllegalArgumentException("마감일은 yyyy-MM-dd로 입력하세요.");}
                List<Recommendation> list=planning.recommendDates(candidate,limit,old==null?null:old.id());
                if(list.isEmpty())recommendations.add(new JLabel("가능한 날짜가 없어요. 시간이나 마감일을 바꿔보세요."));
                int n=expanded?list.size():Math.min(3,list.size());
                for(int i=0;i<n;i++){
                    Recommendation r=list.get(i);JPanel item=Theme.card();item.setLayout(new BorderLayout(8,12));item.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));
                    item.add(new JLabel("<html><b>"+(i+1)+". "+r.date().format(DateTimeFormatter.ofPattern("M월 d일 EEEE",java.util.Locale.KOREAN))+"</b><br><br>등록 후 "+Theme.duration(r.totalMinutes())+"<br>남는 시간 "+Theme.duration(r.remainingMinutes())+"<br><font color='#64748b'>"+(PlanningService.isWeekend(r.date())?"주말 · ":"")+"총시간이 적은 날짜 우선</font></html>"));
                    item.add(Theme.button(r.date().toString().equals(date.getText().trim())?"선택됨":"선택",()->{date.setText(r.date().toString());debounce.stop();refreshPreview();}),BorderLayout.EAST);recommendations.add(item);recommendations.add(Box.createVerticalStrut(10));
                }
                more.setVisible(!expanded&&list.size()>3);more.setEnabled(true);more.setText("더 보기 (전체 "+list.size()+"개)");
            }
        } catch(IllegalArgumentException e){effective.setText("입력 확인 필요");feedback.setForeground(new Color(185,28,28));feedback.setText("<html>"+Theme.html(e.getMessage())+"</html>");save.setEnabled(false);recommendations.removeAll();recommendations.add(new JLabel(deadline.getText().isBlank()?"마감일을 입력하면 날짜를 추천합니다.":"올바른 입력 후 추천을 표시해요."));}
        recommendations.revalidate();recommendations.repaint();
    }
    private void save(){
        try {ScheduleInput input=input();Schedule candidate=schedules.prepare(input,old==null?null:old.id());
            if(!deadline.getText().isBlank())planning.recommendDates(candidate,LocalDate.parse(deadline.getText().trim(),DATE),old==null?null:old.id());
            if(replan)schedules.updateScheduleProgress(old.id(),Status.PLANNED,input.date());else schedules.saveSchedule(input,old==null?null:old.id());
            onSaved.run();dispose();
        }catch(Exception e){Theme.error(this,e);refreshPreview();}
    }
    @Override public void dispose(){if(debounce!=null)debounce.stop();super.dispose();}
}
