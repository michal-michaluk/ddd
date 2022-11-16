package devices.configuration.device;

import devices.configuration.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static devices.configuration.JsonAssert.assertThat;
import static devices.configuration.TestTransaction.transactional;

@IntegrationTest
@Transactional
class DeviceNormalizingRepositoryTest {

    @Autowired
    DeviceNormalizingRepository repository;

    final String deviceId = UUID.randomUUID().toString();

    @Test
    void shouldSaveAndLoad() {
        Device saved = DeviceFixture.someDevice(deviceId);

        transactional(() -> repository.save(saved));
        var loaded = transactional(() -> repository.findByDeviceId(deviceId));

        assertThat(loaded).isExactlyLike(saved);
    }
}
