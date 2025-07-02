package persistence;

import java.time.LocalDate;

public class Payment {

    private double paidAmount;
    private double usedAmount;
    private String customerName;
    private LocalDate paymentDate;

    public Payment(String customerName, double paidAmount){
        this.customerName = customerName;
        this.paidAmount = paidAmount;
        this.paymentDate = LocalDate.now();
        this.usedAmount= 0.0;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public double getUsedAmount() {
        return usedAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void reduceCreditBy(double amount) {
        usedAmount += amount;
    }
}
