package devices.configuration.device;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
class DeviceNormalizingRepository implements DeviceRepository {

    private final JpaRepository repository;

    @Override
    public Page<Device> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(DeviceEntity::getDevice);
    }

    @Override
    public Optional<Device> findByDeviceId(String deviceId) {
        return repository.findById(deviceId)
                .map(DeviceEntity::getDevice);
    }

    @Override
    public void save(Device device) {
        DeviceEntity entity = repository.findById(device.deviceId)
                .orElseGet(() -> new DeviceEntity(device.deviceId));
        entity.setDevice(device);
        repository.save(entity);
    }

    @Repository
    interface JpaRepository extends PagingAndSortingRepository<DeviceEntity, String> {
    }

    @Data
    @Entity
    @Table(name = "device")
    @NoArgsConstructor
    static class DeviceEntity {
        @Id
        @Column(name = "device_id")
        private String deviceId;
        @Version
        private Long version;

        private String operator;
        private String provider;

        private String street;
        private String houseNumber;
        private String city;
        private String postalCode;
        private String state;
        private String country;
        @Column(precision = 11, scale = 8)
        private BigDecimal longitude;
        @Column(precision = 11, scale = 8)
        private BigDecimal latitude;

        private boolean autoStart;
        private boolean remoteControl;
        private boolean billing;
        private boolean reimbursement;
        private boolean showOnMap;
        private boolean publicAccess;

        @OneToMany(orphanRemoval = true)
        @JoinColumn(name = "device_id", referencedColumnName = "device_id")
        private List<OpeningHoursEntity> openingHours;

        public DeviceEntity(String deviceId) {
            this.deviceId = deviceId;
        }

        Device getDevice() {
            return new Device(
                    deviceId,
                    new Ownership(operator, provider),
                    longitude == null ? null : new Location(
                            street,
                            houseNumber,
                            city,
                            postalCode,
                            state,
                            country,
                            new Location.Coordinates(longitude, latitude)
                    ),
                    openingHours(),
                    Settings.builder()
                            .autoStart(autoStart)
                            .remoteControl(remoteControl)
                            .billing(billing)
                            .reimbursement(reimbursement)
                            .showOnMap(showOnMap)
                            .publicAccess(publicAccess)
                            .build()
            );
        }

        void setDevice(Device device) {
            DeviceSnapshot dev = device.toSnapshot();
            this.operator = dev.ownership().operator();
            this.provider = dev.ownership().provider();

            this.street = dev.location() == null ? null : dev.location().street();
            this.houseNumber = dev.location() == null ? null : dev.location().houseNumber();
            this.city = dev.location() == null ? null : dev.location().city();
            this.postalCode = dev.location() == null ? null : dev.location().postalCode();
            this.state = dev.location() == null ? null : dev.location().state();
            this.country = dev.location() == null ? null : dev.location().country();
            this.longitude = dev.location() == null ? null : dev.location().coordinates().longitude();
            this.latitude = dev.location() == null ? null : dev.location().coordinates().latitude();

            this.autoStart = dev.settings().isAutoStart();
            this.remoteControl = dev.settings().isRemoteControl();
            this.billing = dev.settings().isBilling();
            this.reimbursement = dev.settings().isReimbursement();
            this.showOnMap = dev.settings().isShowOnMap();
            this.publicAccess = dev.settings().isPublicAccess();

            openingHours(dev.openingHours());
        }

        private List<OpeningHoursEntity> openingHours(OpeningHours openingHours) {
            if (openingHours.isAlwaysOpen()) {
                return List.of();
            }
            return List.of(
                    OpeningHoursEntity.of(deviceId, "monday", openingHours.getOpened().getMonday()),
                    OpeningHoursEntity.of(deviceId, "tuesday", openingHours.getOpened().getTuesday()),
                    OpeningHoursEntity.of(deviceId, "wednesday", openingHours.getOpened().getWednesday()),
                    OpeningHoursEntity.of(deviceId, "thursday", openingHours.getOpened().getThursday()),
                    OpeningHoursEntity.of(deviceId, "friday", openingHours.getOpened().getFriday()),
                    OpeningHoursEntity.of(deviceId, "saturday", openingHours.getOpened().getSaturday()),
                    OpeningHoursEntity.of(deviceId, "sunday", openingHours.getOpened().getSunday())
            );
        }

        private OpeningHours openingHours() {
            if (openingHours.isEmpty()) {
                return OpeningHours.alwaysOpen();
            } else {
                var week = openingHours.stream()
                        .collect(Collectors.toUnmodifiableMap(
                                OpeningHoursEntity::getDayOfWeek,
                                OpeningHoursEntity::toOpeningTime
                        ));
                return OpeningHours.openAt(
                        week.getOrDefault("monday", OpeningHours.OpeningTime.closed()),
                        week.getOrDefault("tuesday", OpeningHours.OpeningTime.closed()),
                        week.getOrDefault("wednesday", OpeningHours.OpeningTime.closed()),
                        week.getOrDefault("thursday", OpeningHours.OpeningTime.closed()),
                        week.getOrDefault("friday", OpeningHours.OpeningTime.closed()),
                        week.getOrDefault("saturday", OpeningHours.OpeningTime.closed()),
                        week.getOrDefault("sunday", OpeningHours.OpeningTime.closed())
                );
            }
        }
    }

    @Data
    @Entity
    @Table(name = "opening_hours")
    @NoArgsConstructor
    static class OpeningHoursEntity {
        @Id
        private Long id;
        @Column(name = "device_id")
        private String deviceId;
        private String dayOfWeek;
        private boolean open24h;
        private boolean closed;
        private Integer open;
        private Integer close;

        static OpeningHoursEntity of(String deviceId, String dayOfWeek, OpeningHours.OpeningTime openingTime) {
            OpeningHoursEntity entity = new OpeningHoursEntity();
            entity.deviceId = deviceId;
            entity.dayOfWeek = dayOfWeek;
            entity.open24h = openingTime.isOpen24h();
            entity.closed = openingTime.isClosed();
            entity.open = openingTime.getOpen() == null ? null : openingTime.getOpen().getHour();
            entity.close = openingTime.getClose() == null ? null : openingTime.getClose().getHour();
            return entity;
        }

        OpeningHours.OpeningTime toOpeningTime() {
            if (open24h) {
                return OpeningHours.OpeningTime.open24h();
            }
            if (closed) {
                return OpeningHours.OpeningTime.closed();
            }
            return OpeningHours.OpeningTime.opened(open, close);
        }
    }
}
