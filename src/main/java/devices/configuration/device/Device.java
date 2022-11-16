package devices.configuration.device;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class Device {
    final String deviceId;
    private Ownership ownership;
    private Location location;
    private OpeningHours openingHours;
    private Settings settings;

    static Device create(String deviceId) {
        return new Device(
                deviceId,
                Ownership.unowned(),
                null,
                OpeningHours.alwaysOpen(),
                Settings.defaultSettings()
        );
    }

    void assignTo(Ownership ownership) {
        this.ownership = ownership;
    }

    void updateLocation(Location location) {
        this.location = location;
    }

    void updateOpeningHours(OpeningHours openingHours) {
        this.openingHours = openingHours;
    }

    void updateSettings(Settings settings) {
        this.settings = this.settings.merge(settings);
    }

    private Violations checkViolations() {
        return Violations.builder()
                .operatorNotAssigned(ownership == null || ownership.operator() == null)
                .providerNotAssigned(ownership == null || ownership.provider() == null)
                .locationMissing(location == null)
                .showOnMapButMissingLocation(settings.isShowOnMap() && location == null)
                .showOnMapButNoPublicAccess(settings.isShowOnMap() && !settings.isPublicAccess())
                .build();
    }

    DeviceSnapshot toSnapshot() {
        Violations violations = checkViolations();
        Visibility visibility = Visibility.basedOn(
                violations.isValid() && settings.isPublicAccess(),
                settings.isShowOnMap()
        );
        return new DeviceSnapshot(
                deviceId,
                ownership,
                location,
                openingHours,
                settings,
                violations,
                visibility
        );
    }
}
