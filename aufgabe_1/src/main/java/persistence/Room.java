package persistence;

import java.time.LocalDate;
import java.util.List;

public class Room {

    private String roomNumber;
    private List<BookingInterval> bookings;

    public Room(String roomNumber, List<BookingInterval> bookings) {
        this.roomNumber = roomNumber;
        this.bookings = bookings;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public List<BookingInterval> getBookings() {
        return bookings;
    }

    private boolean dateIsFree(LocalDate date) {
        for (BookingInterval booking : bookings) {
            if (booking.contains(date)) {
                return false;
            }
        }
        return true;
    }

    public boolean roomIsFree(BookingInterval interval) {
        for (LocalDate date : interval.dates()) {
            if (!dateIsFree(date)) {
                return false;
            }
        }
        return true;
    }
}
