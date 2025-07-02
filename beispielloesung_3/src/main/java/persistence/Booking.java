package persistence;

import java.time.LocalDate;

public class Booking {

    private final ArrivalDate arrivalDate;
    private final DepartureDate departureDate;
    private final GuestName guestName;
    private boolean invoiced = false;
    private boolean checkedIn = false;
    private boolean checkedOut = false;

    public Booking(ArrivalDate arrivalDate, DepartureDate departureDate, GuestName guestName) {
        if(arrivalDate == null) { throw new IllegalArgumentException("ArrivalDate must be provided"); }
        if(departureDate == null) { throw new IllegalArgumentException("DepartureDate must be provided"); }
        if(guestName == null) { throw new IllegalArgumentException("GuestName must be provided"); }
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.guestName = guestName;
    }

    public Booking(BookingRequest request){
        this(request.arrivalDate(), request.departureDate(), request.guestName());
    }

    public boolean contains(LocalDate date) {
            return arrivalDate.isOnOrBefore(date) && departureDate.isAfter(date);
    }

    public long numberOfDays(){
        return arrivalDate.arrivalDate().datesUntil(departureDate.departureDate()).count();
    }

    public ArrivalDate getArrivalDate() {
        return arrivalDate;
    }

    public DepartureDate getDepartureDate() {
        return departureDate;
    }
    public GuestName getGuestName() {
        return guestName;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public void setInvoiced(boolean invoiced) {
        this.invoiced = invoiced;
    }

    public boolean isInvoiced() {
        return invoiced;
    }

    public void setCheckedOut(boolean checkedOut) {
        this.checkedOut = checkedOut;
    }

    public boolean isCheckedOut() {
        return this.checkedOut;
    }
}
