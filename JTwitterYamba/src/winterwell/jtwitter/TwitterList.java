package winterwell.jtwitter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.Twitter.IHttpClient;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;

/**
 * Status: draft!
 * 
 * A Twitter list, which uses lazy-fetching of its members and other details.
 * <p>
 * The methods of this object will call Twitter when they need to, and store the
 * results. E.g. the first call to {@link #size()} might require a call to
 * Twitter, but subsequent calls will not.
 * <p>
 * WARNING: Twitter only returns list members in batches of 20. So reading a
 * large list can be slow and use quite a few calls to Twitter.
 * <p>
 * To find out what lists you or another user has, see
 * {@link Twitter#getLists()} and {@link Twitter#getLists(String)}.
 * @author daniel
 * 
 */
public class TwitterList extends AbstractList<Twitter.User> {

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}

	private long id;

	/**
	 * Add a user to the list. List size is limited to 500 users.
	 * 
	 * @return testing for membership could be slow, so this is always true.
	 */
	public boolean add(User user) {
		String url = jtwit.TWITTER_URL + "/" + owner.screenName + "/"
				+ idOrSlug() + "/members.json";
		Map map = Twitter.asMap("id", user.screenName);
		String json = http.post(url, map, true);
		// error messages?
		return true;
	}

	/**
	 * @return users who follow this list TODO cursor support for more than 20
	 *         users
	 */
	public List<User> getSubscribers() {
		String url = jtwit.TWITTER_URL + "/" + owner.screenName + "/"
				+ idOrSlug() + "/subscribers.json";
		String json = http.getPage(url, null, true);
		return User.getUsers(json);
	}

	/**
	 * Remove a user from the list.
	 * 
	 * @return testing for membership could be slow, so this is always true.
	 */
	@Override
	public boolean remove(Object o) {
		User user = (User) o;
		String url = jtwit.TWITTER_URL + "/" + owner.screenName + "/"
				+ idOrSlug() + "/members.json";
		Map map = Twitter.asMap("id", user.screenName, "_method", "DELETE");
		String json = http.post(url, map, true);
		// error messages?
		return true;
	}

	private String slug;

	/**
	 * The same client as the JTwitter object used in the constructor.
	 */
	private final IHttpClient http;

	/**
	 * A lazy-loading list viewer. This will fetch details
	 * from Twitter when you call it's methods. This is for access to an
	 * existing list - it does NOT create a new list on Twitter.
	 * 
	 * @param owner.screenName
	 *            The Twitter screen-name for the list's owner.
	 * @param slug
	 *            The list's name. Technically the slug and the name needn't be
	 *            the same, but they usually are.
	 * @param jtwit
	 *            a JTwitter object (this must be able to authenticate).
	 */
	public TwitterList(String ownerScreenName, String slug, Twitter jtwit) {
		assert ownerScreenName != null && slug != null && jtwit != null;
		this.jtwit = jtwit;
		this.owner = new User(ownerScreenName); // use a dummy here
		this.name = slug;
		this.slug = slug;
		this.http = jtwit.getHttpClient();
	}

	/**
	 * CREATE a brand new Twitter list. 
	 * Accounts are limited to 20 lists.
	 * 
	 * @param listName
	 *            The list's name.
	 * @param jtwit
	 *            a JTwitter object (this must be able to authenticate).
	 * @param description A description for this list. Can be null.
	 */
	public TwitterList(String listName, Twitter jtwit, boolean isPublic, String description) {
		assert listName != null && jtwit != null;
		this.jtwit = jtwit;
		String ownerScreenName = jtwit.getScreenName();
		assert ownerScreenName != null;
		this.name = listName;
		this.slug = listName;
		this.http = jtwit.getHttpClient();
		// create!
		String url = jtwit.TWITTER_URL + "/" + ownerScreenName + "/lists.json";		
		Map<String, String> vars = Twitter.asMap(
				"name", listName,
				"mode", isPublic? "public" : "private", 
				"description", description);
		String json = http.post(url, vars, true);
		try {
			JSONObject jobj = new JSONObject(json);
			init(jobj);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}	
	
	/**
	 * Delete this list!
	 * 
	 * @throws TwitterException
	 *             on failure
	 */
	public void delete() {
		try {
			String URL = jtwit.TWITTER_URL + "/" + owner.screenName+ "/lists/"
					+ URLEncoder.encode(slug, "utf-8") + ".json?_method=DELETE";
			http.post(URL, null, http.canAuthenticate());
		} catch (UnsupportedEncodingException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * Used by {@link Twitter#getLists(String)}
	 * 
	 * @param json
	 * @param jtwit
	 * @throws JSONException
	 */
	TwitterList(JSONObject json, Twitter jtwit) throws JSONException {
		this.jtwit = jtwit;		
		this.http = jtwit.getHttpClient();
		init(json);
	}

	private final Twitter jtwit;

	private final List<User> users = new ArrayList<Twitter.User>();

	private String name;

	public String getName() {
		return name;
	}

	/**
	 * Returns a list of statuses from this list.
	 * 
	 * @return List<Status> a list of Status objects for the list
	 * @throws TwitterException
	 */
	// Added TG 3/31/10
	public List<Status> getStatuses() throws TwitterException {
		try {
			String jsonListStatuses = http.getPage(jtwit.TWITTER_URL + "/"
					+ owner.screenName + "/lists/"
					+ URLEncoder.encode(slug, "UTF-8") + "/statuses.json",
					null, http.canAuthenticate());
			List<Status> msgs = Status.getStatuses(jsonListStatuses);
			return msgs;
		} catch (UnsupportedEncodingException e) {
			throw new TwitterException(e);
		}		
	}

	/**
	 * cursor for paging through the members of the list
	 */
	private long cursor = -1;


	@Override
	public User get(int index) {
		// pull from Twitter
		String url = jtwit.TWITTER_URL + "/" + owner.screenName + "/"
				+ idOrSlug() + "/members.json";
		while (users.size() < index + 1 && cursor != 0) {
			Map vars = new HashMap();
			vars.put("cursor", cursor);
			String json = http.getPage(url, vars, true);
			try {
				JSONObject jobj = new JSONObject(json);
				JSONArray jarr = (JSONArray) jobj.get("users");
				List<User> users1page = User.getUsers(jarr.toString());
				users.addAll(users1page);
				cursor = new Long(jobj.getString("next_cursor"));
			} catch (JSONException e) {
				throw new TwitterException("Could not parse user list" + e);
			}
		}
		return users.get(index);
	}

	private int memberCount = -1;

	private int subscriberCount;

	private String description;

	public String getDescription() {
		init();
		return description;
	}

	private boolean _private;

	/**
	 * never null (but may be a dummy object)
	 */
	private User owner;

	public int getSubscriberCount() {
		init();
		return subscriberCount;
	}

	@Override
	public int size() {
		init();
		return memberCount;
	}

	/**
	 * Fetch list info from Twitter
	 */
	private void init() {
		if (memberCount != -1)
			return;
		String url = jtwit.TWITTER_URL + "/" + owner.screenName + "/lists/"
				+ idOrSlug() + ".json";
		String json = http.getPage(url, null, true);
		try {
			JSONObject jobj = new JSONObject(json);
			init(jobj);
		} catch (JSONException e) {
			throw new TwitterException("Could not parse response: " + e);
		}
	}

	private void init(JSONObject jobj) throws JSONException {
		// owner.screenName = ;
		memberCount = jobj.getInt("member_count");
		subscriberCount = jobj.getInt("subscriber_count");
		name = jobj.getString("name");
		slug = jobj.getString("slug");
		id = jobj.getLong("id");
		_private = "private".equals(jobj.optString("mode"));
		description = jobj.optString("description");
		JSONObject user = jobj.getJSONObject("user");
		owner = new User(user, null);
	}

	public boolean isPrivate() {
		init();
		return _private;
	}

	private String idOrSlug() {
		// TODO encode slugs here?
		return id > 0 ? Long.toString(id) : slug;
	}

}
