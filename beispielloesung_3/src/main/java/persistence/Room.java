package persistence;

import java.time.LocalDate;
import java.util.List;

public class Room {

    private final RoomNumber roomNumber;
    private final List<Booking> bookings;

    public Room(RoomNumber roomNumber, List<Booking> bookings) {
        this.roomNumber = roomNumber;
        this.bookings = bookings;
    }

    public RoomNumber getRoomNumber() {
        return roomNumber;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    private boolean dateIsFree(LocalDate date) {
        for (Booking booking : bookings) {
            if (booking.contains(date)) {
                return false;
            }
        }
        return true;
    }

    public boolean roomIsFree(ArrivalDate arrivalDate, DepartureDate departureDate) {
        for (LocalDate date : arrivalDate.arrivalDate().datesUntil(departureDate.departureDate()).toList()) {
            if (!dateIsFree(date)) {
                return false;
            }
        }
        return true;
    }
}
