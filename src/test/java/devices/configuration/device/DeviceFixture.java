package devices.configuration.device;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class DeviceFixture {
    @NotNull
    public static Device someDevice(String deviceId) {
        return new Device(
                deviceId,
                new Ownership("Tauron", "krakow-public"),
                new Location(
                        "Pawia",
                        "21",
                        "Kraków",
                        "31-154",
                        null,
                        "PL",
                        new Location.Coordinates(
                                new BigDecimal("50.071886"),
                                new BigDecimal("19.945200")
                        )
                ),
                OpeningHours.alwaysOpen(),
                Settings.builder()
                        .publicAccess(true)
                        .showOnMap(true)
                        .build()

        );
    }
}
