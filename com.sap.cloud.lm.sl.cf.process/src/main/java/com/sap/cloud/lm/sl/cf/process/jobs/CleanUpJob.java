package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SafeExecutor;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@DisallowConcurrentExecution
public class CleanUpJob implements Job {

    public static final Marker LOG_MARKER = MarkerFactory.getMarker("clean-up-job");
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpJob.class);

    @Inject
    ApplicationConfiguration configuration;
    @Inject
    List<Cleaner> cleaners;
    private final SafeExecutor safeExecutor = new SafeExecutor(CleanUpJob::log);

    @Override
    public void execute(JobExecutionContext context) {
        LOGGER.info(LOG_MARKER, format(Messages.CLEAN_UP_JOB_STARTED_BY_APPLICATION_INSTANCE_0_AT_1,
                                       configuration.getApplicationInstanceIndex(), Instant.now()));

        Date expirationTime = computeExpirationTime();
        LOGGER.info(LOG_MARKER, format(Messages.WILL_CLEAN_UP_DATA_STORED_BEFORE_0, expirationTime));
        LOGGER.info(LOG_MARKER, format(Messages.REGISTERED_CLEANERS_IN_CLEAN_UP_JOB_0, cleaners));
        for (Cleaner cleaner : cleaners) {
            safeExecutor.execute(() -> cleaner.execute(expirationTime));
        }

        LOGGER.info(LOG_MARKER, format(Messages.CLEAN_UP_JOB_FINISHED_AT_0, Instant.now()));
    }

    private Date computeExpirationTime() {
        long maxTtlForOldData = configuration.getMaxTtlForOldData();
        return Date.from(Instant.now()
                                .minusSeconds(maxTtlForOldData));
    }

    private static void log(Exception e) {
        LOGGER.error(LOG_MARKER, format(Messages.ERROR_DURING_CLEAN_UP_0, e.getMessage()), e);
    }

}
