package org.softwire.training.gasmon;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.gasmon.aws.AwsClientFactory;
import org.softwire.training.gasmon.config.Config;
import org.softwire.training.gasmon.model.Event;
import org.softwire.training.gasmon.model.Location;
import org.softwire.training.gasmon.receiver.QueueSubscription;
import org.softwire.training.gasmon.receiver.Receiver;
import org.softwire.training.gasmon.repository.S3Repository;
import org.softwire.training.gasmon.services.LocationsService;
import org.softwire.training.gasmon.services.EventsService;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            run();
        } catch (Throwable e) {
            LOG.error("Fatal error, terminating program", e);
            System.exit(1);
        }
    }

    private static void run() throws IOException {
        LOG.info("Starting to run...");

        Config config = new Config();

        AwsClientFactory awsClientFactory = new AwsClientFactory();
        AmazonSQS sqs = awsClientFactory.sqs();
        AmazonSNS sns = awsClientFactory.sns();
        AmazonS3 s3 = awsClientFactory.s3();

        S3Repository repository = new S3Repository(s3, config.locations.s3Bucket);
        LocationsService locationsService = new LocationsService(repository, config.locations.s3Key);
        List<Location> locations = locationsService.getValidLocations();

        EventsService eventsService = new EventsService();

        for (Location location : locations) {
            LOG.info("{}", location);
        }

        try (QueueSubscription queueSubscription = new QueueSubscription(sqs, sns, config.receiver.snsTopicArn)) {
            Receiver receiver = new Receiver(sqs, queueSubscription.getQueueUrl());
            long timestampWeLastShowedAnAverage = System.currentTimeMillis();
            long programmeStartTimeStamp = System.currentTimeMillis();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");

            while (System.currentTimeMillis() - programmeStartTimeStamp < 3_600_000) {
                eventsService.deleteEventsOlderThan10Minutes();
                List<Event> events = receiver.getEvents();
                for (Event event : events) {
                    if (locationsService.isValidLocation(event.getLocationId())) {
                        if (!eventsService.eventHasBeenSeen(event)) {
                            eventsService.addEvent(event);
                            LOG.info("{}", event);
                            if (System.currentTimeMillis() - timestampWeLastShowedAnAverage > 60000) {
                                Instant fiveMinutesAgoInstant = Instant.ofEpochMilli(System.currentTimeMillis());
                                LocalTime fiveMinutesAgo = fiveMinutesAgoInstant.atZone(ZoneId.systemDefault()).toLocalTime();
                                LocalDate dateOfReading = fiveMinutesAgoInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                                LOG.info("{}: The average reading between {} and {} was: {}", dateFormatter.format(dateOfReading), timeFormatter.format(fiveMinutesAgo.minusMinutes(1)), timeFormatter.format(fiveMinutesAgo), eventsService.averageValueOfEventsWithin1Minute());
                                timestampWeLastShowedAnAverage = System.currentTimeMillis();
                            }
                        } else {
                          LOG.info("Skipped duplicated message");
                        }
                    } else {
                        LOG.info("Skipped event with invalid location ID {}", event.getLocationId());
                    }
                }
            }
            LOG.info("{}", eventsService.averageValueOfLocation());
        }
    }
}
