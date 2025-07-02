package service;

import persistence.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
        this.roomRepository = new RoomRepository();
    }

    public PaymentService(PaymentRepository paymentRepository, RoomRepository roomRepository) {
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
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

    public Invoice produceInvoice(GuestName guestName, DepartureDate departureDate, List<RoomNumber> roomNumbers) {
        List<Room> bookedRooms = roomRepository.findAllRoomsWithBookingsByGuestName(guestName)
                .stream().filter(r -> roomNumbers.contains(r.getRoomNumber())).collect(Collectors.toList());
        Map<RoomNumber, List<Booking>> bookingsForRooms = new HashMap<>();
        bookedRooms.forEach(room -> {
            List<Booking> applicableBookings = room.getBookings().stream()
                    .filter(booking -> Objects.equals(booking.getGuestName(), guestName))
                    .filter(booking -> departureDate.isOnOrBefore(booking.getDepartureDate()))
                    .filter(booking -> !booking.isInvoiced())
                    .filter(Booking::isCheckedIn).collect(Collectors.toList());
            if(applicableBookings.size() > 0 ){
                bookingsForRooms.put(room.getRoomNumber(), applicableBookings);
            } else {
                throw new IllegalArgumentException(String.format("No bookings to be invoiced for given customer " +
                        "'%s', departureDate [%s] and roomNumbers %s", guestName.guestName(), departureDate, roomNumbers));
            }
        });
        Amount totalAmount =
                bookingsForRooms.values().stream()
                        .map(bookingsForRoom -> bookingsForRoom.stream()
                                .map(booking -> new Amount(100.0 * booking.numberOfDays()))
                                .reduce(Amount.ZERO, Amount::add))
                        .reduce(Amount.ZERO, Amount::add);
        Amount credit = remainingCredit(guestName);
        if(totalAmount.isMoreThan(credit)){
            throw new IllegalStateException("Payment insufficient. Necessary payment: " + (totalAmount.subtract(credit)));
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

        return new Invoice(guestName, bookingsForRooms, totalAmount);
    }
}
