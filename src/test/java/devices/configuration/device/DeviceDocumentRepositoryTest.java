package devices.configuration.device;

import devices.configuration.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static devices.configuration.JsonAssert.assertThat;
import static devices.configuration.TestTransaction.transactional;

@IntegrationTest
@Transactional
class DeviceDocumentRepositoryTest {

    @Autowired
    DeviceDocumentRepository repository;
    final String deviceId = UUID.randomUUID().toString();

    @Test
    void findByDeviceId() {
        Device saved = DeviceFixture.someDevice(deviceId);
        transactional(() -> repository.save(saved));
        Optional<Device> read = transactional(() -> repository.findByDeviceId(deviceId));

        assertThat(read).isExactlyLike(saved);
    }
}
