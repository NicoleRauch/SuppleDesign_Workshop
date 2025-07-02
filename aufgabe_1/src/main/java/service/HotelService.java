package service;

import persistence.BookingInterval;
import persistence.Room;
import persistence.RoomRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HotelService {

    private RoomRepository rooms;

    public HotelService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    public Double requestRoom(LocalDate startDate, LocalDate endDate) {
        for (Room room : rooms.getRooms().values()) {
            BookingInterval bookingInterval = new BookingInterval(startDate, endDate);
            if (room.roomIsFree(bookingInterval)) {
                return 100.0 * bookingInterval.dates().size();
            }
        }
        return null;
    }

    public void bookRoom(LocalDate startDate, LocalDate endDate, String customerName) {
        if (customerName == null) {
            throw new IllegalArgumentException("Customer name must not be null");
        }
        for (Room room : rooms.getRooms().values()) {
            BookingInterval bookingInterval = new BookingInterval(startDate, endDate, customerName);
            if (room.roomIsFree(bookingInterval)) {
                room.getBookings().add(bookingInterval);
                rooms.save(room); // not needed here, but generally required for persistence
                return;
            }
        }
        throw new IllegalStateException("No rooms available on the given date(s)");
    }

    public List<String> checkIn(String customerName, LocalDate startDate) {
        List<Room> roomsForCustomer = rooms.findAllRoomsWithBookingIntervalsByCustomerName(customerName);
        if (roomsForCustomer.size() == 0) {
            throw new IllegalStateException("Customer cannot check in because they did not book a room");
        }
        List<String> bookedRoomNumbers = new ArrayList<>();
        roomsForCustomer.forEach(room -> {
            List<BookingInterval> currentBookings = room.getBookings().stream()
                    .filter(interval -> interval.getCustomerName().equals(customerName))
                    .filter(interval -> interval.getStartDate().equals(startDate))
                    .toList();
            if (currentBookings.size() > 0) {
                currentBookings.forEach(interval -> interval.setCheckedIn(true));
                bookedRoomNumbers.add(room.getRoomNumber());
                rooms.save(room);
            }
        });
        return bookedRoomNumbers;
    }

    public void checkOut(String customerName, String roomNumber, LocalDate endDate) {
        Room room = rooms.getRooms().get(roomNumber);
        List<BookingInterval> bookingsToCheckOut = room.getBookings().stream()
                .filter(interval -> Objects.equals(interval.getCustomerName(), customerName))
                .filter(interval -> interval.getEndDate().equals(endDate)).toList();
        if(bookingsToCheckOut.size() == 0){
            throw new IllegalStateException("No booking to be checked out!");
        }
        if(bookingsToCheckOut.size() > 1){
            throw new IllegalStateException("More than one booking found!");
        }
        BookingInterval booking = bookingsToCheckOut.getFirst();
        if(!booking.isInvoiced()){
            throw new IllegalStateException("Checkout only possible for invoiced bookings.");
        }
        booking.setCheckedOut(true);
        rooms.save(room);
    }
}
