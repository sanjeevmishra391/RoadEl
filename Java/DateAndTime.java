package Java;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateAndTime {
    
    private static void performAnimalEnrichment(LocalDate start, LocalDate end, Period period) {
        LocalDate upTo = start;
        while (upTo.isBefore(end)) {          // check if still before end
            System.out.println("give new toy: " + upTo);
            upTo = upTo.plus(period);                   // add a month
        }
    }
    
    public static void main(String[] args) {
        // LocalDate
        System.out.println(LocalDate.now());
        LocalDate d = LocalDate.of(2024, Month.DECEMBER, 17);
        System.out.println(d);
        d = d.plusDays(15);
        System.err.println(d);
        d = d.plusWeeks(1);
        System.out.println(d);
        d = d.plusMonths(2);
        System.out.println(d);
        d = d.plusYears(1);
        System.err.println(d);
        d = d.minusDays(15);
        System.out.println(d);

        System.out.println(d.toEpochDay()); // number of days since January 1, 1970.

        performAnimalEnrichment(LocalDate.of(2015, Month.JANUARY, 1), LocalDate.of(2015, Month.MARCH, 30), Period.ofMonths(1));
        performAnimalEnrichment(LocalDate.of(2015, Month.JANUARY, 1), LocalDate.of(2015, Month.MARCH, 30), Period.ofWeeks(1));

        Period wrong = Period.ofYears(1);
        wrong = Period.ofWeeks(7);
        System.out.println(wrong);

        // LocalTime
        System.out.println(LocalTime.now());
        LocalTime time3 =  LocalTime.of(6, 15, 30, 200);
        System.out.println(time3);

        // LocalDateTime
        System.out.println(LocalDateTime.now());
        LocalDateTime dateTime1 = LocalDateTime.of(2015, Month.JANUARY, 20, 6, 15, 30);
        System.out.println(dateTime1);
        LocalDateTime dateTime2 = LocalDateTime.of(d, time3);
        System.out.println(dateTime2);
        dateTime1 = dateTime1.minusMinutes(5);
        System.out.println(dateTime1);

        
        
        LocalDate date = LocalDate.of(2020, Month.FEBRUARY, 20);
        System.out.println(date.getDayOfWeek());     // MONDAY
        System.out.println(date.getMonth());          // FEBRUARY
        System.out.println(date.getYear());          // 2020
        System.out.println(date.getDayOfYear());     // 51

        // System.out.println(LocalDate.of(2015, Month.JANUARY, 32));

        // DateTimeFormatter

        System.out.println(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
