package service;

import persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HotelService {

    private final RoomRepository rooms;

    public HotelService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    public Either<Error, Amount> requestRoom(ArrivalDate arrivalDate, DepartureDate departureDate) {
        for (Room room : rooms.getRooms().values()) {
            if (room.roomIsFree(arrivalDate, departureDate)) {
                return Either.ofResult(new Amount(100.0 * arrivalDate.daysUntil(departureDate.departureDate())));
            }
        }
        return Either.ofError(new Error("No available room found for the desired dates"));
    }

    public Either<Error, RoomNumber> bookRoom(BookingRequest bookingRequest) {
        if(bookingRequest == null){
            return Either.ofError(new Error("Booking request must be provided on booking!"));
        }
        for (Room room : rooms.getRooms().values()) {
            if (room.roomIsFree(bookingRequest.arrivalDate(), bookingRequest.departureDate())) {
                room.getBookings().add(new Booking(bookingRequest));
                rooms.save(room); // not needed here, but generally required for persistence
                return Either.ofResult(room.getRoomNumber());
            }
        }
        return Either.ofError(new Error("No rooms available on the given date(s)"));
    }

    public Either<Error, List<RoomNumber>> checkIn(GuestName guestName, ArrivalDate arrivalDate) {
        List<Room> roomsForGuest = rooms.findAllRoomsWithBookingsByGuestName(guestName);
        if (roomsForGuest.size() == 0) {
            return Either.ofError(new Error("Guest cannot check in because they did not book a room"));
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
        return Either.ofResult(bookedRoomNumbers);
    }

    public Either<Error, Booking> checkOut(GuestName guestName, RoomNumber roomNumber, DepartureDate departureDate) {
        Room room = rooms.getRooms().get(roomNumber);
        List<Booking> bookingsToCheckOut = room.getBookings().stream()
                .filter(booking -> Objects.equals(booking.getGuestName(), guestName))
                .filter(booking -> booking.getDepartureDate().equals(departureDate)).toList();
        if(bookingsToCheckOut.size() == 0){
            return Either.ofError(new Error("No booking to be checked out!"));
        }
        if(bookingsToCheckOut.size() > 1){
            return Either.ofError(new Error("More than one booking found!"));
        }
        Booking booking = bookingsToCheckOut.getFirst();
        if(!booking.isInvoiced()){
            return Either.ofError(new Error("Checkout only possible for invoiced bookings."));
        }
        booking.setCheckedOut(true);
        rooms.save(room);
        return Either.ofResult(booking);
    }
}
