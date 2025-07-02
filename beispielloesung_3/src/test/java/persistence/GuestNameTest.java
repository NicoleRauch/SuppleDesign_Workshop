package persistence;

import org.junit.jupiter.api.Test;
import service.HotelService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class GuestNameTest {

    @Test
    void guestNameMustBeProvided() {
        // WHEN
        Throwable t = catchThrowable(() -> new GuestName(null));

        // THEN
        assertThat(t).isInstanceOf(IllegalArgumentException.class);
    }

}
