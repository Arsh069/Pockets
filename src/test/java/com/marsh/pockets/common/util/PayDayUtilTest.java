package com.marsh.pockets.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayDayUtilTest {

    @Test
    @DisplayName("payDay=15 matches only the 15th of the month")
    void testStandardPayDay() {
        LocalDate fifteenth = LocalDate.of(2026, 4, 15);
        LocalDate fourteenth = LocalDate.of(2026, 4, 14);

        assertTrue(PayDayUtil.isResetDay(15, fifteenth));
        assertFalse(PayDayUtil.isResetDay(15, fourteenth));
    }

    @Test
    @DisplayName("payDay=31 matches the 30th in April (30-day month)")
    void testPayDay31InApril() {
        LocalDate april30 = LocalDate.of(2026, 4, 30);
        LocalDate april29 = LocalDate.of(2026, 4, 29);

        assertTrue(PayDayUtil.isResetDay(31, april30));
        assertFalse(PayDayUtil.isResetDay(31, april29));
    }

    @Test
    @DisplayName("payDay=29 matches Feb 28 in a non-leap year")
    void testPayDay29InFebNonLeapYear() {
        LocalDate feb28NonLeap = LocalDate.of(2025, 2, 28);
        LocalDate feb27 = LocalDate.of(2025, 2, 27);

        assertTrue(PayDayUtil.isResetDay(29, feb28NonLeap));
        assertFalse(PayDayUtil.isResetDay(29, feb27));
    }
}
