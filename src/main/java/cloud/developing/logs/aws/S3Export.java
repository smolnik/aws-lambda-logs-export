package cloud.developing.logs.aws;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.time.Duration.ofDays;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Instant;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateExportTaskRequest;
import com.amazonaws.services.logs.model.CreateExportTaskResult;
import com.amazonaws.services.logs.model.DescribeExportTasksRequest;

public class S3Export {

	private static final String DEST_BUCKET = getProperty("s3DestBucket");

	private static final String DEST_PREFIX = getProperty("s3DestPrefix");

	private static final String[] LOG_GROUPS = getProperty("logGroups").split(",");

	private static final int MAX_ATTEMPTS_NUMBER = 18;

	private static final int WAIT_INTERVAL_IN_SEC = 15;

	private static final AWSLogs logs = AWSLogsClientBuilder.standard().withRegion(getProp("AWS_REGION")).build();

	public void handle(Context context) {
		LambdaLogger ll = context.getLogger();
		Instant to = Instant.now().truncatedTo(DAYS);
		Instant from = to.truncatedTo(DAYS).minus(ofDays(1));
		ll.log("Logs to be exported From = " + from);
		ll.log("Logs to be exported To = " + to);
		for (String logGroup : LOG_GROUPS) {
			if (logGroup == null || logGroup.trim().isEmpty()) {
				continue;
			}
			logGroup = logGroup.trim();

			ll.log("Export for log group " + logGroup + " is about to start");
			CreateExportTaskResult result = logs.createExportTask(new CreateExportTaskRequest()
					.withDestination(DEST_BUCKET).withDestinationPrefix(DEST_PREFIX + logGroup)
					.withLogGroupName(logGroup).withFrom(from.toEpochMilli()).withTo(to.toEpochMilli()));
			ll.log("Create export task result = " + result);
			String taskId = result.getTaskId();
			String statusCode = getStatusCode(taskId);
			ll.log("Export task status code = " + statusCode);
			int attemptCount = 1;
			while ((attemptCount <= MAX_ATTEMPTS_NUMBER) && ("PENDING".equals(statusCode)
					|| "PENDING_CANCEL".equals(statusCode) || "RUNNING".equals(statusCode))) {
				ll.log("Attempt count = " + attemptCount);
				ll.log("Will wait until export task of id " + taskId + " is over or it timeouts");
				try {
					SECONDS.sleep(WAIT_INTERVAL_IN_SEC);
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
				statusCode = getStatusCode(taskId);
				ll.log("Export task status code = " + statusCode);
				attemptCount++;
			}
		}
	}

	private static String getStatusCode(String taskId) {
		return logs.describeExportTasks(new DescribeExportTasksRequest().withTaskId(taskId)).getExportTasks().get(0)
				.getStatus().getCode();
	}

	private static String getProp(String key) {
		String propertyValue = getProperty(key);
		return propertyValue == null ? getenv(key) : propertyValue;
	}

}
