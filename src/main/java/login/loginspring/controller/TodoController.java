package login.loginspring.controller;

import login.loginspring.domain.*;
import login.loginspring.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class TodoController {

    private final CalenderService calenderService;
    private final MemberService memberService;
    private final TodoService todoService;
    private final GoalService goalService;
    private final ChallengeService challengeService;

    LocalDate now = LocalDate.now();
    int current_year = now.getYear();
    int current_month = now.getMonthValue();
    int current_date = now.getDayOfMonth();
    String today = current_year + "-" + (current_month < 10 ? "0" + current_month : current_month) + "-" + (current_date < 10 ? "0" + current_date : current_date);

    String feed_date = today;
    String onlyDate = String.valueOf(current_date);
    String day = String.valueOf(now.getDayOfWeek().getValue()-1);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    public TodoController(CalenderService calenderService, MemberService memberService, TodoService todoService, GoalService goalService, ChallengeService challengeService) {
        this.calenderService = calenderService;
        this.memberService = memberService;
        this.todoService = todoService;
        this.goalService = goalService;
        this.challengeService = challengeService;
    }

    @GetMapping("/todolist")
    public String main(Model model, Authentication authentication) {
        Member member = (Member) authentication.getPrincipal(); //로그인된 사용자 정보
        model.addAttribute("id", member.getUserId());
        model.addAttribute("name", member.getUserName());
        model.addAttribute("message", member.getUserMessage());
        ArrayList<String> cal = calenderService.changeYearMonth(current_year, current_month);
        model.addAttribute("cal", cal);
        model.addAttribute("year", current_year);
        model.addAttribute("month", current_month);
        model.addAttribute("feed_date", feed_date);
        model.addAttribute("day",day);
        model.addAttribute("onlyDate",onlyDate);
//      은화
        List<Goals> goalsList = goalService.findGoals();
        model.addAttribute("goals", goalsList);
        List<Todos> todosList = todoService.findTodos();
        model.addAttribute("todos", todosList);

        String memberId = member.getUserId();
        List<Challenge> challenges = challengeService.findChallenges(memberId);
        model.addAttribute("challenges", challenges);

        return "todolist";
    }


    @PostMapping("/todolist")
    public String ButtonControl(Member member, Model model, DiffForm diff, DateForm btn_date) {
        int differ;
        if (diff.getDiff() == null) {
            differ = 0;
        } else {
            differ = Integer.parseInt(diff.getDiff());
        }

        if (differ != 0) {
            int[] date = calenderService.changeMonth(current_year, current_month, differ);
            current_year = date[0];
            current_month = date[1];
        } else if (feed_date != null) {
            feed_date = String.valueOf(btn_date.getFeed_date());
            onlyDate = btn_date.getOnlyDate();
            day = btn_date.getDay();
        }
        return "redirect:/todolist"; //접근 html
    }

    @GetMapping("/menu")
    public String menu(Model model, Authentication authentication) {
        Member member = (Member) authentication.getPrincipal(); //로그인된 사용자 정보
        model.addAttribute("name", member.getUserName());
        return "menu";
    }

    @PostMapping("/menu")
    public String menu() {
        return "redirect:/todolist";
    }


    @GetMapping("/profile_edit")
    public String ProfileEdit(Model model, Authentication authentication) {
        Member member = (Member) authentication.getPrincipal(); //로그인된 사용자 정보
        model.addAttribute("name", member.getUserName());
        model.addAttribute("message", member.getUserMessage());
        return "profile_edit";
    }

    @PostMapping("/profile_edit")
    public String ProfileUpdate(updateMember updatemember) {
        Member member = memberService.updateUser(updatemember);
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(member.getUsername(), member.getUserRpw()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:/todolist";
    }

    @PostMapping("/insertTodo")
    public String createTodo(TodoForm form, Authentication authentication) {
        Member member = (Member) authentication.getPrincipal(); //로그인한 사용자 정보
        String memberId = member.getUserId();
//        System.out.println(form.getGoalId()+","+form.getContent()+","+form.getDate()+","+form.getOrderNum());
        Todos todos = new Todos();
        todos.setUserId(memberId);
        todos.setGoalId(Integer.valueOf(form.getGoalId()));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            todos.setDate(formatter.parse(feed_date)); //getDate() 클릭한 달력의 날짜로 바꿔야 함
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        todos.setOrderNum(Integer.valueOf(form.getOrderNum()));
        todos.setContent(form.getContent());
        todos.setStartDate(null);
        todos.setEndDate(null);
        todos.setIsRepeatMon(0);
        todos.setIsRepeatTue(0);
        todos.setIsRepeatWed(0);
        todos.setIsRepeatThu(0);
        todos.setIsRepeatFri(0);
        todos.setIsRepeatSat(0);
        todos.setIsRepeatSun(0);
        todos.setIsChecked(0);

        todoService.join(todos);
        System.out.println(todos.toString());
        return "redirect:/todolist";
    }

    @PostMapping("/updateTodo")
    public String updateTodo(TodoForm form) {
        System.out.println(form.getMode());
        if (form.getMode().equals("수정하기")) {
            if (form.getContent().equals("")) {
                System.out.println("데이터 삭제하라우");
                todoService.delete(Integer.valueOf(form.getId()));
            } else {
                Optional<Todos> todos = todoService.findById(Integer.valueOf(form.getId()));
                todos.get().setContent(form.getContent());
                todoService.join(todos.get());
            }
        } else if (form.getMode().equals("내일 하기")) {
            String[] result = feed_date.split("-");
            int year = Integer.valueOf(result[0]);
            int month = Integer.valueOf(result[1]);
            int date = Integer.valueOf(result[2]) + 1;

            String tomorrow = year + "-" + (month < 10 ? "0" + month : month) + "-" + (date < 10 ? "0" + date : date);

            Optional<Todos> todos = todoService.findById(Integer.valueOf(form.getId()));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            try {
                todos.get().setDate(formatter.parse(tomorrow));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            todoService.join(todos.get());
        } else if (form.getMode().equals("삭제하기")) {
            todoService.delete(Integer.valueOf(form.getId()));
        } else if (form.getMode().equals("check")) {
            System.out.println("응 체크~");
            System.out.println(form.getIsChecked());
            System.out.println(form.getId());
            Optional<Todos> todos = todoService.findById(Integer.valueOf(form.getId()));
            if (todos.get().getIsChecked() == 0) {
                todos.get().setIsChecked(1);
                todoService.join(todos.get());
            } else {
                todos.get().setIsChecked(0);
                todoService.join(todos.get());
            }

        }
        return "redirect:/todolist";
    }
}
