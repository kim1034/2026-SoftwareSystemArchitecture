package timefit.ui;

import timefit.model.*;
import timefit.model.Schedule.*;
import timefit.service.*;
import timefit.storage.StateStore;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Main calendar, selected-day activities, search and list views. */
public final class MainFrame extends JFrame {
    private final StateStore store;
    private final ScheduleService schedules;
    private final PlanningService planning;
    private final SettingsService settings;
    private YearMonth month;
    private LocalDate selected;
    private final JPanel calendar = new JPanel(new GridLayout(0,7,5,5));
    private final JPanel dayList = new JPanel();
    private final JPanel allList = new JPanel();
    private final JLabel monthTitle = Theme.label("",23,true), dayTitle = Theme.label("날짜를 선택하세요",18,true), summary = new JLabel();
    private final JTextField search = new JTextField(17), filterDate = new JTextField(10);
    private final JComboBox<String> category = new JComboBox<>(new String[]{"전체", "학습", "과제", "연구", "개인", "기타"});
    private final JComboBox<Importance> importance = Theme.combo(new Importance[]{null, Importance.LOW, Importance.NORMAL, Importance.HIGH});
    private final JComboBox<Status> status = Theme.combo(new Status[]{null, Status.PLANNED, Status.IN_PROGRESS, Status.COMPLETED});
    private final JLabel filterError = new JLabel(" ");
    public MainFrame(StateStore store, ScheduleService schedules, PlanningService planning, SettingsService settings) {
        super("TimeFit · 개인 일정 관리 · Beta 0.4.0");
        this.store=store; this.schedules=schedules; this.planning=planning; this.settings=settings;
        month=YearMonth.now(planning.clock());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setSize(1240,850); setMinimumSize(new Dimension(1120,780)); setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout(18,18)); root.setBorder(new EmptyBorder(22,24,18,24)); setContentPane(root);
        JPanel top = new JPanel(new BorderLayout());
        JPanel brand = new JPanel(new GridLayout(2,1,0,6)); brand.add(Theme.label("TimeFit",30,true)); brand.add(new JLabel("오늘의 부담을 확인하고, 더 나은 날짜를 고르세요.")); top.add(brand,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(Theme.primary("+ 일정 추가",()->openEditor(null,false))); actions.add(Theme.button("시간 설정",this::openSettings)); top.add(actions,BorderLayout.EAST);
        root.add(top,BorderLayout.NORTH);
        JPanel middle=new JPanel(new BorderLayout(0,10));
        JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT,7,3));
        filters.add(new JLabel("제목")); filters.add(search); filters.add(new JLabel("날짜")); filters.add(filterDate); filterDate.setToolTipText("선택 사항 · yyyy-MM-dd");
        filters.add(category); filters.add(importance); filters.add(status); filters.add(Theme.button("필터 지우기",()->{search.setText(""); filterDate.setText("");category.setSelectedIndex(0);importance.setSelectedIndex(0);status.setSelectedIndex(0);}));
        JPanel f=new JPanel(new BorderLayout()); f.add(filters); filterError.setForeground(new Color(185,28,28)); f.add(filterError,BorderLayout.SOUTH); middle.add(f,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane(); JPanel calendarView=new JPanel(new BorderLayout(18,0));
        JPanel left=Theme.card(); left.setLayout(new BorderLayout(0,16)); JPanel nav=new JPanel(new BorderLayout());
        JPanel prevNext=new JPanel(new FlowLayout(FlowLayout.LEFT)); prevNext.setOpaque(false);
        prevNext.add(Theme.button("‹",()->{month=month.minusMonths(1);refresh();})); prevNext.add(Theme.button("›",()->{month=month.plusMonths(1);refresh();})); nav.add(prevNext,BorderLayout.WEST);
        monthTitle.setHorizontalAlignment(SwingConstants.CENTER); nav.add(monthTitle); nav.setOpaque(false);
        nav.add(Theme.button("오늘",()->{selected=LocalDate.now(planning.clock());month=YearMonth.from(selected);refresh();}),BorderLayout.EAST); left.add(nav,BorderLayout.NORTH);
        calendar.setOpaque(false); left.add(calendar); calendarView.add(left);
        JPanel right=Theme.card(); right.setLayout(new BorderLayout(0,12)); right.setPreferredSize(new Dimension(365,550));
        JPanel headings=new JPanel(new GridLayout(2,1,0,8)); headings.setOpaque(false); headings.add(dayTitle); headings.add(summary);right.add(headings,BorderLayout.NORTH);
        dayList.setLayout(new BoxLayout(dayList,BoxLayout.Y_AXIS));dayList.setBackground(Color.WHITE);JScrollPane scroll=new JScrollPane(dayList);scroll.setBorder(null);scroll.getVerticalScrollBar().setUnitIncrement(16);right.add(scroll);
        right.add(Theme.primary("선택 날짜에 일정 추가",()->openEditor(null,false)),BorderLayout.SOUTH);calendarView.add(right,BorderLayout.EAST);tabs.addTab("월별 달력",calendarView);
        allList.setLayout(new BoxLayout(allList,BoxLayout.Y_AXIS)); JScrollPane allScroll=new JScrollPane(allList);allScroll.getVerticalScrollBar().setUnitIncrement(20);tabs.addTab("전체 목록",allScroll);middle.add(tabs);root.add(middle);
        JLabel footer=new JLabel("완료 일정도 총시간에 포함 · 저장 위치: " + store.repository().path());footer.setForeground(Theme.MUTED);footer.setFont(footer.getFont().deriveFont(11f)); root.add(footer,BorderLayout.SOUTH);
        DocumentListener listener=new DocumentListener(){public void insertUpdate(DocumentEvent e){refresh();}public void removeUpdate(DocumentEvent e){refresh();}public void changedUpdate(DocumentEvent e){refresh();}};
        search.getDocument().addDocumentListener(listener); filterDate.getDocument().addDocumentListener(listener);
        category.addActionListener(e->refresh());importance.addActionListener(e->refresh());status.addActionListener(e->refresh());
        new Timer(60000,e->refresh()).start();refresh();
    }
    public void selectDate(LocalDate date) { selected=date;month=YearMonth.from(date);refresh(); }
    private List<Schedule> filtered(LocalDate date) {
        LocalDate requested=filterDate.getText().isBlank()?null:LocalDate.parse(filterDate.getText().trim());
        if (date!=null && requested!=null && !date.equals(requested)) return List.of();
        return schedules.searchSchedules(search.getText(),date==null?requested:date,category.getSelectedIndex()==0?null:(String)category.getSelectedItem(),
                (Importance)importance.getSelectedItem(),(Status)status.getSelectedItem());
    }
    public void refresh() {
        calendar.removeAll();monthTitle.setText(month.getYear()+"년 "+month.getMonthValue()+"월");
        for(String d:new String[]{"일","월","화","수","목","금","토"}) { JLabel l=new JLabel(d,SwingConstants.CENTER);l.setForeground(Theme.MUTED);calendar.add(l); }
        int offset=month.atDay(1).getDayOfWeek().getValue()%7; Map<LocalDate,Long> counts=planning.getMonthScheduleCounts(month);
        for(int cell=0;cell<42;cell++) {
            int day=cell-offset+1;
            if(day<1||day>month.lengthOfMonth()){JPanel blank=new JPanel();blank.setOpaque(false);calendar.add(blank);continue;}
            LocalDate date=month.atDay(day);long count=counts.getOrDefault(date,0L);long total=planning.getDailySummary(date);
            String dot=count==0?"&nbsp;":"● "+count+"개";boolean over=total>settings.current().dailyLimitMinutes();
            JButton b=Theme.button("<html><div style='text-align:center'>"+day+(date.equals(LocalDate.now(planning.clock()))?" · 오늘":"")+"<br><br><font color='"+(over?"#b91c1c":"#2563eb")+"'>"+dot+(over?" !":"")+"</font></div></html>",()->{selected=date;refresh();});
            b.setMargin(new Insets(4,1,4,1));b.setFont(b.getFont().deriveFont(12f));b.setBackground(date.equals(selected)?new Color(219,234,254):Color.WHITE);calendar.add(b);
        }
        dayList.removeAll();allList.removeAll();filterError.setText(" ");
        try {
            List<Schedule> all=filtered(null); if(all.isEmpty())allList.add(new JLabel("조건에 맞는 일정이 없어요."));else all.forEach(s->allList.add(activityCard(s,true)));
            if(selected==null){dayTitle.setText("날짜를 선택하세요");summary.setText("달력의 날짜를 클릭하면 일정을 보여드려요.");dayList.add(new JLabel("일정이 있는 날은 점과 개수로 표시해요."));}
            else {
                dayTitle.setText(selected.format(DateTimeFormatter.ofPattern("M월 d일 EEEE",java.util.Locale.KOREAN)));
                long total=planning.getDailySummary(selected),remaining=settings.current().dailyLimitMinutes()-total;
                summary.setText("<html>총 "+Theme.duration(total)+" / 최대 "+Theme.duration(settings.current().dailyLimitMinutes())+"<br>"+(remaining<0?"초과 "+Theme.duration(-remaining):"남은 시간 "+Theme.duration(remaining))+"</html>");
                summary.setForeground(remaining<0?new Color(185,28,28):Theme.INK);
                List<Schedule> list=filtered(selected);if(list.isEmpty())dayList.add(new JLabel("표시할 일정이 없어요. 새 일정을 추가하세요."));else list.forEach(s->dayList.add(activityCard(s,false)));
            }
        } catch(java.time.format.DateTimeParseException e){filterError.setText("날짜 필터는 yyyy-MM-dd로 입력하세요.");dayList.add(new JLabel("날짜 필터를 확인하세요."));}
        calendar.revalidate();calendar.repaint();dayList.revalidate();dayList.repaint();allList.revalidate();allList.repaint();
    }
    private JPanel activityCard(Schedule s,boolean withDate) {
        JPanel card=Theme.card();card.setLayout(new BorderLayout(0,9));card.setMaximumSize(new Dimension(Integer.MAX_VALUE,190));
        String time=s.kind()==Kind.ALL_DAY?"하루종일":s.start()==null?"시간 미지정":s.start()+"–"+s.end();
        JLabel description=new JLabel("<html><b>"+Theme.html(s.title())+"</b><br>"+(withDate?s.date()+" · ":"")+time+" · "+Theme.duration(s.effectiveMinutes(settings.current()))
                +"<br><font color='#64748b'>"+Theme.html(s.category())+" · "+Theme.display(s.importance())+" · "+Theme.display(s.status())+(planning.isOverdue(s)?" · 미완료":"")+"</font></html>");card.add(description);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));actions.setOpaque(false);actions.add(Theme.button("수정",()->openEditor(s,false)));
        actions.add(Theme.button("삭제",()->{if(JOptionPane.showConfirmDialog(this,"이 일정을 삭제할까요?","일정 삭제",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)try{schedules.deleteSchedule(s.id());refresh();}catch(Exception e){Theme.error(this,e);}}));
        actions.add(Theme.button("상태",()->{String choice=(String)JOptionPane.showInputDialog(this,"상태를 선택하세요.","상태 변경",JOptionPane.QUESTION_MESSAGE,null,new String[]{"예정","진행 중","완료"},Theme.display(s.status()));if(choice!=null)try{Status next=switch(choice){case "진행 중"->Status.IN_PROGRESS;case "완료"->Status.COMPLETED;default->Status.PLANNED;};schedules.updateScheduleProgress(s.id(),next,null);refresh();}catch(Exception e){Theme.error(this,e);}}));
        if(planning.isOverdue(s))actions.add(Theme.button("재계획",()->openEditor(s,true)));card.add(actions,BorderLayout.SOUTH);return card;
    }
    public void openEditor(Schedule old,boolean replan) {
        ScheduleDialog dialog=new ScheduleDialog(this,schedules,planning,settings,old,replan,selected==null?LocalDate.now(planning.clock()):selected,this::refresh);dialog.setVisible(true);
    }
    public void openSettings(){new SettingsDialog(this,settings,this::refresh).setVisible(true);}
}
