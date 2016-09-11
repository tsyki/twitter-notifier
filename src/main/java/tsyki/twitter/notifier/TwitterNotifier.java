package tsyki.twitter.notifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import twitter4j.Logger;
import twitter4j.Query;
import twitter4j.QueryResult;
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
	private static final String KEY_SEND_TO_SCREEN_NAME = "send_to_screen_name";

	private static final String KEY_KEYWORD = "keyword";

	private String confFileName;
	
	private String sinceIdFileName;

	private List<String> keywords = new ArrayList<>();

	/** 検索開始ID*/
	private Long sinceId;

	/** DM送信先のScreenName*/
	private String sendToScreenName;

	private Logger logger = Logger.getLogger(this.getClass());

	public TwitterNotifier(String confFileName,String sinceIdFileName) {
		this.confFileName = confFileName;
		this.sinceIdFileName = sinceIdFileName;
	}

	public void run() throws TwitterException, UnsupportedEncodingException, FileNotFoundException, IOException{
	    Twitter twitter = TwitterFactory.getSingleton();

	    loadConfig();

	    Set<Status> hitTweets = doSearch(twitter, keywords, sinceId);
	    if(hitTweets.isEmpty()){
	    	logger.info("ヒットなし");
	    	return;
	    }
	    // 送信メッセージ作成
	    String dm = createSendText(hitTweets);
	    // DM送信
	    twitter.sendDirectMessage(sendToScreenName, dm);
	    // 最新IDを保存
	    saveLastId(hitTweets);
	}

	private void saveLastId(Set<Status> hitTweets) throws IOException,
			FileNotFoundException {
		Long maxId = 0L;
	    for (Status tweet : hitTweets) {
	    	if(maxId < tweet.getId()){
	    		maxId = tweet.getId();
	    	}
	    }
	    try (Writer fileWriter =  new OutputStreamWriter(new FileOutputStream(sinceIdFileName))){
	    	fileWriter.write(maxId.toString());
	    }
	}

	private String createSendText(Set<Status> hitTweets) {
		List<String> msgs = new ArrayList<>();
		List<Status> sorted = new LinkedList<>(hitTweets);
		Collections.sort(sorted, new Comparator<Status>() {
			@Override
			public int compare(Status o1, Status o2) {
				// 作成日降順
				return -o1.getCreatedAt().compareTo(o2.getCreatedAt());
			}
		});
	    for (Status status : sorted) {
	    	// ツイートへのリンク+作成日時+テキスト
	    	String msg = getTweetUrl(status.getUser().getScreenName(), status.getId()) + " " + status.getCreatedAt() + " " + status.getText().replace("\n", " ");
	    	logger.info("hit:" + msg);
	    	msgs.add(msg);
	    }

	    StringBuilder dmBuilder = new StringBuilder();
	    for (String msg : msgs) {
			dmBuilder.append(msg + "\n");
		}
	    String dm = dmBuilder.toString();
		return dm;
	}

	private Set<Status> doSearch(Twitter twitter,List<String> keywords,Long sinceId) throws TwitterException {
		Set<Status> hitTweets = new LinkedHashSet<>();
	    for (String keyword : keywords) {
		    Query query = new Query();
		    // RTは重複してしまうので除外
		    query.setQuery(keyword + " exclude:retweets");
		    if(sinceId != null){
		    	query.setSinceId(sinceId);
		    }

		    QueryResult searchResult = twitter.search(query);
		    List<Status> tweets = searchResult.getTweets();
		    hitTweets.addAll(tweets);
		}
		return hitTweets;
	}

	private Properties loadConfig() throws IOException, UnsupportedEncodingException,
			FileNotFoundException {
		Properties prop = new Properties();
	    prop.load(new InputStreamReader(new FileInputStream(confFileName), "UTF-8"));
	    // 検索キーワード読み込み
	    int n = 1;
	    while(true){
	    	String keyword = prop.getProperty(KEY_KEYWORD  + n++);
	    	if(keyword == null || keyword.isEmpty()){
	    		break;
	    	}
	    	this.keywords.add(keyword);
	    }
	    if(keywords.isEmpty()){
	    	throw new IllegalStateException("設定ファイルに検索キーワードが指定されていません");
	    }
	    this.sendToScreenName = prop.getProperty(KEY_SEND_TO_SCREEN_NAME);
	    if(this.sendToScreenName == null || this.sendToScreenName.isEmpty()){
	    	throw new IllegalStateException("設定ファイルにDM送信先ユーザのscreenNameが指定されていません");
	    }
	    // 検索開始Id
	    File sinceIdFile = new File(sinceIdFileName);
	    if(sinceIdFile.exists()){
	    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sinceIdFile)))){
	    	String sinceIdStr = reader.readLine();
		    if(sinceIdStr != null && !sinceIdStr.isEmpty()){
		    	this.sinceId = Long.parseLong(sinceIdStr);
		    }
	    }
	    }
	    return prop;
	}

	private String getTweetUrl(String userName,long id){
		return "https://twitter.com/" + userName +  "/status/" + id;
	}

	public static void main(String[] args) throws TwitterException, UnsupportedEncodingException, FileNotFoundException, IOException {
		String confFileName = "search.properties";
		if(args.length >= 1){
			confFileName = args[0];
		}
		String sinceIdFileName = "sinceId";
		if(args.length >= 2){
			sinceIdFileName = args[1];
		}

		TwitterNotifier notifier = new TwitterNotifier(confFileName,sinceIdFileName);
		notifier.run();
	}
}
