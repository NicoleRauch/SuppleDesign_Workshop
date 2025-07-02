package persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentRepository {

    private final Map<String, List<Payment>> payments = new HashMap<>();

    public List<Payment> load(GuestName guestName){
        List<Payment> guestPayments = payments.get(guestName.guestName());
        return guestPayments == null ? new ArrayList<>() : guestPayments;
    }

    public void save(GuestName guestName, List<Payment> guestPayments){
        payments.put(guestName.guestName(), guestPayments);
    }
}
