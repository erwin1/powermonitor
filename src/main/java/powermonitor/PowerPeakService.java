package powermonitor;

import powermonitor.homewizard.HomewizardClient;
import powermonitor.homewizard.Telegram;

import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PowerPeakService {
    public static final Logger LOGGER = Logger.getLogger(PowerPeakService.class.getName());

    private static final int minimum15MinPeak = 2500;

    private ZonedDateTime lastAlertTime;
    private ZonedDateTime currentPeakTimestamp;

    private HomewizardClient homewizardClient;
    private ScheduledExecutorService scheduler;
    private SlackService slackService;

    public PowerPeakService(HomewizardClient homewizardClient, SlackService slackService) {
        this.homewizardClient = homewizardClient;
        this.slackService = slackService;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::estimateNewPeak, 10, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    void estimateNewPeak() {
        LOGGER.log(Level.INFO, "Checking peak");
        try {
            Telegram telegram = homewizardClient.getTelegram();

            //Alert of new peak:
            if (this.currentPeakTimestamp != null) {
                if (!this.currentPeakTimestamp.equals(telegram.getMontlyPowerPeakTimestampAsDate())) {
                    //                notificationService.sendNotification("!!! New Peak: "+currentMonthPeak.getValue()+"W\n"+currentMonthPeak.getTimestamp());
                }
            }
            this.currentPeakTimestamp = telegram.getMontlyPowerPeakTimestampAsDate();


            //Alert of new peak estimate:
            if ((telegram.getTimestampAsDate().getMinute() - 1) % 15 == 0) {
                //do not estimate in first minute of the quarter
                return;
            }
            int currentPeakW = Math.max(telegram.getMontly_power_peak_w(), minimum15MinPeak);

            int estimatedPeakW = estimatePeakInCurrentPeriod(telegram);

            if (estimatedPeakW > currentPeakW) {
                ZonedDateTime now = ZonedDateTime.now();
                if (lastAlertTime == null || !isSame15minPeriod(now, lastAlertTime)) {
                    this.lastAlertTime = ZonedDateTime.now();
                    String message = MessageFormat.format(
                            "-= PEAK WARNING =-\n" +
                                    "Current power:         {0}W\n" +
                                    "Current period peak:   {1}W\n" +
                                    "Current month peak:    {3}W\n" +
                                    "Peak date:             {4}\n" +
                                    "Estimated new peak:    {5}W\n",
                            telegram.getActive_power_import_w(),
                            telegram.getActive_power_average_w(),
                            telegram.getMontly_power_peak_w(),
                            telegram.getMontlyPowerPeakTimestampAsDate(),
                            estimatedPeakW);
                    LOGGER.log(Level.SEVERE, message);
                    //notificationService.sendNotification(message);
                }
            }
        }catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error estimating new peak ", e);
            //notificationService.sendNotification(message);
        }
    }

    private boolean isSame15minPeriod(ZonedDateTime now, ZonedDateTime other) {
        if (now.truncatedTo(ChronoUnit.DAYS).equals(other.truncatedTo(ChronoUnit.DAYS))) {
            int x1 = (now.getHour() * 60 + now.getMinute()) / 15;
            int x2 = (other.getHour() * 60 + other.getMinute()) / 15;
            return x1 == x2;
        }
        return false;
    }

    public int estimatePeakInCurrentPeriod(Telegram telegram) {
        LOGGER.log(Level.INFO, "Estimate peak");

        ZonedDateTime now = LocalDateTime.parse(telegram.getTimestamp(), DateTimeFormatter.ofPattern("yyMMddHHmmss")).atZone(ZoneId.of("Europe/Brussels"));
        ZonedDateTime startOfPeriod = now.minusMinutes(now.getMinute() % 15).withSecond(0).withNano(0);
        int passedTime = (int) (now.toEpochSecond() - startOfPeriod.toEpochSecond());
        int remainingTime = (15 * 60) - passedTime;
        if (passedTime == 0) {
            return telegram.getActive_power_import_w();
        }
        if (remainingTime == 0) {
            return telegram.getActive_power_average_w();
        }

        double usageInPeriodWh = telegram.getActive_power_average_w() / 4;

        double estimatedTotalUsageInRemainingTimeWh = usageInPeriodWh * 1.0 / passedTime * remainingTime;

        double totalEstimatedUsageInPeriodWh = estimatedTotalUsageInRemainingTimeWh + usageInPeriodWh;

        int estimatedPeakInPeriodW = (int) (totalEstimatedUsageInPeriodWh * 4);

        LOGGER.log(Level.INFO, "Estimated peak in period: {0}W", estimatedPeakInPeriodW);

        return estimatedPeakInPeriodW;
    }

}
