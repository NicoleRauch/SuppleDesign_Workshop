package service;

import persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HotelService {

    private RoomRepository rooms;

    public HotelService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    public Amount requestRoom(ArrivalDate arrivalDate, DepartureDate departureDate) {
        for (Room room : rooms.getRooms().values()) {
            if (room.roomIsFree(arrivalDate, departureDate)) {
                return new Amount(100.0 * arrivalDate.daysUntil(departureDate.departureDate()));
            }
        }
        return null;
    }

    public void bookRoom(ArrivalDate arrivalDate, DepartureDate departureDate, GuestName guestName) {
        if (guestName == null) {
            throw new IllegalArgumentException("Guest name must be provided");
        }
        Booking booking = new Booking(arrivalDate, departureDate, guestName);
        for (Room room : rooms.getRooms().values()) {
            if (room.roomIsFree(arrivalDate, departureDate)) {
                room.getBookings().add(booking);
                rooms.save(room); // not needed here, but generally required for persistence
                return;
            }
        }
        throw new IllegalStateException("No rooms available on the given date(s)");
    }

    public List<RoomNumber> checkIn(GuestName guestName, ArrivalDate arrivalDate) {
        List<Room> roomsForGuest = rooms.findAllRoomsWithBookingsByGuestName(guestName);
        if (roomsForGuest.size() == 0) {
            throw new IllegalStateException("Guest cannot check in because they did not book a room");
        }
        List<RoomNumber> bookedRoomNumbers = new ArrayList<>();
        roomsForGuest.forEach(room -> {
            List<Booking> currentBookings = room.getBookings().stream()
                    .filter(booking -> booking.getGuestName().equals(guestName))
                    .filter(booking -> booking.getArrivalDate().equals(arrivalDate))
                    .toList();
            if (currentBookings.size() > 0) {
                currentBookings.forEach(booking -> booking.setCheckedIn(true));
                bookedRoomNumbers.add(room.getRoomNumber());
                rooms.save(room);
            }
        });
        return bookedRoomNumbers;
    }

    public void checkOut(GuestName guestName, RoomNumber roomNumber, DepartureDate departureDate) {
        Room room = rooms.getRooms().get(roomNumber);
        List<Booking> bookingsToCheckOut = room.getBookings().stream()
                .filter(booking -> Objects.equals(booking.getGuestName(), guestName))
                .filter(booking -> booking.getDepartureDate().equals(departureDate)).toList();
        if(bookingsToCheckOut.size() == 0){
            throw new IllegalStateException("No booking to be checked out!");
        }
        if(bookingsToCheckOut.size() > 1){
            throw new IllegalStateException("More than one booking found!");
        }
        Booking booking = bookingsToCheckOut.getFirst();
        if(!booking.isInvoiced()){
            throw new IllegalStateException("Checkout only possible for invoiced bookings.");
        }
        booking.setCheckedOut(true);
        rooms.save(room);
    }
}
