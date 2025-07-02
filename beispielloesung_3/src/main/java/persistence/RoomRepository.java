package persistence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomRepository {

    private final Map<RoomNumber, Room> rooms = new ConcurrentHashMap<>();

    public void save(Room room) {
        rooms.put(room.getRoomNumber(), room);
    }

    public Map<RoomNumber, Room> getRooms() {
        return rooms;
    }

    public List<Room> findAllRoomsWithBookingsByGuestName(GuestName guestName) {
        Set<Room> rooms = new HashSet<>();
        for (Room room : this.rooms.values()) {
            for (Booking booking : room.getBookings()) {
                if (Objects.equals(booking.getGuestName(), guestName)) {
                    rooms.add(room);
                }
            }
        }
        return rooms.stream().toList();
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
        bookingsForRooms.forEach((roomNumber, bookingsForRoom) -> {
            Room room = rooms.get(roomNumber);
            room.getBookings().forEach(booking -> {
                if (listContainsBooking(bookingsForRoom, booking)) {
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
