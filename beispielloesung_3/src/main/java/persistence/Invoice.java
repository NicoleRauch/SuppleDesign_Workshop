package persistence;

import java.util.List;
import java.util.Map;

public record Invoice(InvoiceId id, GuestName guestName, Map<RoomNumber, List<Booking>> bookingsForRooms, Amount totalAmount) {
}
