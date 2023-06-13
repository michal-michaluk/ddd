package devices.configuration.installations;

import devices.configuration.device.Location;
import devices.configuration.device.Ownership;
import devices.configuration.installations.InstallationProcessState.State;
import devices.configuration.protocols.BootNotification;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static devices.configuration.installations.DomainEvent.*;

@AllArgsConstructor
@RequiredArgsConstructor
class InstallationProcess {

    final List<DomainEvent> events = new ArrayList<>();
    String orderId;
    String deviceId;
    private Ownership ownership;
    private BootNotification boot;
    private Location location;
    private boolean bootConfirmation;
    private boolean finalized;

    static InstallationProcess startInstallationProcessFor(WorkOrder order) {
        Objects.requireNonNull(order.orderId());
        Objects.requireNonNull(order.ownership().operator());
        Objects.requireNonNull(order.ownership().provider());
        var event = new InstallationStarted(order.orderId(), order);
        var process = new InstallationProcess();
        process.handle(event);
        return process;
    }

    static InstallationProcess fromHistory(List<DomainEvent> history) {
        var process = new InstallationProcess();
        for (DomainEvent event : history)
            switch (event) {
                case InstallationStarted e -> process.handle(e);
                case DeviceAssigned e -> process.handle(e);
                case LocationPredefined e -> process.handle(e);
                case BootNotificationConfirmed e -> process.handle(e);
                case InstallationCompleted e -> process.handle(e);
            }
        return process;
    }

    private void handle(InstallationStarted event) {
        this.orderId = event.orderId();
        this.ownership = event.order().ownership();
    }

    void assignDevice(String deviceId) {
        Objects.requireNonNull(deviceId);
        ensureProcessIsActive();
        if (!Objects.equals(this.deviceId, deviceId)) {
            var event = new DeviceAssigned(orderId, deviceId);
            handle(event);
            events.add(event);
        }
    }

    private void handle(DeviceAssigned event) {
        this.deviceId = event.deviceId();
        boot = null;
        bootConfirmation = false;
    }

    void assignLocation(Location location) {
        Objects.requireNonNull(location);

        ensureProcessIsActive();
        var event = new LocationPredefined(orderId, deviceId, location);
        handle(event);
        events.add(event);
    }

    private void handle(LocationPredefined event) {
        this.location = event.location();
    }

    void handleBootNotification(BootNotification boot) {
        Objects.requireNonNull(boot);

        if (finalized) return;
        boolean bootConfirmed = Objects.equals(this.boot, boot) && this.bootConfirmation;
        var event = new BootNotificationConfirmed(orderId, deviceId, boot, bootConfirmed);
        handle(event);
        events.add(event);
    }

    private void handle(BootNotificationConfirmed event) {
        this.boot = event.boot();
        this.bootConfirmation = event.confirmed();
    }

    void confirmBootData() {
        if (bootConfirmation) return;
        this.bootConfirmation = true;
        var event = new BootNotificationConfirmed(orderId, deviceId, boot, bootConfirmation);
        events.add(event);
    }

    CompletionResult complete() {
        ensureProcessIsActive();
        boolean locationIsDefined = location != null;
        boolean bootIsConfirmed = boot != null && bootConfirmation;
        if (locationIsDefined && bootIsConfirmed) {
            var event = new InstallationCompleted(orderId, deviceId);
            handle(event);
            events.add(event);
        }
        return new CompletionResult(
                locationIsDefined,
                bootIsConfirmed,
                location,
                ownership
        );
    }

    private void handle(InstallationCompleted event) {
        finalized = true;
    }

    InstallationProcessState toSnapshot() {
        return new InstallationProcessState(
                orderId, deviceId, state()
        );
    }

    private State state() {
        if (finalized) return State.FINALIZED;
        if (bootConfirmation) return State.BOOTED;
        if (deviceId != null) return State.DEVICE_ASSIGNED;
        return State.PENDING;
    }

    private void ensureProcessIsActive() {
        if (finalized) throw new IllegalStateException();
    }
}