package com.cavetale.vote;

import java.util.Calendar;
import java.util.GregorianCalendar;

public final class State {
    int currentMonth; // 1-12
    long nextResetTime;

    void setCurrentMonth() {
        GregorianCalendar calendar = new GregorianCalendar();
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        nextResetTime = calculateNextResetTime();
    }

    long calculateNextResetTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = currentMonth + 1;
        if (month > 12) {
            year += 1;
            month = 1;
        }
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }
}
