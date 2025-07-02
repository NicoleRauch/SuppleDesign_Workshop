package persistence;

import java.util.List;
import java.util.Map;

public class Invoice {

    private String customerName;
    private Map<String, List<BookingInterval>> bookingsForRooms;
    private double totalAmount;

    public Invoice(String customerName, Map<String, List<BookingInterval>> bookingsForRooms, double totalAmount) {
        this.customerName = customerName;
        this.bookingsForRooms = bookingsForRooms;
        this.totalAmount = totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Map<String, List<BookingInterval>> getBookingsForRooms() {
        return bookingsForRooms;
    }

    public double getTotalAmount() {
        return totalAmount;
    }
}
