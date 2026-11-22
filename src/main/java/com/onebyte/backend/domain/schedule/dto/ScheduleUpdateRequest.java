package com.onebyte.backend.domain.schedule.dto;

/**
 * 일정 부분 수정 요청 (PATCH).
 * 요청에 없는 필드는 기존 값을 유지하고, 명시적으로 null을 보낸 필드는 값을 지운다.
 * 이를 구분하기 위해 Jackson이 필드가 있을 때만 호출하는 setter에서 전달 여부를 기록한다.
 * (has* 메서드는 bean 접근자 이름이 아니라 JSON 속성으로 노출되지 않는다)
 */
public class ScheduleUpdateRequest {

    private String title;
    private boolean titlePresent;

    private String date;
    private boolean datePresent;

    private String time;
    private boolean timePresent;

    private String deadlineDate;
    private boolean deadlineDatePresent;

    private String priority;
    private boolean priorityPresent;

    private RecurrenceRequest recurrence;
    private boolean recurrencePresent;

    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public void setDate(String date) {
        this.date = date;
        this.datePresent = true;
    }

    public void setTime(String time) {
        this.time = time;
        this.timePresent = true;
    }

    public void setDeadlineDate(String deadlineDate) {
        this.deadlineDate = deadlineDate;
        this.deadlineDatePresent = true;
    }

    public void setPriority(String priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    public void setRecurrence(RecurrenceRequest recurrence) {
        this.recurrence = recurrence;
        this.recurrencePresent = true;
    }

    public String title() {
        return title;
    }

    public String date() {
        return date;
    }

    public String time() {
        return time;
    }

    public String deadlineDate() {
        return deadlineDate;
    }

    public String priority() {
        return priority;
    }

    public RecurrenceRequest recurrence() {
        return recurrence;
    }

    public boolean hasTitle() {
        return titlePresent;
    }

    public boolean hasDate() {
        return datePresent;
    }

    public boolean hasTime() {
        return timePresent;
    }

    public boolean hasDeadlineDate() {
        return deadlineDatePresent;
    }

    public boolean hasPriority() {
        return priorityPresent;
    }

    public boolean hasRecurrence() {
        return recurrencePresent;
    }

    public boolean hasAnyField() {
        return titlePresent || datePresent || timePresent || deadlineDatePresent || priorityPresent || recurrencePresent;
    }
}
