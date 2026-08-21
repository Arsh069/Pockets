package com.marsh.pockets.common.util;

import java.time.LocalDate;
import java.time.YearMonth;

public final class PayDayUtil {

    private PayDayUtil() {
    }

    public static boolean isResetDay(int payDay, LocalDate today) {
        if (payDay < 1 || payDay > 31) {
            throw new IllegalArgumentException("payDay must be between 1 and 31");
        }
        if (today == null) {
            throw new IllegalArgumentException("today cannot be null");
        }

        if (payDay <= 28) {
            return today.getDayOfMonth() == payDay;
        }

        int lastDayOfMonth = YearMonth.from(today).lengthOfMonth();
        return today.getDayOfMonth() == lastDayOfMonth;
    }
}
