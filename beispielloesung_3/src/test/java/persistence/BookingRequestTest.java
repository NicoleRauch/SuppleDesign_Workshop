package persistence;

import org.junit.jupiter.api.Test;
import service.Either;
import service.Error;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingRequestTest {
    @Test
    void bookRoom_bookingRequiresGuestName() {
        ArrivalDate arrivalDate = new ArrivalDate(2020, 10, 10);
        DepartureDate departureDate = new DepartureDate(2020, 10, 11);

        // WHEN
        Either<Error, BookingRequest> result = BookingRequest.of(arrivalDate, departureDate, null);

        // THEN
        assertThat(result.isError()).isTrue();
        assertThat(result.error().errorMessage()).isEqualTo("Guest name must be provided");
    }


}
