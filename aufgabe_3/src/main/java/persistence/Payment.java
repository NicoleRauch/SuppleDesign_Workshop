package persistence;

import java.time.LocalDate;

public class Payment {

    private Amount paidAmount;
    private Amount usedAmount;
    private GuestName guestName;
    private PaymentDate paymentDate;

    public Payment(GuestName guestName, Amount paidAmount){
        this.guestName = guestName;
        this.paidAmount = paidAmount;
        this.paymentDate = new PaymentDate(LocalDate.now());
        this.usedAmount= new Amount(0.0);
    }

    public Amount getPaidAmount() {
        return paidAmount;
    }

    public Amount getUsedAmount() {
        return usedAmount;
    }

    public GuestName getGuestName() {
        return guestName;
    }

    public PaymentDate getPaymentDate() {
        return paymentDate;
    }

    public void reduceCreditBy(Amount amount) {
        usedAmount = usedAmount.add(amount);
    }
}
