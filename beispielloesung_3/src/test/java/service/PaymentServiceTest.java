package service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import persistence.*;

import java.util.ArrayList;
import java.util.List;

class PaymentServiceTest {
    private final GuestName guestName1 = new GuestName("Peter Meier");
    private final String guest2 = "Lisa Müller";
    private final RoomNumber roomNumber1 = new RoomNumber("1");
    private final RoomNumber roomNumber2 = new RoomNumber("2");

    public PaymentService setupPaymentService(PaymentRepository paymentRepository){
        return new PaymentService(paymentRepository);
    }

    public PaymentService setupPaymentService(PaymentRepository paymentRepository, RoomRepository roomRepository){
        return new PaymentService(paymentRepository, roomRepository, new InvoiceRepository());
    }

    @Test
    public void payAmount_guestPaidForTheFirstTime() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService service = setupPaymentService(paymentRepository);

        // WHEN
        service.payAmount(guestName1, new Amount(42.0));

        // THEN
        Assertions.assertThat(paymentRepository.load(guestName1)).hasSize(1);
        Assertions.assertThat(paymentRepository.load(guestName1).getFirst().getPaidAmount()).isEqualTo(new Amount(42.0));
    }

    @Test
    public void payAmount_guestPaidForTheSecondTime() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService service = setupPaymentService(paymentRepository);
        service.payAmount(guestName1, new Amount(42.0));

        // WHEN
        service.payAmount(guestName1, new Amount(120.0));

        // THEN
        Assertions.assertThat(paymentRepository.load(guestName1)).hasSize(2);
        Assertions.assertThat(paymentRepository.load(guestName1).getFirst().getPaidAmount()).isEqualTo(new Amount(42.0));
        Assertions.assertThat(paymentRepository.load(guestName1).get(1).getPaidAmount()).isEqualTo(new Amount(120.0));
    }

    @Test
    public void payAmount_secondGuestPaidForTheFirstTime() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService service = setupPaymentService(paymentRepository);
        service.payAmount(guestName1, new Amount(42.0));

        // WHEN
        service.payAmount(new GuestName(guest2), new Amount(120.0));

        // THEN
        Assertions.assertThat(paymentRepository.load(guestName1)).hasSize(1);
        Assertions.assertThat(paymentRepository.load(new GuestName(guest2))).hasSize(1);
        Assertions.assertThat(paymentRepository.load(guestName1).getFirst().getPaidAmount()).isEqualTo(new Amount(42.0));
        Assertions.assertThat(paymentRepository.load(new GuestName(guest2)).getFirst().getPaidAmount()).isEqualTo(new Amount(120.0));
    }

    @Test
    public void produceInvoice_noPayment() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);

        // WHEN
        Either<Error, Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isTrue();
        Assertions.assertThat(result.error().errorMessage()).contains("100.0");
    }

    @Test
    public void produceInvoice_paymentInsufficient() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(50.0));

        // WHEN
        Either<Error, Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isTrue();
        Assertions.assertThat(result.error().errorMessage()).contains("50.0");
    }

    @Test
    public void produceInvoice_oneRoomOneNight_withOldBooking() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        roomRepository.save(new Room(new RoomNumber("2"), new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(100.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(result.result().guestName()).isEqualTo(guestName1);
        Assertions.assertThat(result.result().totalAmount()).isEqualTo(new Amount(100.0));
        Assertions.assertThat(result.result().bookingsForRooms().size()).isEqualTo(1);
        Assertions.assertThat(result.result().bookingsForRooms().get(roomNumber1).size()).isEqualTo(1);
    }

    @Test
    public void produceInvoice_manyBookingsDifferentStartDaysSameEndDay() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        roomRepository.save(new Room(new RoomNumber("2"), new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);
        roomNumbers.add(roomNumber2);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate.minusDays(3), departureDate, guestName1).result());
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate.minusDays(3));
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(500.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(result.result().guestName()).isEqualTo(guestName1);
        Assertions.assertThat(result.result().totalAmount()).isEqualTo(new Amount(500.0));
        Assertions.assertThat(result.result().bookingsForRooms().size()).isEqualTo(2);
        Assertions.assertThat(result.result().bookingsForRooms().get(roomNumber1).size()).isEqualTo(1);
        Assertions.assertThat(result.result().bookingsForRooms().get(roomNumber2).size()).isEqualTo(1);
    }
    @Test
    public void produceInvoice_manyBookingsEndingOnInvoiceDayOrEarlier() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate.minusDays(1), departureDate.minusDays(1), guestName1).result());
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate.minusDays(1));
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(200.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(result.result().guestName()).isEqualTo(guestName1);
        Assertions.assertThat(result.result().totalAmount()).isEqualTo(new Amount(200.0));
        Assertions.assertThat(result.result().bookingsForRooms().size()).isEqualTo(1);
        Assertions.assertThat(result.result().bookingsForRooms().get(roomNumber1).size()).isEqualTo(2);
    }

    @Test
    public void produceInvoice_onePaymentIsMarkedAsUsed() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(100.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(service.remainingCredit(guestName1)).isEqualTo(Amount.ZERO);
    }

    @Test
    public void produceInvoice_twoPaymentsAreMarkedAsUsed() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(70.0));
        service.payAmount(guestName1, new Amount(30.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(service.remainingCredit(guestName1)).isEqualTo(Amount.ZERO);
    }

    @Test
    public void produceInvoice_onePaymentIsDeducted() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(170.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(service.remainingCredit(guestName1)).isEqualTo(new Amount(70.0));
    }

    @Test
    public void produceInvoice_twoPaymentsArePartiallyDeducted() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(70.0));
        service.payAmount(guestName1, new Amount(100.0));

        // WHEN
        Either<Error, Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(service.remainingCredit(guestName1)).isEqualTo(new Amount(70.0));
    }

    @Test
    public void produceInvoice_sameInvoiceTwiceLeadsToExcetionAlreadyPaid() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(70.0));
        service.payAmount(guestName1, new Amount(100.0));
        service.produceInvoice(guestName1, departureDate, roomNumbers);

        // WHEN
        Either<Error, Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isTrue();
        Assertions.assertThat(result.error().errorMessage())
                .isEqualTo(String.format("No bookings to be invoiced for given customer '%s', departureDate [%s] " +
                        "and roomNumbers %s", guestName1.guestName(), departureDate, roomNumbers));
        Assertions.assertThat(service.remainingCredit(guestName1)).isEqualTo(new Amount(70.0));
    }

    @Test
    public void markBookingsAsInvoiced_oneBooking() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(100.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(roomRepository.findAllBookingsByGuestName(guestName1))
                .extracting("invoiced").containsOnly(true);
    }

    @Test
    public void markBookingsAsInvoiced_twoBookingsInPast() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.bookRoom(BookingRequest.of(arrivalDate.minusDays(5), departureDate.minusDays(5), guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);
        hotelService.checkIn(guestName1, arrivalDate.minusDays(5));

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(200.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(roomRepository.findAllBookingsByGuestName(guestName1))
                .extracting("invoiced").containsOnly(true);
    }

    @Test
    public void markBookingsAsInvoiced_twoBookingsOneInPast() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room(roomNumber1, new ArrayList<>()));
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);
        List<RoomNumber> roomNumbers = new ArrayList<>();
        roomNumbers.add(roomNumber1);

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(BookingRequest.of(arrivalDate, departureDate, guestName1).result());
        hotelService.bookRoom(BookingRequest.of(arrivalDate.plusDays(5), departureDate.plusDays(5), guestName1).result());
        hotelService.checkIn(guestName1, arrivalDate);
        hotelService.checkIn(guestName1, arrivalDate.plusDays(5));

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(guestName1, new Amount(100.0));

        // WHEN
        Either<Error,Invoice> result = service.produceInvoice(guestName1, departureDate, roomNumbers);

        // THEN
        Assertions.assertThat(result.isError()).isFalse();
        Assertions.assertThat(roomRepository.findAllBookingsByGuestName(guestName1).size()).isEqualTo(2);
        Assertions.assertThat(roomRepository.findAllBookingsByGuestName(guestName1))
                .extracting("invoiced").containsExactly(true, false);
    }

}

