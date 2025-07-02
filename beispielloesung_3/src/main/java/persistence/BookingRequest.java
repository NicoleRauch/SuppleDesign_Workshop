package persistence;

import service.Either;
import service.Error;

import java.time.LocalDate;

public class BookingRequest{

    private final ArrivalDate arrivalDate;
    private final DepartureDate departureDate;
    private final GuestName guestName;

    public static Either<Error, BookingRequest> of(ArrivalDate arrivalDate, DepartureDate departureDate, GuestName guestName){
        if(arrivalDate == null) { return Either.ofError(new Error("Arrival date must be provided")); }
        if(departureDate == null) { return Either.ofError(new Error("Departure date must be provided")); }
        if(guestName == null) { return Either.ofError(new Error("Guest name must be provided")); }
        return Either.ofResult(new BookingRequest(arrivalDate, departureDate, guestName));
    }

    private BookingRequest(ArrivalDate arrivalDate, DepartureDate departureDate, GuestName guestName) {
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.guestName = guestName;
    }

    public ArrivalDate arrivalDate(){ return arrivalDate; }
    public DepartureDate departureDate(){ return departureDate; }
    public GuestName guestName(){ return guestName; }

    public boolean contains(LocalDate date) {
            return arrivalDate.isOnOrBefore(date) && departureDate.isAfter(date);
    }

    public long numberOfDays(){
        return arrivalDate.arrivalDate().datesUntil(departureDate.departureDate()).count();
    }

    public Booking confirm() {
        return new Booking(arrivalDate, departureDate, guestName);
    }
}
