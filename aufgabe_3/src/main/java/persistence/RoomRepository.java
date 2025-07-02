package persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RoomRepository {

    private Map<RoomNumber, Room> rooms = new ConcurrentHashMap<>();

    public void save(Room room) {
        rooms.put(room.getRoomNumber(), room);
    }

    public Map<RoomNumber, Room> getRooms() {
        return rooms;
    }

    public List<Room> findAllRoomsWithBookingsByGuestName(GuestName guestName) {
        List<Room> rooms = new ArrayList<>();
        for (Room room : this.rooms.values()) {
            for (Booking booking : room.getBookings()) {
                if (Objects.equals(booking.getGuestName(), guestName)) {
                    rooms.add(room);
                }
            }
        }
        return rooms;
    }

    public List<Booking> findAllBookingsByGuestName(GuestName guestName) {
        List<Booking> bookings = new ArrayList<>();
        for (Room room : rooms.values()) {
            for (Booking booking : room.getBookings()) {
                if (Objects.equals(booking.getGuestName(), guestName)) {
                    bookings.add(booking);
                }
            }
        }
        return bookings;
    }

    public void markBookingsAsInvoiced(Map<RoomNumber, List<Booking>> bookingsForRooms) {
        bookingsForRooms.keySet().forEach(roomNumber -> {
            Room room = rooms.get(roomNumber);
            room.getBookings().forEach(booking -> {
                if (listContainsBooking(bookingsForRooms.get(roomNumber), booking)) {
                    booking.setInvoiced(true);
                }
            });
            save(room);
        });
    }

    private boolean listContainsBooking(List<Booking> bookings, Booking booking) {
        for (Booking aBooking : bookings) {
            if (aBooking.getGuestName().equals(booking.getGuestName()) && aBooking.getArrivalDate().equals(booking.getArrivalDate())) {
                return true;
            }
        }
        return false;
    }
}
