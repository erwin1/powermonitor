package powermonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import powermonitor.homewizard.Telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerPeakServiceTest {

    private PowerPeakService service;

    @BeforeEach
    void setUp() {
        service = new PowerPeakService(null, null);
    }

    private void assertPeakEstimation(String timestamp, int averagePower, int importPower, int expectedPeak) {
        var telegram = new Telegram();
        telegram.setTimestamp(timestamp);
        telegram.setActive_power_average_w(averagePower);
        telegram.setActive_power_import_w(importPower);

        // Act
        int estimatedPeak = service.estimatePeakInCurrentPeriod(telegram);

        // Assert
        assertEquals(expectedPeak, estimatedPeak, "Estimated peak for timestamp " + timestamp);
    }

    @Test
    void estimatePeak_atStartOfPeriod_returnsImportPower() {
        assertPeakEstimation("240115120000", 1000, 500, 500);
    }

    @Test
    void estimatePeak_inFirstQuarter_halfway() {
        // 7 minutes and 30 seconds into the quarter (450s passed)
        // 1000W average should result in 2000W estimate (1000 * 900 / 450)
        assertPeakEstimation("240115120730", 1000, 0, 2000);
    }

    @Test
    void estimatePeak_inSecondQuarter_oneThird() {
        // 5 minutes into the quarter (300s passed)
        // 1000W average should result in 3000W estimate (1000 * 900 / 300)
        assertPeakEstimation("240115122000", 1000, 0, 3000);
    }

    @Test
    void estimatePeak_inThirdQuarter_nearEnd() {
        // 14 minutes and 59 seconds into the quarter (899s passed)
        // 1000W average should result in ~1001W estimate (1000 * 900 / 899)
        assertPeakEstimation("240115124459", 1000, 0, 1001);
    }

    @Test
    void estimatePeak_inFourthQuarter_nearStart() {
        // 1 second into the quarter (1s passed)
        // 1000W average should result in 900000W estimate (1000 * 900 / 1)
        assertPeakEstimation("240115124501", 1000, 0, 900000);
    }
}
