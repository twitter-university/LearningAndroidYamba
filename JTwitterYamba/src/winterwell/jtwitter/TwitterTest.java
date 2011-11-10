package winterwell.jtwitter;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;

import junit.framework.TestCase;
import winterwell.jtwitter.Twitter.Message;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;
import winterwell.jtwitter.TwitterException.E403;

/**
 * Unit tests for JTwitter.
 * These only provide partial testing -- sorry.
 *
 *
 * @author daniel
 */
public class TwitterTest
extends TestCase // Comment out to remove the JUnit dependency
{

	public void testRetweetsByMe() {
		Twitter source = new Twitter("spoonmcguffin", "my1stpassword"); // FIXME
		int salt = new Random().nextInt(1000);
		Status original = source.setStatus(salt+" strewth, what a test!");
		Twitter twitter = new Twitter(TEST_USER, TEST_PASSWORD);
		
		Status tweet = twitter.getStatus("spoonmcguffin");
		assert tweet.equals(original);
		
		Status retweet = twitter.retweet(tweet);
		
		List<Status> rtsByMe = twitter.getRetweetsByMe();
		List<Status> rtsOfMe = source.getRetweetsOfMe();
		Status s = twitter.getStatus();
		assert s == null;
		
		assert retweet.inReplyToStatusId == tweet.id;		
		assert retweet.getOriginal().equals(tweet);
		assert retweet.getText().startsWith("RT @spoonmcguffin: "+salt);		
		assert ! rtsByMe.isEmpty();
		assert rtsByMe.contains(retweet);		
		assert ! rtsOfMe.isEmpty();
		assert rtsOfMe.contains(tweet);
	}
	
	public void testMisc() {
		Twitter twitter = new Twitter();//"aagha", PASSWORD);
    	// problem 
        Status tStatus = twitter.getStatus("aagha");
    	String status = tStatus.getText();
    	System.out.println(status);
	}
	
	static final String TEST_USER = "jtwit";
	
	public static final String TEST_PASSWORD = "notsofast";

	public static void main(String[] args) {
		TwitterTest tt = new TwitterTest();
		Method[] meths = TwitterTest.class.getMethods();
		for(Method m : meths) {
			if ( ! m.getName().startsWith("test")
					|| m.getParameterTypes().length != 0) continue;
			try {
				m.invoke(tt);
				System.out.println(m.getName());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.out.println("TEST FAILED: "+m.getName());
				System.out.println("\t"+e.getCause());
			}
		}
	}

	public void testSearchUsers() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);

		List<User> users = tw.searchUsers("Nigel Griffiths");
		System.out.println(users);

		// AND Doesn't work!
		List<User> users2 = tw.searchUsers("Fred near:Scotland");
		assert ! users.isEmpty();
	}

	public void testBulkShow() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<User> users = tw.bulkShow(Arrays.asList("winterstein", "joehalliwell", "annettemees"));
		assert users.size() == 3 : users;
		assert users.get(1).description != null;
	}

	public void testBulkShowById() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);		
		List<Long> userIds = Arrays.asList(32L, 34L, 45L, 12435562L);
		List<User> users = tw.bulkShowById(userIds);
		assert users.size() == 2 : users;
	}
	
	// slow, as we have to wade through a lot of misses
//	public void testBulkShowById2() {
//		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);		
//		List<Long> userIds = new ArrayList<Long>();
//		for(int i=0; i<5000; i++) {
//			userIds.add(12435562L + i);
//		}
//		List<User> users = tw.bulkShowById(userIds);
//		System.out.println(users.size());
//		assert users.size() > 100 : users;
//	}

	/**
	 * Check that you can send 160 chars if you wants
	 */
	public void canSend160() {
		String s = "";
		for(int i=0; i<15; i++) {
			s += i+"23456789 ";
		}
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		tw.setStatus(s);
	}

	/**
	 *  NONDETERMINISTIC! Had to increase sleep time to make it more reliable.
	 * @throws InterruptedException
	 */
	public void testDestroyStatus() throws InterruptedException {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		Status s1 = tw.getStatus();
		tw.destroyStatus(s1.getId());
		Status s0 = tw.getStatus();
		assert s0.id != s1.id : "Status id should differ from that of destroyed status";
	}

	public void testDestroyStatusBad() {
		// Check security failure
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		Status hs = tw.getStatus("winterstein");
		try {
			tw.destroyStatus(hs);
			assert false;
		} catch (Exception ex) {
			// OK
		}
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#follow(java.lang.String)}.
	 */
	public void testFollowAndStopFollowing() throws InterruptedException {
		int lag = 1000; //300000;
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		tw.flush();
		List<User> friends = tw.getFriends();
		if ( ! tw.isFollowing("winterstein")) {
			tw.follow("winterstein");
			Thread.sleep(lag);
		}
		assert tw.isFollowing("winterstein") : friends;

		// Stop
		User h = tw.stopFollowing("winterstein");
		assert h != null;
		Thread.sleep(lag);
		assert ! tw.isFollowing("winterstein") : friends;

		// break where no friendship exists
		User h2 = tw.stopFollowing("winterstein");
		assert h2==null;

		// Follow
		tw.follow("winterstein");
		Thread.sleep(lag);
		assert tw.isFollowing("winterstein") : friends;

		try {
			User suspended = tw.follow("Alysha6822");
			assert false : "Trying to follow a suspended user should throw an exception";
		} catch (TwitterException e) {
		}
	}

	public void testIdenticaAccess() {
		Twitter jtwit = new Twitter(TEST_USER, TEST_PASSWORD);
		jtwit.setAPIRootUrl("http://identi.ca/api");
		char salt = (char) ('A' + new Random().nextInt(48));
		System.out.println(salt);
		Status s1 = null;
		try {
			s1 = jtwit.updateStatus(salt+" Hello to you shiny open source people");
		} catch (TwitterException.Timeout e) {
			// identi.ca has problems
		}
		Status s2 = jtwit.getStatus();
		assert s1.equals(s2) : s1+" vs "+s2;
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFollowerIDs()}
	 * and {@link winterwell.jtwitter.Twitter#getFollowerIDs(String)}.
	 *
	 */
	public void testFollowerIDs() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Long> ids = tw.getFollowerIDs();
		for (Long id : ids) {
			// Getting a 403 Forbidden error here - not sure what that means
			// user id = 33036740 is causing the problem
			// possibly to do with protected updates?
			try {
				assert tw.isFollower(id.toString(), TEST_USER) : id;
			} catch (E403 e) {
				// this seems to be a corner issue with Twitter's API rather than a bug in JTwitter
				System.out.println(id+" "+e);
			}
		}
		List<Long> ids2 = tw.getFollowerIDs(TEST_USER);
		assert ids.equals(ids2);
	}

	/**
	 * Test the new cursor-based follower/friend methods.
	 */
	public void testManyFollowerIDs() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		tw.setMaxResults(50000);
		List<Long> ids = tw.getFollowerIDs("stephenfry");
		assertTrue(ids.size() >= 50000);
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriendIDs()}
	 * and {@link winterwell.jtwitter.Twitter#getFriendIDs(String)}.
	 */
	public void testFriendIDs() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Long> ids = tw.getFriendIDs();
		for (Long id : ids) {
			try {
				assert tw.isFollower(TEST_USER, id.toString());
			} catch (E403 e) {
				// ignore
				e.printStackTrace();
			}
		}
		List<Long> ids2 = tw.getFriendIDs(TEST_USER);
		assert ids.equals(ids2);
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getDirectMessages()}.
	 */
	public void testGetDirectMessages() {
		// send one to make sure there is one
//		Twitter tw0 = new Twitter("winterstein", "");
//		String salt = Utils.getRandomString(4);
//		String msg = "Hello "+TEST_USER+" "+salt;
//		tw0.sendMessage(TEST_USER, msg);

		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Message> msgs = tw.getDirectMessages();
		for (Message message : msgs) {
			User recipient = message.getRecipient();
			assert recipient.equals(new User(TEST_USER));
		}
		assert msgs.size() != 0;
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getDirectMessagesSent()}.
	 */
	public void testGetDirectMessagesSent() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Message> msgs = tw.getDirectMessagesSent();
		for (Message message : msgs) {
			assert message.getSender().equals(new User(TEST_USER));
		}
		assert msgs.size() != 0;
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFeatured()}.
	 */
	public void testGetFeatured() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<User> f = tw.getFeatured();
		assert f.size() > 0;
		assert f.get(0).status != null;
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFollowers()}.
	 */
	public void testGetFollowers() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<User> f = tw.getFollowers();
		assert f.size() > 0;
		assert Twitter.getUser("winterstein", f) != null;
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriends()}.
	 */
	public void testGetFriends() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<User> friends = tw.getFriends();
		assert friends != null;
	}

	/**
	 * Test the cursor-based API for getting many followers.
	 * Slightly intermittent
	 */
	public void testGetManyFollowers() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		tw.setMaxResults(10000); // we don't want to run the test for ever.
		String victim = "psychovertical";
		User user = tw.getUser(victim);
		assertFalse("More than 10000 followers; choose a different victim or increase the maximum results",
				user.followersCount > 10000);
		Set<User> followers = new HashSet(tw.getFollowers(victim));
		Set<Long> followerIDs = new HashSet(tw.getFollowerIDs(victim));
		// psychovertical has about 600 followers, as of 14/12/09
		assertEquals(user.followersCount, followers.size());
		assertEquals(user.followersCount, followerIDs.size());
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriends(java.lang.String)}.
	 */
	public void testGetFriendsString() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<User> friends = tw.getFriends("winterstein");
		assert friends != null;
	}
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getFriendsTimeline()}.
	 */
	public void testGetFriendsTimeline() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Status> ft = tw.getFriendsTimeline();
		assert ft.size() > 0;
	}

	public void testTooOld() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		try {
			tw.setSinceId(10584958134L);
			tw.setSearchLocation(55.954151,-3.20277,"18km");
			List<Status> tweets = tw.search("stuff");
			assert false;
		} catch (TwitterException.E403 e) {
			String msg = e.getMessage();
		}
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getPublicTimeline()}.
	 */
	public void testGetPublicTimeline() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Status> pt = tw.getPublicTimeline();
		assert pt.size() > 5;
	}

	public void testGetRateLimitStats() throws InterruptedException {
		{
			Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
			int i = tw.getRateLimitStatus();
			if (i<1) return;
			tw.getStatus();
			Thread.sleep(1000);
			int i2 = tw.getRateLimitStatus();
			assert i - 1 == i2;
		}
		{
			Twitter tw = new Twitter();
			int i = tw.getRateLimitStatus();
		}
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getReplies()}.
	 */
	public void testGetReplies() {
		{
			Matcher m = Status.AT_YOU_SIR.matcher("@dan hello");
			assert m.find();
			m.group(1).equals("dan");
		}
		//		{	// done in code
		//			Matcher m = Status.atYouSir.matcher("dan@email.com hello");
		//			assert ! m.find();
		//		}
		{
			Matcher m = Status.AT_YOU_SIR.matcher("hello @dan");
			assert m.find();
			m.group(1).equals("dan");
		}

		Twitter tw = new Twitter(TEST_USER,TEST_PASSWORD);
		List<Status> r = tw.getReplies();
		for (Status message : r) {
			List<String> ms = message.getMentions();
			assert ms.contains(TEST_USER) : message;
		}
		System.out.println("Replies "+r);
	}

	public void testAagha() {
		Twitter tw = new Twitter(TEST_USER,TEST_PASSWORD);
		Status s = tw.getStatus("aagha");
		assert s != null;
	}
	
	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getStatus(int)}.
	 */
	public void testGetStatus() {
		Twitter tw = new Twitter(TEST_USER,TEST_PASSWORD);
		Status s = tw.getStatus();
		assert s != null;
		System.out.println(s);

		//		// test no status
		//		tw = new Twitter(ANOther Account);
		//		s = tw.getStatus();
		//		assert s == null;
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getStatus(long)}.
	 */
	public void testGetStatusLong() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		Status s = tw.getStatus();
		Status s2 = tw.getStatus(s.getId());
		assert s.text.equals(s2.text) : "Fetching a status by id should yield correct text";
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getUserTimeline()}.
	 */
	public void testGetUserTimeline() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Status> ut = tw.getUserTimeline();
		assert ut.size() > 0;
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#getUserTimeline(java.lang.String, java.lang.Integer, java.util.Date)}.
	 */
	public void testGetUserTimelineString() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		List<Status> ns = tw.getUserTimeline("anonpoetry");
		System.out.println(ns.get(0));
	}


	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#isFollower(String)}
	 * and {@link winterwell.jtwitter.Twitter#isFollower(String, String)}.
	 */
	public void testIsFollower() throws InterruptedException {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);

		assert tw.isFollower("winterstein");
		int LAG = 5000;
		User u = tw.stopFollowing("winterstein");
		Thread.sleep(LAG);
		assert ! tw.isFollowing("winterstein");
		tw.follow("winterstein");
		Thread.sleep(LAG);
		assert tw.isFollowing("winterstein");
	}

	public void testRetweet() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		String[] tweeps = new String[]{
				"winterstein", "joehalliwell", "spoonmcguffin", "forkmcguffin"};
		Status s = tw.getStatus(tweeps[new Random().nextInt(tweeps.length)]);
		Status rt1 = tw.retweet(s);
		assert rt1.text.contains(s.text) : rt1+ " vs "+s;
		Status s2 = tw.getStatus("joehalliwell");
		Status rt2 = tw.updateStatus("RT @"+s2.user.screenName+" "+s2.text);
		assert rt2.text.contains(s2.text) : rt2;
	}

	public void testSearch() {
		{
			Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
			List<Status> javaTweets = tw.search("java");
			assert javaTweets.size() != 0;
		}
		{	// few results
			Twitter tw = new Twitter();
			tw.setMaxResults(10);
			List<Status> tweets = tw.search(":)");
			assert tweets.size() == 10;
		}
		{	// Lots of results
			Twitter tw = new Twitter();
			tw.setMaxResults(300);
			List<Status> tweets = tw.search(":)");
			assert tweets.size() > 100 : tweets.size();
		}
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#sendMessage(java.lang.String, java.lang.String)}.
	 */
	public void testSendMessage() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		Message sent = tw.sendMessage("winterstein", "Please ignore this message");
		System.out.println(""+sent);
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#show(java.lang.String)}.
	 */
	public void testShow() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		User show = tw.show(TEST_USER);
		assert show != null;

		// a protected user
		User ts = tw.show("tassosstevens");
	}

	/**
	 * Test method for {@link winterwell.jtwitter.Twitter#updateStatus(java.lang.String)}.
	 */
	public void testUpdateStatus() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		String s = "Experimenting (http://winterwell.com at "+new Date().toString()+")";
		Status s2a = tw.updateStatus(s);
		Status s2b = tw.getStatus();
		assert s2b.text.equals(s) : s2b.text;
		assert s2a.id == s2b.id;
		//		assert s2b.source.equals("web") : s2b.source;
	}


	/**
	 * This crashes out at above 140, which is correct
	 * @throws InterruptedException
	 */
	public void testUpdateStatusLength() throws InterruptedException {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		Random rnd = new Random();
		{	// WTF?!
			Status s2a = tw.updateStatus("Test tweet aaaa "+rnd.nextInt(1000));
		}
		String salt = new Random().nextInt(1000)+" ";
		Thread.sleep(1000);
		{	// well under
			String s = salt+"help help ";
			for(int i=0; i<2; i++) {
				s += rnd.nextInt(1000);
				s += " ";
			}
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s.trim()) : s2b.text;
			assert s2a.id == s2b.id;
		}
		{	// 130
			String s = salt;
			for(int i=0; i<12; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}
		{	// 140
			String s = salt;
			for(int i=0; i<13; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}
		// uncomment if you wish to test longer statuses
		if (true) return;
		{	// 150
			String s = salt;
			for(int i=0; i<14; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}
		{	// 160
			String s = salt;
			for(int i=0; i<15; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}
		{	// 170
			String s = salt;
			for(int i=0; i<16; i++) {
				s += repeat((char) ('a'+i), 9);
				s += " ";
			}
			s = s.trim();
			System.out.println(s.length());
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}

	}


	private String repeat(char c, int i) {
		String s = "";
		for(int j=0; j<i; j++) {
			s += c;
		}
		return s;
	}

	public void testUpdateStatusUnicode() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		{
			String s = "Katten är hemma. Hur mår du? お元気ですか";
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}
		{
			String s = new Random().nextInt(1000)+" Гладыш Владимир";
			Status s2a = tw.updateStatus(s);
			Status s2b = tw.getStatus();
			assert s2a.text.equals(s) : s2a.text;
			assert s2b.text.equals(s) : s2b.text;
			assert s2a.id == s2b.id;
		}
	}



	public void testUserExists() {
		Twitter tw = new Twitter(TEST_USER, TEST_PASSWORD);
		assert tw.userExists("spoonmcguffin") : "There is a Spoon, honest";
		assert ! tw.userExists("chopstickmcguffin") : "However, there is no Chopstick";
		assert ! tw.userExists("Alysha6822") : "Suspended users show up as nonexistent";
	}

	/**
	 * Created on a day when Twitter's followers API was being particularly flaky,
	 * in order to find out just how bad the lag was.
	 * @author miles
	 * @throws IOException if the output file can't be opened for writing
	 * @throws InterruptedException
	 *
	 */
	public void dontTestFollowLag() throws IOException, InterruptedException {
		Twitter jt = new Twitter(TEST_USER, TEST_PASSWORD);
		String spoon = "spoonmcguffin";
		long timestamp = (new Date()).getTime();
		FileWriter outfile = new FileWriter("twitlag" + timestamp + ".txt");
		for (int i = 0; i < 1000; i++) {
			System.out.println("Starting iteration " + i);
			try {
			if (jt.isFollowing(spoon)) {
				System.out.println("jtwit was following Spoon");
				jt.stopFollowing(spoon);
				int counter = 0;
				while (jt.isFollowing(spoon)) {
					Thread.sleep(1000);
					// jt.stopFollowing(spoon);
					counter++;
				}
				try {
					outfile.write("Stopped following: " + counter + "00ms\n");
				} catch (IOException e) {
					System.out.println("Couldn't write to file: " + e);
				}
			} else {
				System.out.println("jtwit was not following Spoon");
				jt.follow(spoon);
				int counter = 0;
				while (!jt.isFollowing(spoon)) {
					Thread.sleep(1000);
					// jt.follow(spoon);
					counter++;
				}
				try {
					outfile.write("Started following: " + counter + "00ms\n");
				} catch (IOException e) {
					System.out.println("Couldn't write to file: " + e);
				}
			}
			} catch (E403 e) {
				System.out.println("isFollower() was mistaken: " + e);
			}
			outfile.flush();
		}
		outfile.close();
	}

	/**
	 *
	 */
	public void testIsValidLogin() {
		Twitter twitter = new Twitter(TEST_USER, TEST_PASSWORD);
		assertTrue(twitter.isValidLogin());
		twitter = new Twitter("rumpelstiltskin", "thisisnotarealpassword");
		assertFalse(twitter.isValidLogin());
	}

	public void testIdentica() {
		Twitter twitter = new Twitter(TEST_USER, TEST_PASSWORD);
		twitter.setAPIRootUrl("http://identi.ca/api");
		twitter.setStatus("Testing jTwitter http://winterwell.com/software/jtwitter.php");
	}

}
