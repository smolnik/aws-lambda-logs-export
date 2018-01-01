package cloud.developing.logs.aws;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.PutRetentionPolicyRequest;

public class CloudWatchLogsCleaner {

	public static void main(String[] args) throws Exception {

		AWSLogs logs = AWSLogsClientBuilder.standard().withCredentials(new ProfileCredentialsProvider("xyz"))
				.withRegion(Regions.US_EAST_1).build();
		logs.describeLogGroups().getLogGroups().forEach(lg -> {
			String name = lg.getLogGroupName();
			logs.putRetentionPolicy(new PutRetentionPolicyRequest(name, 1));
		});

	}

}
