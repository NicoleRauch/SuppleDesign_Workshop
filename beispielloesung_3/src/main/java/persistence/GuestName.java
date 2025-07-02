package persistence;

public record GuestName(String guestName) {

    public GuestName {
        if(guestName == null || guestName.isBlank()) { throw new IllegalArgumentException("guestName cannot be null"); }
    }
}
