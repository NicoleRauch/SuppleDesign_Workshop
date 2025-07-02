package persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceRepository {

    private final Map<InvoiceId, Invoice> invoices = new HashMap<>();

    public List<Invoice> loadFor(GuestName guestName){
        return invoices.values().stream()
                .filter(invoice -> invoice.guestName().equals(guestName))
                .toList();
    }

    public Invoice getFor(InvoiceId invoiceId){
        return invoices.get(invoiceId);
    }

    public void save(Invoice invoice){
        invoices.put(invoice.id(), invoice);
    }
}
