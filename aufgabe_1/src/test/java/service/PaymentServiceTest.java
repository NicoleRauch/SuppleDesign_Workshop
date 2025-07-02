package service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import persistence.Invoice;
import persistence.PaymentRepository;
import persistence.Room;
import persistence.RoomRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.catchThrowable;

class PaymentServiceTest {
    String customer1 = "Peter Meier";
    String customer2 = "Lisa Müller";

    public PaymentService setupPaymentService(PaymentRepository paymentRepository){
        return new PaymentService(paymentRepository);
    }

    public PaymentService setupPaymentService(PaymentRepository paymentRepository, RoomRepository roomRepository){
        return new PaymentService(paymentRepository, roomRepository);
    }

    @Test
    public void payAmount_customerPaidForTheFirstTime() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService service = setupPaymentService(paymentRepository);

        // WHEN
        service.payAmount(customer1, 42.0);

        // THEN
        Assertions.assertThat(paymentRepository.load(customer1)).hasSize(1);
        Assertions.assertThat(paymentRepository.load(customer1).getFirst().getPaidAmount()).isEqualTo(42.0);
    }

    @Test
    public void payAmount_customerPaidForTheSecondTime() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService service = setupPaymentService(paymentRepository);
        service.payAmount(customer1, 42.0);

        // WHEN
        service.payAmount(customer1, 120.0);

        // THEN
        Assertions.assertThat(paymentRepository.load(customer1)).hasSize(2);
        Assertions.assertThat(paymentRepository.load(customer1).getFirst().getPaidAmount()).isEqualTo(42.0);
        Assertions.assertThat(paymentRepository.load(customer1).get(1).getPaidAmount()).isEqualTo(120.0);
    }

    @Test
    public void payAmount_secondCustomerPaidForTheFirstTime() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService service = setupPaymentService(paymentRepository);
        service.payAmount(customer1, 42.0);

        // WHEN
        service.payAmount(customer2, 120.0);

        // THEN
        Assertions.assertThat(paymentRepository.load(customer1)).hasSize(1);
        Assertions.assertThat(paymentRepository.load(customer2)).hasSize(1);
        Assertions.assertThat(paymentRepository.load(customer1).getFirst().getPaidAmount()).isEqualTo(42.0);
        Assertions.assertThat(paymentRepository.load(customer2).getFirst().getPaidAmount()).isEqualTo(120.0);
    }

    @Test
    public void produceInvoice_noPayment() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);

        // WHEN
        Throwable t = catchThrowable(() -> service.produceInvoice(customer1, endDate, roomNumbers));

        // THEN
        Assertions.assertThat(t).isInstanceOf(IllegalStateException.class);
        Assertions.assertThat(t.getMessage()).contains("100.0");
    }

    @Test
    public void produceInvoice_paymentInsufficient() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 50.0);

        // WHEN
        Throwable t = catchThrowable(() -> service.produceInvoice(customer1, endDate, roomNumbers));

        // THEN
        Assertions.assertThat(t).isInstanceOf(IllegalStateException.class);
        Assertions.assertThat(t.getMessage()).contains("50.0");
    }

    @Test
    public void produceInvoice_oneRoomOneNight_withOldBooking() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        roomRepository.save(new Room("2", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 100.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(invoice.getCustomerName()).isEqualTo(customer1);
        Assertions.assertThat(invoice.getTotalAmount()).isEqualTo(100.0);
        Assertions.assertThat(invoice.getBookingsForRooms().get("1").size()).isEqualTo(1);
    }

    @Test
    public void produceInvoice_manyBookingsDifferentStartDaysSameEndDay() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        roomRepository.save(new Room("2", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");
        roomNumbers.add("2");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate.minusDays(3), endDate, customer1);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate.minusDays(3));
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 500.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(invoice.getCustomerName()).isEqualTo(customer1);
        Assertions.assertThat(invoice.getTotalAmount()).isEqualTo(500.0);
        Assertions.assertThat(invoice.getBookingsForRooms().get("1").size()).isEqualTo(1);
        Assertions.assertThat(invoice.getBookingsForRooms().get("2").size()).isEqualTo(1);
    }
    @Test
    public void produceInvoice_manyBookingsEndingOnInvoiceDayOrEarlier() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate.minusDays(1), endDate.minusDays(1), customer1);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate.minusDays(1));
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 200.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(invoice.getCustomerName()).isEqualTo(customer1);
        Assertions.assertThat(invoice.getTotalAmount()).isEqualTo(200.0);
        Assertions.assertThat(invoice.getBookingsForRooms().get("1").size()).isEqualTo(2);
    }

    @Test
    public void produceInvoice_onePaymentIsMarkedAsUsed() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 100.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(service.remainingCredit(customer1)).isEqualTo(0.0);
    }

    @Test
    public void produceInvoice_twoPaymentsAreMarkedAsUsed() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 70.0);
        service.payAmount(customer1, 30.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(service.remainingCredit(customer1)).isEqualTo(0.0);
    }

    @Test
    public void produceInvoice_onePaymentIsDeducted() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 170.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(service.remainingCredit(customer1)).isEqualTo(70.0);
    }

    @Test
    public void produceInvoice_twoPaymentsArePartiallyDeducted() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 70.0);
        service.payAmount(customer1, 100.0);

        // WHEN
        service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(service.remainingCredit(customer1)).isEqualTo(70.0);
    }

    @Test
    public void produceInvoice_sameInvoiceTwiceLeadsToExcetionAlreadyPaid() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 70.0);
        service.payAmount(customer1, 100.0);
        service.produceInvoice(customer1, endDate, roomNumbers);

        // WHEN
        Throwable throwable = catchThrowable(() -> service.produceInvoice(customer1, endDate, roomNumbers));

        // THEN
        Assertions.assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThat(throwable.getMessage())
                .isEqualTo(String.format("No bookingIntervals to be invoiced for given customer '%s', endDate [%s] " +
                        "and roomNumbers %s", customer1, endDate, roomNumbers));
        Assertions.assertThat(service.remainingCredit(customer1)).isEqualTo(70.0);
    }

    @Test
    public void markBookingsAsInvoiced_oneBooking() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.checkIn(customer1, startDate);

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 100.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1))
                .extracting("invoiced").containsOnly(true);
    }

    @Test
    public void markBookingsAsInvoiced_twoBookingsInPast() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.bookRoom(startDate.minusDays(5), endDate.minusDays(5), customer1);
        hotelService.checkIn(customer1, startDate);
        hotelService.checkIn(customer1, startDate.minusDays(5));

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 200.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1))
                .extracting("invoiced").containsOnly(true);
    }

    @Test
    public void markBookingsAsInvoiced_twoBookingsOneInPast() {
        // GIVEN
        PaymentRepository paymentRepository = new PaymentRepository();
        RoomRepository roomRepository = new RoomRepository();
        roomRepository.save(new Room("1", new ArrayList<>()));
        LocalDate startDate = LocalDate.of(2020, 10, 10);
        LocalDate endDate = LocalDate.of(2020, 10, 11);
        List<String> roomNumbers = new ArrayList<>();
        roomNumbers.add("1");

        HotelService hotelService = new HotelService(roomRepository);
        hotelService.bookRoom(startDate, endDate, customer1);
        hotelService.bookRoom(startDate.plusDays(5), endDate.plusDays(5), customer1);
        hotelService.checkIn(customer1, startDate);
        hotelService.checkIn(customer1, startDate.plusDays(5));

        PaymentService service = setupPaymentService(paymentRepository, roomRepository);
        service.payAmount(customer1, 100.0);

        // WHEN
        Invoice invoice = service.produceInvoice(customer1, endDate, roomNumbers);

        // THEN
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1).size()).isEqualTo(2);
        Assertions.assertThat(roomRepository.findAllBookingIntervalsByCustomerName(customer1))
                .extracting("invoiced").containsExactly(true, false);
    }

}

