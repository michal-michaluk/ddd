package devices.configuration.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.Optional;

@Primary
@Repository
@AllArgsConstructor
class DeviceDocumentRepository implements DeviceRepository {

    private final JpaRepository repository;

    @Override
    public Page<Device> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(DeviceDocumentEntity::getDevice);
    }

    @Override
    public Optional<Device> findByDeviceId(String deviceId) {
        return repository.findById(deviceId)
                .map(DeviceDocumentEntity::getDevice);
    }

    @Override
    public void save(Device device) {
        DeviceDocumentEntity entity = repository.findById(device.deviceId)
                .orElseGet(() -> new DeviceDocumentEntity(device.deviceId));
        entity.setDevice(device);
        repository.save(entity);
    }

    @Repository
    interface JpaRepository extends PagingAndSortingRepository<DeviceDocumentEntity, String> {
    }

    @Getter
    @Entity
    @Table(name = "device_document")
    @NoArgsConstructor
    static class DeviceDocumentEntity {
        @Id
        private String deviceId;
        @Version
        private long version;

        @Setter
        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private Device device;

        DeviceDocumentEntity(String deviceId) {
            this.deviceId = deviceId;
        }
    }
}
