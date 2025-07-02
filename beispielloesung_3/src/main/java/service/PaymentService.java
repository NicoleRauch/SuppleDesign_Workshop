package service;

import persistence.*;

import java.util.*;
import java.util.stream.Collectors;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
        this.roomRepository = new RoomRepository();
        this.invoiceRepository = new InvoiceRepository();
    }

    public PaymentService(PaymentRepository paymentRepository, RoomRepository roomRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public void payAmount(GuestName guestName, Amount amount){
        List<Payment> guestPayments = paymentRepository.load(guestName);
        guestPayments.add(new Payment(guestName, amount));
        paymentRepository.save(guestName, guestPayments);
    }

    public Amount remainingCredit(GuestName guestName){
        return paymentRepository.load(guestName).stream()
                .map(payment -> payment.getPaidAmount().subtract(payment.getUsedAmount()))
                .reduce(Amount.ZERO, Amount::add);
    }

    public Either<Error,Invoice> produceInvoice(GuestName guestName, DepartureDate departureDate, List<RoomNumber> roomNumbers) {
        List<Room> bookedRooms = roomRepository.findAllRoomsWithBookingsByGuestName(guestName)
                .stream().filter(r -> roomNumbers.contains(r.getRoomNumber())).toList();
        Map<RoomNumber, List<Booking>> bookingsForRooms = new HashMap<>();
        bookedRooms.forEach(room -> {
            List<Booking> applicableBookings = room.getBookings().stream()
                    .filter(booking -> Objects.equals(booking.getGuestName(), guestName))
                    .filter(booking -> departureDate.isOnOrBefore(booking.getDepartureDate()))
                    .filter(booking -> !booking.isInvoiced())
                    .filter(Booking::isCheckedIn).collect(Collectors.toList());
            bookingsForRooms.put(room.getRoomNumber(), applicableBookings);
        });
        List<RoomNumber> roomsWithoutBookings = new ArrayList<>();
        bookingsForRooms.forEach(
                (roomNumber, bookings) -> {
                    if(bookings.isEmpty()){
                        roomsWithoutBookings.add(roomNumber);
                    }
                }
        );
        if (roomsWithoutBookings.size() > 0) {
            return Either.ofError(new Error(String.format("No bookings to be invoiced for given customer " +
                    "'%s', departureDate [%s] and roomNumbers %s", guestName.guestName(), departureDate, roomsWithoutBookings)));
        }
        Amount totalAmount =
                bookingsForRooms.values().stream()
                        .map(bookingsForRoom -> bookingsForRoom.stream()
                                .map(booking -> new Amount(100.0 * booking.numberOfDays()))
                                .reduce(Amount.ZERO, Amount::add)
                        ).reduce(Amount.ZERO, Amount::add);
        Amount credit = remainingCredit(guestName);
        if(totalAmount.isMoreThan(credit)){
            return Either.ofError(new Error("Payment insufficient. Necessary payment: " + (totalAmount.subtract(credit))));
        }

        List<Payment> payments = paymentRepository.load(guestName);
        payments.sort((o1, o2) -> o1.getPaymentDate().paymentDate().isEqual(o2.getPaymentDate().paymentDate()) ? 0 :
                        o1.getPaymentDate().paymentDate().isBefore(o2.getPaymentDate().paymentDate()) ? -1 : 1);
        Amount remainingTotalAmount = totalAmount;
        for (Payment payment: payments){
            if(remainingTotalAmount.isMoreThan(Amount.ZERO)){
                Amount remainingCreditForPayment = payment.getPaidAmount().subtract(payment.getUsedAmount());
                if(remainingCreditForPayment.isMoreThanOrEqual(remainingTotalAmount)){
                    payment.reduceCreditBy(remainingTotalAmount);
                    remainingTotalAmount = Amount.ZERO;
                    break;
                } else {
                    payment.reduceCreditBy(remainingCreditForPayment);
                    remainingTotalAmount = remainingTotalAmount.subtract(remainingCreditForPayment);
                }
            } else {
                break;
            }
        }
        paymentRepository.save(guestName, payments);

        roomRepository.markBookingsAsInvoiced(bookingsForRooms);

        Invoice invoice = new Invoice(new InvoiceId(UUID.randomUUID().toString()), guestName, bookingsForRooms, totalAmount);

        invoiceRepository.save(invoice);

        return Either.ofResult(invoice);
    }
}
