package service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import persistence.BookingInterval;
import persistence.PaymentRepository;
import persistence.Room;
import persistence.RoomRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class HotelServiceTest {

    public HotelService setupHotelService(int numberOfRooms) {
        RoomRepository rooms = new RoomRepository();
        for (int i = 1; i <= numberOfRooms; i++) {
            rooms.save(new Room(Integer.toString(i), new ArrayList<>()));
        }
        return new HotelService(rooms);
    }

    public RoomRepository setupRoomsWithOneRoomAndBookings(BookingInterval... bookingIntervals){
        RoomRepository rooms = new RoomRepository();
        rooms.save(new Room("1", new ArrayList<>(Arrays.asList(bookingIntervals))));
        return rooms;
    }

    @Test
    void requestRoom_roomAvailable() {
        // GIVEN
        HotelService service = setupHotelService(1);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);

        // WHEN
        Double price = service.requestRoom(startDate, endDate);

        // THEN
        assertThat(price).isEqualTo(100.0);
    }

    @Test
    void requestRoom_roomAvailableForMultipleNights() {
        // GIVEN
        HotelService service = setupHotelService(1);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);

        // WHEN
        Double price = service.requestRoom(startDate, endDate);

        // THEN
        assertThat(price).isEqualTo(200.0);
    }

    @Test
    void requestRoom_roomNotAvailable() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        HotelService service = new HotelService(setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate)));

        // WHEN
        Double price = service.requestRoom(startDate, endDate);

        // THEN
        assertThat(price).isNull();
    }

    @Test
    void requestRoom_roomAvailableAlthoughBookedOnDifferentDate() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        HotelService service = new HotelService(setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate.plusDays(5), endDate.plusDays(7))));

        // WHEN
        Double price = service.requestRoom(startDate, endDate);

        // THEN
        assertThat(price).isEqualTo(100.0);
    }

    @Test
    void bookRoom_bookingRequiresCustomerName() {
        HotelService service = setupHotelService(1);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);

        // WHEN
        Throwable t = catchThrowable(() -> service.bookRoom(startDate, endDate, null));

        // THEN
        assertThat(t).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bookRoom_roomAvailable() {
        // GIVEN
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings();
        HotelService service = new HotelService(rooms);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);

        // WHEN
        service.bookRoom(startDate, endDate, "Peter");

        // THEN
        List<BookingInterval> foundIntervals = rooms.findAllBookingIntervalsByCustomerName("Peter");
        assertThat(foundIntervals).hasSize(1);
        assertThat(foundIntervals.getFirst().getStartDate()).isEqualTo(startDate);
        assertThat(foundIntervals.getFirst().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void bookRoom_bookTwoRoomsForSameNights() {
        // GIVEN
        RoomRepository rooms = new RoomRepository();
        rooms.save(new Room("1", new ArrayList<>()));
        rooms.save(new Room("2", new ArrayList<>()));
        HotelService service = new HotelService(rooms);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);

        // WHEN
        service.bookRoom(startDate, endDate, "Peter");
        service.bookRoom(startDate, endDate, "Peter");

        // THEN
        List<Room> foundRooms = rooms.findAllRoomsWithBookingIntervalsByCustomerName("Peter");
        assertThat(foundRooms).hasSize(2);
        assertThat(foundRooms).extracting("roomNumber")
                        .containsExactly("1", "2");
        assertThat(foundRooms.get(0).getBookings().get(0).getStartDate()).isEqualTo(startDate);
        assertThat(foundRooms.get(0).getBookings().get(0).getEndDate()).isEqualTo(endDate);
        assertThat(foundRooms.get(1).getBookings().get(0).getStartDate()).isEqualTo(startDate);
        assertThat(foundRooms.get(1).getBookings().get(0).getEndDate()).isEqualTo(endDate);
    }

    @Test
    void bookRoom_roomAvailableForMultipleNights() {
        // GIVEN
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings();
        HotelService service = new HotelService(rooms);
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);

        // WHEN
        service.bookRoom(startDate, endDate, "Fred");

        // THEN
        List<BookingInterval> foundIntervals = rooms.findAllBookingIntervalsByCustomerName("Fred");
        assertThat(foundIntervals).hasSize(1);
        assertThat(foundIntervals.getFirst().getStartDate()).isEqualTo(startDate);
        assertThat(foundIntervals.getFirst().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void bookRoom_roomNotAvailable() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate));
        HotelService service = new HotelService(rooms);

        // WHEN
        Throwable t = catchThrowable(() -> service.bookRoom(startDate, endDate, "Jack"));

        // THEN
        List<BookingInterval> foundIntervals = rooms.findAllBookingIntervalsByCustomerName("Jack");
        assertThat(foundIntervals).hasSize(0);
        assertThat(t).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bookRoom_roomAvailableAlthoughBookedOnDifferentDate() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate.plusDays(5), endDate.plusDays(7)));
        HotelService service = new HotelService(rooms);

        // WHEN
        service.bookRoom(startDate, endDate, "Jim");

        // THEN
        List<BookingInterval> foundIntervals = rooms.findAllBookingIntervalsByCustomerName("Jim");
        assertThat(foundIntervals).hasSize(1);
        assertThat(foundIntervals.getFirst().getStartDate()).isEqualTo(startDate);
        assertThat(foundIntervals.getFirst().getEndDate()).isEqualTo(endDate);
    }

    @Test
    void checkIn_roomWasBooked() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate, "Fritz"));
        HotelService service = new HotelService(rooms);

        // WHEN
        List<String> checkedInRoomNumbers = service.checkIn("Fritz", startDate);

        // THEN
        assertThat(checkedInRoomNumbers.size()).isEqualTo(1);
        assertThat(checkedInRoomNumbers.getFirst()).isEqualTo("1");
    }

    @Test
    void checkIn_roomWasNotBooked() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings();
        HotelService service = new HotelService(rooms);

        // WHEN
        Throwable t = catchThrowable(() -> service.checkIn("Fritz", startDate));

        // THEN
        List<BookingInterval> foundIntervals = rooms.findAllBookingIntervalsByCustomerName("Fritz");
        assertThat(foundIntervals).hasSize(0);
        assertThat(t).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkIn_roomWasBookedOnDifferentDate() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate, "Fritz"));
        HotelService service = new HotelService(rooms);
        LocalDate checkInDate = startDate.plusDays(17);

        // WHEN
        List<String> checkedInRoomNumbers = service.checkIn("Fritz", checkInDate);

        // THEN
        assertThat(checkedInRoomNumbers.size()).isEqualTo(0);
    }

    @Test
    void checkOut_roomWasBooked_error() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate, "Fritz"));
        HotelService service = new HotelService(rooms);

        // WHEN
        Throwable t = catchThrowable(() -> service.checkOut("Fritz", "1", endDate));

        // THEN
        assertThat(t).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkOut_roomWasCheckedIn_error() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate, "Fritz"));
        HotelService service = new HotelService(rooms);
        service.checkIn("Fritz", startDate);

        // WHEN
        Throwable t = catchThrowable(() -> service.checkOut("Fritz", "1", endDate));

        // THEN
        assertThat(t).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkOut_roomWasInvoiced() {
        // GIVEN
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 12);
        RoomRepository rooms = setupRoomsWithOneRoomAndBookings(new BookingInterval(startDate,
                endDate, "Fritz"));
        HotelService service = new HotelService(rooms);
        service.checkIn("Fritz", startDate);

        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService paymentService = new PaymentService(paymentRepository, rooms);
        paymentService.payAmount("Fritz", 200.0);
        paymentService.produceInvoice("Fritz", endDate, Collections.singletonList("1"));

        // WHEN
        service.checkOut("Fritz", "1", endDate);

        // THEN
        Assertions.assertThat(rooms.getRooms().get("1").getBookings().getFirst().isCheckedOut()).isTrue();
    }

}
