package persistence;

import java.time.LocalDate;

public record ArrivalDate(LocalDate arrivalDate) {
    public ArrivalDate(int year, int month, int day){
        this(LocalDate.of(year, month, day));
    }
    public boolean isOnOrBefore(LocalDate date) {
        return arrivalDate.equals(date) || arrivalDate.isBefore(date);
    }

    public ArrivalDate plusDays(int i) {
        return new ArrivalDate(arrivalDate.plusDays(i));
    }

    public ArrivalDate minusDays(int i) {
        return new ArrivalDate(arrivalDate.minusDays(i));
    }

    public long daysUntil(LocalDate endExclusive) {
        return this.arrivalDate.datesUntil(endExclusive).count();
    }
}
