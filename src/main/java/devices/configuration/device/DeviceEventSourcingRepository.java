package devices.configuration.device;

import devices.configuration.tools.EventTypes;
import devices.configuration.tools.LastEvents;
import devices.configuration.tools.LegacyDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static devices.configuration.device.DomainEvent.*;

@Repository
@AllArgsConstructor
class DeviceEventSourcingRepository implements DeviceRepository {

    private final EventRepository repository;
    private final ApplicationEventPublisher publisher;

    @Override
    public Optional<Device> findByDeviceId(String deviceId) {
        List<DomainEvent> history = repository.findByDeviceId(deviceId).stream()
                .map(DeviceEventEntity::getEvent)
                .map(LegacyDomainEvent::normalise)
                .toList();
        if (history.isEmpty()) {
            return Optional.empty();
        }
        LastEvents events = LastEvents.fromHistoryOf(history);
        Device device = new Device(deviceId, new ArrayList<>(),
                events.getOrNull(OwnershipUpdated.class, OwnershipUpdated::ownership),
                events.getOrNull(LocationUpdated.class, LocationUpdated::location),
                events.getOrDefault(OpeningHoursUpdated.class, OpeningHoursUpdated::openingHours, OpeningHours.alwaysOpen()),
                events.getOrDefault(SettingsUpdated.class, SettingsUpdated::settings, Settings.defaultSettings())
        );
        return Optional.of(device);
    }

    @Override
    public Page<Device> findAll(Pageable pageable) {
        return Page.empty();
    }

    @Override
    public void save(Device device) {
        device.events.forEach(event -> {
            repository.save(new DeviceEventEntity(
                    device.deviceId,
                    EventTypes.of(event),
                    event));
            publisher.publishEvent(event);
        });
        if (!device.events.isEmpty()) {
            publisher.publishEvent(device.toSnapshot());
        }
    }

    @Repository
    interface EventRepository extends CrudRepository<DeviceEventEntity, UUID> {
        @Query(value = """
                select distinct on (type) *
                 from device_events
                 where device_id = :deviceId
                 order by type, time desc
                """, nativeQuery = true)
        List<DeviceEventEntity> findByDeviceId(String deviceId);
    }

    @Data
    @Entity
    @Table(name = "device_events")
    @NoArgsConstructor
    static class DeviceEventEntity {
        @Id
        private UUID id;
        private String deviceId;
        private String type;
        private Instant time;
        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private DomainEvent event;

        DeviceEventEntity(String deviceId, EventTypes.Type type, DomainEvent event) {
            this.id = UUID.randomUUID();
            this.deviceId = deviceId;
            this.type = type.type();
            this.time = Instant.now();
            this.event = event;
        }
    }

    public static class LegacyEvents {

        public record OwnershipUpdatedV1(
                String deviceId,
                String operator,
                String provider) implements LegacyDomainEvent {
            @Override
            public DomainEvent normalise() {
                return new OwnershipUpdated(deviceId,
                        new Ownership(operator, provider)
                );
            }
        }

    }
}
