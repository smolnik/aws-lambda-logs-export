package cloud.developing.logs.aws;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateExportTaskRequest;
import com.amazonaws.services.logs.model.CreateExportTaskResult;
import com.amazonaws.services.logs.model.DescribeExportTasksRequest;

public class S3Export {

	private static final String DEST_BUCKET = getProperty("s3DestBucket");

	private static final String[] LOG_GROUPS = getProperty("logGroups").split(",");

	private static final AWSLogs logs = AWSLogsClientBuilder.standard().withRegion(getProperty("AWS_REGION")).build();

	private static final int MAX_ATTEMPTS_NUMBER = 18;

	private static final int WAIT_INTERVAL_IN_SEC = 15;

	public void handle(Context context) {
		LambdaLogger logger = context.getLogger();
		Instant to = Instant.now().truncatedTo(ChronoUnit.DAYS);
		Instant from = to.truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(1));
		logger.log("Logs exported From = " + from);
		logger.log("Logs exported To = " + to);
		for (String logGroup : LOG_GROUPS) {
			if (logGroup == null || logGroup.trim().isEmpty()) {
				continue;
			}
			logGroup = logGroup.trim();

			logger.log("Export for log group " + logGroup + " is about to start");
			CreateExportTaskResult result = logs.createExportTask(
					new CreateExportTaskRequest().withDestination(DEST_BUCKET).withDestinationPrefix(logGroup)
							.withLogGroupName(logGroup).withFrom(from.toEpochMilli()).withTo(to.toEpochMilli()));
			logger.log("Create export task result = " + result);
			String taskId = result.getTaskId();
			String statusCode = getStatusCode(taskId);
			logger.log("Export task status code = " + statusCode);
			int attemptCount = 1;
			while ((attemptCount <= MAX_ATTEMPTS_NUMBER) && (statusCode.equals("PENDING")
					|| statusCode.equals("PENDING_CANCEL") || statusCode.equals("RUNNING"))) {
				logger.log("Will wait until export task of id " + taskId + " is over or it timeouts");
				logger.log("Attempt count = " + attemptCount);
				try {
					TimeUnit.SECONDS.sleep(WAIT_INTERVAL_IN_SEC);
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
				statusCode = getStatusCode(taskId);
				logger.log("Export task status code = " + statusCode);
				attemptCount++;
			}
		}
	}

	private static String getStatusCode(String taskId) {
		return logs.describeExportTasks(new DescribeExportTasksRequest().withTaskId(taskId)).getExportTasks().get(0)
				.getStatus().getCode();
	}

	private static String getProperty(String key) {
		String propertyValue = System.getProperty(key);
		return propertyValue == null ? System.getenv(key) : propertyValue;
	}

}
