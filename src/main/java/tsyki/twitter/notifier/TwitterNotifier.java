package tsyki.twitter.notifier;

import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 *
 * 指定ワードをTwitterから検索、ヒットしたらDMで通知する
 * @author TOSHIYUKI.IMAIZUMI
 *
 */
public class TwitterNotifier {
	public void run() throws TwitterException{
	    // このファクトリインスタンスは再利用可能でスレッドセーフです
	    Twitter twitter = TwitterFactory.getSingleton();
	    List<Status> statuses = twitter.getHomeTimeline();
	    System.out.println("Showing home timeline.");
	    for (Status status : statuses) {
	        System.out.println(status.getUser().getName() + ":" +
	                           status.getText());
	    }
	}


	public static void main(String[] args) throws TwitterException {
		TwitterNotifier notifier = new TwitterNotifier();
		notifier.run();
	}

}
