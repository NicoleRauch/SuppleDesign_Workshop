package persistence;

import java.util.List;
import java.util.Map;

public class Invoice {

    private GuestName guestName;
    private Map<RoomNumber, List<Booking>> bookingsForRooms;
    private Amount totalAmount;

    public Invoice(GuestName guestName, Map<RoomNumber, List<Booking>> bookingsForRooms, Amount totalAmount) {
        this.guestName = guestName;
        this.bookingsForRooms = bookingsForRooms;
        this.totalAmount = totalAmount;
    }

    public GuestName getGuestName() {
        return guestName;
    }

    public Map<RoomNumber, List<Booking>> getBookingsForRooms() {
        return bookingsForRooms;
    }

    public Amount getTotalAmount() {
        return totalAmount;
    }
}
