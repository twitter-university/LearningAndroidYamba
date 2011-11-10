package winterwell.jtwitter;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import winterwell.jtwitter.TwitterException.E403;

/**
 * Java wrapper for the Twitter API version 1.6.0
 * <p>
 * Example usage: <code><pre>
  // Make a Twitter object
  Twitter twitter = new Twitter("my-name","my-password");
  // Print Winterstein's status
  System.out.println(twitter.getStatus("winterstein"));
  // Set my status
  twitter.updateStatus("Messing about in Java");
</pre></code>
 * <p>
 * See {@link http://www.winterwell.com/software/jtwitter.php} for more
 * information about this wrapper. See {@link http
 * ://apiwiki.twitter.com/Twitter-API-Documentation} for more information about
 * the Twitter API.
 * <p>
 * Notes:
 * <ul>
 * <li>This wrapper takes care of all url-encoding/decoding.
 * <li>This wrapper will throw a runtime exception (TwitterException) if a
 * methods fails, e.g. it cannot connect to Twitter.com or you make a bad
 * request.
 * <li>Note that Twitter treats old-style retweets (those made by sending a
 * normal tweet beginning "RT @whoever") differently from new-style retweets
 * (those made using the retweet API). The differences are documented in various
 * methods.
 * <li>Most methods are in this class (Twitter), except for list support (in
 * {@link TwitterList} - though {@link #getLists()} is here) and some
 * profile/account settings (in {@link TwitterAccount}).
 * </ul>
 * 
 * <h4>Copyright and License</h4>
 * This code is copyright (c) Winterwell Associates 2008/2009 and (c) ThinkTank
 * Mathematics Ltd, 2007 except where otherwise stated. It is released as
 * open-source under the LGPL license. See <a
 * href="http://www.gnu.org/licenses/lgpl.html"
 * >http://www.gnu.org/licenses/lgpl.html</a> for license details. This code
 * comes with no warranty or support.
 * 
 * <h4>Change List</h4>
 * The change list is kept online at: {@link http
 * ://www.winterwell.com/software/changelist.txt}
 * 
 * @author Daniel Winterstein
 */
public class Twitter {
  // parse Twitter date
  static final SimpleDateFormat dateFormat = new SimpleDateFormat(
      "EEE MMM dd HH:mm:ss ZZZZZ yyyy");

  /**
   * Change this to access sites other than Twitter that support the Twitter
   * API.
   */
  String TWITTER_URL = "http://api.twitter.com/1";

  /**
   * Set this to access sites other than Twitter that support the Twitter API.
   * E.g. WordPress or Identi.ca. Note that not all methods may work!
   * 
   * @param url
   *          Format: "http://domain-name", e.g. "http://twitter.com" by
   *          default.
   */
  public void setAPIRootUrl(String url) {
    assert url.startsWith("http://") || url.startsWith("https://");
    assert !url.endsWith("/") : "Please remove the trailing / from " + url;
    TWITTER_URL = url;
  }

  /**
   * Use to register per-page callbacks for long-running searches. To stop the
   * search, return true.
   * 
   * @author miles
   * 
   */
  public interface ICallback {
    public boolean process(List<Status> statuses);
  }

  /**
   * Interface for an http client - e.g. allows for OAuth to be used instead.
   * The default version is {@link URLConnectionHttpClient}.
   * <p>
   * If creating your own version, please provide support for throwing the right
   * subclass of TwitterException - see
   * {@link URLConnectionHttpClient#processError(java.net.HttpURLConnection)}
   * for example code.
   * 
   * @author Daniel Winterstein
   */
  public static interface IHttpClient {
    /** Whether this client can authenticate to the server. */
    boolean canAuthenticate();

    /**
     * Send an HTTP GET request and return the response body. Note that this
     * will change all line breaks into system line breaks!
     * 
     * @param uri
     *          The uri to fetch
     * @param vars
     *          get arguments to add to the uri
     * @param authenticate
     *          If true, use authentication. The authentication method used
     *          depends on the implementation (basic-auth, OAuth). It is an
     *          error to use true if no authentication details have been set.
     * 
     * @throws TwitterException
     *           for a variety of reasons
     * @throws TwitterException.E404
     *           for resource-does-not-exist errors
     */
    String getPage(String uri, Map<String, String> vars, boolean authenticate)
        throws TwitterException;

    /**
     * Send an HTTP POST request and return the response body.
     * 
     * @param uri
     *          The uri to post to.
     * @param vars
     *          The form variables to send. These are URL encoded before
     *          sending.
     * @param authenticate
     *          If true, send user authentication
     * @return The response from the server.
     * 
     * @throws TwitterException
     *           for a variety of reasons
     * @throws TwitterException.E404
     *           for resource-does-not-exist errors
     */
    String post(String uri, Map<String, String> vars, boolean authenticate)
        throws TwitterException;

  }

  /**
   * This gives common access to features that are common to both
   * {@link Message}s and {@link Status}es.
   * 
   * @author daniel
   * 
   */
  public static interface ITweet {

    Date getCreatedAt();

    /**
     * @return The Twitter id for this post. This is used by some API methods.
     */
    long getId();

    /** The actual status text. This is also returned by {@link #toString()} */
    String getText();

    /** The User who made the tweet */
    User getUser();

  }

  /**
   * A Twitter direct message. Fields are null if unset.
   * <p>
   * Historical note: this class used to cover \@you mentions as well, but these
   * are better described by Statuses.
   */
  public static final class Message implements ITweet {

    /**
     * Equivalent to {@link Status#inReplyToStatusId} *but null by default*. If
     * you want to use this, you must set it yourself. Strangely Twitter don't
     * report the previous ID for messages.
     */
    public Long inReplyToMessageId;

    /**
     * 
     * @param json
     * @return
     * @throws TwitterException
     */
    static List<Message> getMessages(String json) throws TwitterException {
      if (json.trim().equals(""))
        return Collections.emptyList();
      try {
        List<Message> msgs = new ArrayList<Message>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          Message u = new Message(obj);
          msgs.add(u);
        }
        return msgs;
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
    }

    private Date createdAt;
    private final long id;
    private final User recipient;
    private final User sender;
    private final String text;

    /**
     * @param obj
     * @throws JSONException
     * @throws TwitterException
     */
    Message(JSONObject obj) throws JSONException, TwitterException {
      id = obj.getLong("id");
      String _text = obj.getString("text");
      text = unencode(_text);
      String c = jsonGet("created_at", obj);
      try {
        createdAt = dateFormat.parse(c);
      } catch (ParseException e) {
        e.printStackTrace();
        createdAt = new Date();
      }
      sender = new User(obj.getJSONObject("sender"), null);
      // recipient - for messages you sent
      if (obj.has("recipient")) {
        recipient = new User(obj.getJSONObject("recipient"), null);
      } else {
        recipient = null;
      }
    }

    public Date getCreatedAt() {
      return createdAt;
    }

    public long getId() {
      return id;
    }

    /**
     * @return the recipient (for messages sent by the authenticating user)
     */
    public User getRecipient() {
      return recipient;
    }

    public User getSender() {
      return sender;
    }

    public String getText() {
      return text;
    }

    /**
     * This is equivalent to {@link #getSender()}
     */
    public User getUser() {
      return getSender();
    }

    @Override
    public String toString() {
      return text;
    }

  }

  /**
   * A Twitter status post. .toString() returns the status text.
   * <p>
   * Notes: This is a finalised data object. It exposes its fields for
   * convenient access. If you want to change your status, use
   * {@link Twitter#setStatus(String)} and {@link Twitter#destroyStatus(Status)}.
   */
  public static final class Status implements ITweet {

    @Override
    public int hashCode() {
      return (int) (id ^ (id >>> 32));
    }

    /**
     * Tests by class=Status and tweet id number
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Status other = (Status) obj;
      if (id != other.id)
        return false;
      return true;
    }

    /**
     * Convert from a json array of objects into a list of tweets.
     * 
     * @param json
     *          can be empty, must not be null
     * @throws TwitterException
     */
    static List<Status> getStatuses(String json) throws TwitterException {
      if (json.trim().equals(""))
        return Collections.emptyList();
      try {
        List<Status> tweets = new ArrayList<Status>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          Status tweet = new Status(obj, null);
          tweets.add(tweet);
        }
        return tweets;
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
    }

    /**
     * Search results use a slightly different protocol! In particular w.r.t.
     * user ids and info.
     * 
     * @param searchResults
     * @return search results as Status objects - but with dummy users! The
     *         dummy users have a screenname and a profile image url, but no
     *         other information. This reflects the current behaviour of the
     *         Twitter API.
     */
    static List<Status> getStatusesFromSearch(Twitter tw, String json) {
      try {
        JSONObject searchResults = new JSONObject(json);
        List<Status> users = new ArrayList<Status>();
        JSONArray arr = searchResults.getJSONArray("results");
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          String userScreenName = obj.getString("from_user");
          String profileImgUrl = obj.getString("profile_image_url");
          User user = new User(userScreenName);
          user.profileImageUrl = URI(profileImgUrl);
          Status s = new Status(obj, user);
          users.add(s);
        }
        return users;
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
    }

    public Date createdAt;
    public final long id;
    /** The actual status text. */
    public final String text;

    public final User user;

    /**
     * E.g. "web" vs. "im"
     */
    public final String source;

    /**
     * Often null (even when this Status is a reply). This is the in-reply-to
     * status id as reported by Twitter.
     */
    public final Long inReplyToStatusId;

    private boolean favorited;
    /**
     * null, except for official retweets when this is the original retweeted
     * Status.
     */
    private Status original;

    /**
     * Only set for official new-style retweets. This is the original retweeted
     * Status. null otherwise.
     */
    public Status getOriginal() {
      return original;
    }

    /**
     * true if this has been marked as a favourite by the authenticating user
     */
    public boolean isFavorite() {
      return favorited;
    }

    /**
     * regex for @you mentions
     */
    static final Pattern AT_YOU_SIR = Pattern.compile("@(\\w+)");

    /**
     * @param object
     * @param user
     *          Set when parsing the json returned for a User. null when parsing
     *          the json returned for a Status.
     * @throws TwitterException
     */
    Status(JSONObject object, User user) throws TwitterException {
      try {
        id = object.getLong("id");
        String _text = jsonGet("text", object);
        text = unencode(_text);
        String c = jsonGet("created_at", object);
        try {
          createdAt = dateFormat.parse(c);
        } catch (ParseException e) {
          e.printStackTrace();
          createdAt = new Date();
        }
        source = jsonGet("source", object);
        // retweet?
        JSONObject retweeted = object.optJSONObject("retweeted_status");
        if (retweeted != null) {
          original = new Status(retweeted, null);
        }
        String irt = jsonGet("in_reply_to_status_id", object);
        if (irt == null) {
          // Twitter doesn't give in-reply-to for retweets
          // - but since we have the info, let's make it available
          inReplyToStatusId = original == null ? null : original.getId();
        } else {
          inReplyToStatusId = Long.valueOf(irt);
        }
        favorited = object.has("favorited") && object.getBoolean("favorited");
        if (user != null) {
          this.user = user;
        } else {
          JSONObject jsonUser = object.optJSONObject("user");
          this.user = new User(jsonUser, this);
        }
        // TODO location if geocoding is on
        assert true;
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
    }

    /**
     * Create a *fake* Status object. This does not represent a real tweet!
     * Uses: few and far between. There is no real contract as to how objects
     * made in this way will be have.
     * <p>
     * If you want to post a tweet (and hence get a real Status object), use
     * {@link Twitter#setStatus(String)}.
     * 
     * @param user
     *          Can be null or bogus -- provided that's OK with your code.
     * @param text
     *          Can be null or bogus -- provided that's OK with your code.
     * @param id
     *          Can be null or bogus -- provided that's OK with your code.
     */
    @Deprecated
    public Status(User user, String text, long id) {
      this.text = text;
      this.user = user;
      createdAt = new Date();
      this.id = id;
      inReplyToStatusId = null;
      source = "fake";
    }

    public Date getCreatedAt() {
      return createdAt;
    }

    /**
     * @return The Twitter id for this post. This is used by some API methods.
     */
    public long getId() {
      return id;
    }

    /**
     * @return list of \@mentioned people (there is no guarantee that these
     *         mentions are for correct Twitter screen-names). May be empty,
     *         never null. Screen-names are always lowercased.
     */
    public List<String> getMentions() {
      Matcher m = AT_YOU_SIR.matcher(text);
      List<String> list = new ArrayList<String>(2);
      while (m.find()) {
        // skip email addresses (and other poorly formatted things)
        if (m.start() != 0 && text.charAt(m.start() - 1) != ' ')
          continue;
        String mention = m.group(1);
        // enforce lower case
        list.add(mention.toLowerCase());
      }
      return list;
    }

    /** The actual status text. This is also returned by {@link #toString()} */
    public String getText() {
      return text;
    }

    public User getUser() {
      return user;
    }

    /**
     * @return The text of this status. E.g. "Kicking fommil's arse at
     *         Civilisation."
     */

    @Override
    public String toString() {
      return text;
    }
  }

  /**
   * A Twitter user. Fields are null if unset.
   * 
   * @author daniel
   */
  public static final class User {
    /**
     * Convert from a JSON array into a list of users.
     * 
     * @param json
     * @throws TwitterException
     */
    static List<User> getUsers(String json) throws TwitterException {
      if (json.trim().equals(""))
        return Collections.emptyList();
      try {
        List<User> users = new ArrayList<User>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
          JSONObject obj = arr.getJSONObject(i);
          User u = new User(obj, null);
          users.add(u);
        }
        return users;
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
    }

    public final String description;
    public final long id;
    public final String location;
    /** The display name, e.g. "Daniel Winterstein" */
    public final String name;
    /**
     * The url for the user's Twitter profile picture.
     * <p>
     * Note: we allow this to be edited as a convenience for the User objects
     * generated by search
     */
    public URI profileImageUrl;
    /**
     * true if this user keeps their updates private
     */
    public final boolean protectedUser;
    /**
     * The login name, e.g. "winterstein" This is the only thing used by
     * equals() and hashcode(). This is always lower-case, as Twitter
     * screen-names are case insensitive.
     */
    public final String screenName;
    /**
     * The user's current status - *if* returned by Twitter. Not all calls
     * return this, so can be null.
     */
    public final Status status;
    public final URI website;
    /**
     * Number of seconds between a user's registered time zone and Greenwich
     * Mean Time (GMT) - aka Coordinated Universal Time or UTC. Can be positive
     * or negative.
     */
    public final int timezoneOffSet;
    public final String timezone;
    public int followersCount;
    public final String profileBackgroundColor;
    public final String profileLinkColor;
    public final String profileTextColor;
    public final String profileSidebarFillColor;
    public final String profileSidebarBorderColor;

    /**
     * number of people this user is following
     */
    public final int friendsCount;

    public Date createdAt;

    public final int favoritesCount;

    public final URI profileBackgroundImageUrl;

    public final boolean profileBackgroundTile;

    public final int statusesCount;

    public final boolean notifications;

    public final boolean verified;
    public final boolean following;

    /**
     * Create a User from a json blob
     * 
     * @param obj
     * @param status
     *          can be null
     * @throws TwitterException
     */
    User(JSONObject obj, Status status) throws TwitterException {
      try {
        id = obj.getLong("id");
        name = unencode(jsonGet("name", obj));
        screenName = jsonGet("screen_name", obj).toLowerCase();
        location = jsonGet("location", obj);
        description = unencode(jsonGet("description", obj));
        String img = jsonGet("profile_image_url", obj);
        profileImageUrl = img == null ? null : URI(img);
        String url = jsonGet("url", obj);
        website = url == null ? null : URI(url);
        protectedUser = obj.getBoolean("protected");
        followersCount = obj.getInt("followers_count");
        profileBackgroundColor = obj.getString("profile_background_color");
        profileLinkColor = obj.getString("profile_link_color");
        profileTextColor = obj.getString("profile_text_color");
        profileSidebarFillColor = obj.getString("profile_sidebar_fill_color");
        profileSidebarBorderColor = obj
            .getString("profile_sidebar_border_color");
        friendsCount = obj.getInt("friends_count");
        String c = jsonGet("created_at", obj);
        try {
          createdAt = dateFormat.parse(c);
        } catch (ParseException e) {
          e.printStackTrace();
          createdAt = new Date();
        }
        favoritesCount = obj.getInt("favourites_count");
        String utcOffSet = jsonGet("utc_offset", obj);
        timezoneOffSet = utcOffSet == null ? 0 : Integer.parseInt(utcOffSet);
        timezone = jsonGet("time_zone", obj);
        img = jsonGet("profile_background_image_url", obj);
        profileBackgroundImageUrl = img == null ? null : URI(img);
        profileBackgroundTile = obj.getBoolean("profile_background_tile");
        statusesCount = obj.getInt("statuses_count");
        notifications = obj.optBoolean("notifications");
        verified = obj.optBoolean("verified");
        following = obj.optBoolean("following");
        // status
        if (status == null) {
          JSONObject s = obj.optJSONObject("status");
          this.status = s == null ? null : new Status(s, this);
        } else {
          this.status = status;
        }
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
    }

    /**
     * Create a dummy User object. All fields are set to null. This will be
     * equals() to an actual User object, so it can be used to query
     * collections. E.g. <code><pre>
     * // Test whether jtwit is a friend
     * twitter.getFriends().contains(new User("jtwit"));
     * </pre></code>
     * 
     * @param screenName
     *          This will be converted to lower-case as Twitter screen-names are
     *          case insensitive
     */
    public User(String screenName) {
      id = -1;
      name = null;
      this.screenName = screenName.toLowerCase();
      status = null;
      location = null;
      description = null;
      profileImageUrl = null;
      website = null;
      protectedUser = false;
      followersCount = 0;
      profileBackgroundColor = null;
      profileLinkColor = null;
      profileTextColor = null;
      profileSidebarFillColor = null;
      profileSidebarBorderColor = null;
      friendsCount = 0;
      createdAt = null;
      favoritesCount = 0;
      timezoneOffSet = -1;
      timezone = null;
      profileBackgroundImageUrl = null;
      profileBackgroundTile = false;
      statusesCount = 0;
      notifications = false;
      verified = false;
      following = false;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other)
        return true;
      if (!(other instanceof User))
        return false;
      User ou = (User) other;
      if (screenName.equals(ou.screenName))
        return true;
      return false;
    }

    public Date getCreatedAt() {
      return createdAt;
    }

    public String getDescription() {
      return description;
    }

    /** number of statuses a user has marked as favorite */
    public int getFavoritesCount() {
      return favoritesCount;
    }

    public int getFollowersCount() {
      return followersCount;
    }

    /**
     * @return number of people this user is following
     */
    public int getFriendsCount() {
      return friendsCount;
    }

    public long getId() {
      return id;
    }

    public String getLocation() {
      return location;
    }

    /**
     * The display name, e.g. "Daniel Winterstein"
     * 
     * @see #getScreenName()
     * */
    public String getName() {
      return name;
    }

    public String getProfileBackgroundColor() {
      return profileBackgroundColor;
    }

    public URI getProfileBackgroundImageUrl() {
      return profileBackgroundImageUrl;
    }

    public URI getProfileImageUrl() {
      return profileImageUrl;
    }

    public String getProfileLinkColor() {
      return profileLinkColor;
    }

    public String getProfileSidebarBorderColor() {
      return profileSidebarBorderColor;
    }

    public String getProfileSidebarFillColor() {
      return profileSidebarFillColor;
    }

    public String getProfileTextColor() {
      return profileTextColor;
    }

    public boolean getProtectedUser() {
      return protectedUser;
    }

    /** The login name, e.g. "winterstein". Never null */
    public String getScreenName() {
      return screenName;
    }

    /**
     * The user's current status - *if* returned by Twitter. Not all calls
     * return this, so can be null.
     */
    public Status getStatus() {
      return status;
    }

    public int getStatusesCount() {
      return statusesCount;
    }

    /**
     * String version of the timezone
     */
    public String getTimezone() {
      return timezone;
    }

    /**
     * Number of seconds between a user's registered time zone and Greenwich
     * Mean Time (GMT) - aka Coordinated Universal Time or UTC. Can be positive
     * or negative.
     */
    public int getTimezoneOffSet() {
      return timezoneOffSet;
    }

    public URI getWebsite() {
      return website;
    }

    @Override
    public int hashCode() {
      return screenName.hashCode();
    }

    /**
     * @return true if this is a dummy User object, in which case almost all of
     *         it's fields will be null - with the exception of screenName and
     *         possibly {@link #profileImageUrl}. Dummy User objects are
     *         equals() to full User objects.
     */
    public boolean isDummyObject() {
      return name == null;
    }

    public boolean isFollowing() {
      return following;
    }

    public boolean isNotifications() {
      return notifications;
    }

    public boolean isProfileBackgroundTile() {
      return profileBackgroundTile;
    }

    /**
     * true if this user keeps their updates private
     */
    public boolean isProtectedUser() {
      return protectedUser;
    }

    /**
     * @return true if the account has been verified by Twitter to really be who
     *         it claims to be.
     */
    public boolean isVerified() {
      return verified;
    }

    /**
     * Returns the User's screenName (i.e. their Twitter login)
     */
    @Override
    public String toString() {
      return screenName;
    }
  }

  /**
   * JTwitter version
   */
  public final static String version = "1.6.0";

  /**
   * Create a map from a list of key, value pairs. An easy way to make small
   * maps, basically the equivalent of {@link Arrays#asList(Object...)}.
   */
  @SuppressWarnings("unchecked")
  static <K, V> Map<K, V> asMap(Object... keyValuePairs) {
    assert keyValuePairs.length % 2 == 0;
    Map m = new HashMap(keyValuePairs.length / 2);
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      m.put(keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return m;
  }

  /**
   * Twitter html encodes some entities: ", ', <, >, &
   * 
   * @param text
   *          Can be null (which returns null)
   * @return normal-ish text
   */
  static String unencode(String text) {
    if (text == null)
      return null;
    // TODO use Jakarta to handle all html entities
    text = text.replace("&quot;", "\"");
    text = text.replace("&apos;", "'");
    text = text.replace("&nbsp;", " ");
    text = text.replace("&amp;", "&");
    text = text.replace("&gt;", ">");
    text = text.replace("&lt;", "<");
    // if (Pattern.compile("&\\w+;").matcher(text).find()) {
    // System.out.print(text);
    // }
    return text;
  }

  /**
   * Convenience method for making Dates. Because Date is a tricksy bugger of a
   * class.
   * 
   * @param year
   * @param month
   * @param day
   * @return date object
   */
  public static Date getDate(int year, String month, int day) {
    try {
      Field field = GregorianCalendar.class.getField(month.toUpperCase());
      int m = field.getInt(null);
      Calendar date = new GregorianCalendar(year, m, day);
      return date.getTime();
    } catch (Exception x) {
      throw new IllegalArgumentException(x.getMessage());
    }
  }

  /**
   * Convenience method: Finds a user with the given screen-name from the list.
   * 
   * @param screenName
   *          aka login name
   * @param users
   * @return User with the given name, or null.
   */
  public static User getUser(String screenName, List<User> users) {
    assert screenName != null && users != null;
    for (User user : users) {
      if (screenName.equals(user.screenName))
        return user;
    }
    return null;
  }

  /** Helper method to deal with JSON-in-Java weirdness */
  protected static String jsonGet(String key, JSONObject jsonObj) {
    Object val = jsonObj.opt(key);
    if (val == null)
      return null;
    if (JSONObject.NULL.equals(val))
      return null;
    return val.toString();
  }

  /**
   * 
   * @param args
   *          Can be used as a command-line tweet tool. To do so, enter 3
   *          arguments: name, password, tweet
   * 
   *          If empty, prints version info.
   */
  public static void main(String[] args) {
    // Post a tweet if we are handed a name, password and tweet
    if (args.length == 3) {
      Twitter tw = new Twitter(args[0], args[1]);
      // int s = 0;
      // List<Long> fids = tw.getFollowerIDs();
      // for (Long fid : fids) {
      // User f = tw.follow(""+fid);
      // if (f!=null) s++;
      // }
      Status s = tw.setStatus(args[2]);
      System.out.println(s);
      return;
    }
    System.out.println("Java interface for Twitter");
    System.out.println("--------------------------");
    System.out.println("Version " + version);
    System.out.println("Released under LGPL by Winterwell Associates Ltd.");
    System.out.println("See source code or JavaDoc for details on how to use.");
  }

  /**
   * Convert to a URI, or return null if this is badly formatted
   */
  private static URI URI(String uri) {
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      return null; // Bad syntax
    }
  }

  private String sourceApp = "jtwitter-yamba";

  /**
   * Gets used once then reset to null by {@link #addStandardishParameters(Map)}
   * . Gets updated in the while loops of methods doing a get-all-pages.
   */
  private Integer pageNumber;

  private Long sinceId;

  private Date sinceDate;

  private Date untilDate;

  /**
   * Provides support for fetching many pages
   */
  private int maxResults = -1;

  private final IHttpClient http;

  /**
   * Twitter login name. Can be null even if we have authentication when using
   * OAuth
   */
  private final String name;

  /**
   * Create a Twitter client without specifying a user.
   */
  public Twitter() {
    this(null, new URLConnectionHttpClient());
  }

  /**
   * Java wrapper for the Twitter API.
   * 
   * @param name
   *          the authenticating user's name, if known. Can be null.
   * @param client
   */
  public Twitter(String name, IHttpClient client) {
    this.name = name;
    http = client;
  }

  /**
   * Java wrapper for the Twitter API.
   * 
   * @param screenName
   *          The name of the Twitter user. Only used by some methods. Can be
   *          null if you avoid methods requiring authentication.
   * @param password
   *          The password of the Twitter user. Can be null if you avoid methods
   *          requiring authentication.
   */
  public Twitter(String screenName, String password) {
    this(screenName, new URLConnectionHttpClient(screenName, password));
  }

  /**
   * Add in since_id, page and count, if set. This is called by methods that
   * return lists of statuses or messages.
   * 
   * @param vars
   * @return vars
   */
  private Map<String, String> addStandardishParameters(Map<String, String> vars) {
    if (sinceId != null)
      vars.put("since_id", sinceId.toString());
    if (pageNumber != null) {
      vars.put("page", pageNumber.toString());
      // this is used once only
      pageNumber = null;
    }
    if (count != null) {
      vars.put("count", count.toString());
    }
    return vars;
  }

  Integer count;

  private String lang;

  /**
   * *Some* methods - the timeline ones for example - allow a count of
   * number-of-tweets to return.
   * 
   * @param count
   *          null for default behaviour. 200 is the current maximum. Twitter
   *          may reject or ignore high counts.
   */
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   * Create a map from a list of key/value pairs.
   * 
   * @param keyValuePairs
   * @return
   */
  private Map<String, String> aMap(String... keyValuePairs) {
    HashMap<String, String> map = new HashMap<String, String>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put(keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }

  /**
   * Equivalent to {@link #follow(String)}. C.f.
   * http://apiwiki.twitter.com/Migrating-to-followers-terminology
   * 
   * @param username
   *          Required. The screen name of the user to befriend.
   * @return The befriended user.
   * @deprecated Use {@link #follow(String)} instead, which is equivalent.
   */
  @Deprecated
  public User befriend(String username) throws TwitterException {
    return follow(username);
  }

  /**
   * Equivalent to {@link #stopFollowing(String)}.
   * 
   * @deprecated Please use {@link #stopFollowing(String)} instead.
   */
  @Deprecated
  public User breakFriendship(String username) {
    return stopFollowing(username);
  }

  /**
   * Filter keeping only those messages that come between sinceDate and
   * untilDate (if either or both are set).
   * 
   * @param list
   * @return filtered list
   */
  private <T extends ITweet> List<T> dateFilter(List<T> list) {
    if (sinceDate == null && untilDate == null)
      return list;
    ArrayList<T> filtered = new ArrayList<T>(list.size());
    for (T message : list) {
      // this code could prob be cleaned up a bit...
      if (message.getCreatedAt() == null) {
        filtered.add(message);
        continue;
      }
      if (notTooEarly(message) && notTooLate(message)) {
        filtered.add(message);
      }
    }
    return filtered;
  }

  /**
   * Is a message later than sinceDate (if that's set)?
   * 
   * @param <T>
   * @param message
   * @return
   */
  private <T extends ITweet> boolean notTooEarly(T message) {
    return (sinceDate == null) || (sinceDate.before(message.getCreatedAt()));
  }

  /**
   * Is a message earlier than untilDate (if that's set)?
   * 
   * @param <T>
   * @param message
   * @return
   */
  private <T extends ITweet> boolean notTooLate(T message) {
    return (untilDate == null) || (untilDate.after(message.getCreatedAt()));
  }

  /**
   * Destroys the status specified by the required ID parameter. The
   * authenticating user must be the author of the specified status.
   * 
   */
  public void destroyStatus(long id) throws TwitterException {
    String page = post(TWITTER_URL + "/statuses/destroy/" + id + ".json", null,
        true);
    // Note: Sends two HTTP requests to Twitter rather than one: Twitter appears
    // not to make deletions visible until the user's status page is requested.
    flush();
    assert page != null;
  }

  /**
   * Destroys the given status. Equivalent to {@link #destroyStatus(int)}. The
   * authenticating user must be the author of the status post.
   */
  public void destroyStatus(Status status) throws TwitterException {
    destroyStatus(status.getId());
  }

  void flush() {
    // This seems to prompt twitter to update in some cases!
    http.getPage("http://twitter.com/" + name, null, true);
  }

  /**
   * Start following a user.
   * 
   * @param username
   *          Required. The ID or screen name of the user to befriend.
   * @return The befriended user, or null if they were already being followed.
   * @throws TwitterException
   *           if the user does not exist or has been suspended.
   */
  public User follow(String username) throws TwitterException {
    if (username == null)
      throw new NullPointerException();
    if (username.equals(getScreenName())) {
      throw new IllegalArgumentException("follow yourself makes no sense");
    }
    String page;
    try {
      Map<String, String> vars = newMap("screen_name", username);
      page = post(TWITTER_URL + "/friendships/create.json", vars, true);
      // is this needed? doesn't seem to fix things
      // http.getPage(TWITTER_URL+"/friends", null, true);
    } catch (E403 e) {
      // check if we've tried to follow someone we're already
      // following
      if (isFollowing(username)) {
        return null;
      }
      throw e;
    }
    try {
      return new User(new JSONObject(page), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Convenience for {@link #follow(String)}
   * 
   * @param user
   */
  public void follow(User user) {
    follow(user.screenName);
  }

  /**
   * Returns a list of the direct messages sent to the authenticating user.
   * <p>
   * Note: the Twitter API makes this available in rss if that's of interest.
   */
  public List<Message> getDirectMessages() {
    return getMessages(TWITTER_URL + "/direct_messages.json",
        standardishParameters());
  }

  /**
   * Returns a list of the direct messages sent *by* the authenticating user.
   */
  public List<Message> getDirectMessagesSent() {
    return getMessages(TWITTER_URL + "/direct_messages/sent.json",
        standardishParameters());
  }

  /**
   * The most recent 20 favourite tweets. (Note: This can use page - and page
   * only - to fetch older favourites).
   */
  public List<Status> getFavorites() {
    return getStatuses(TWITTER_URL + "/favorites.json",
        standardishParameters(), true);
  }

  public void setFavorite(Status status, boolean isFavorite) {
    String uri = isFavorite ? TWITTER_URL + "/favorites/create/" + status.id
        + ".json" : TWITTER_URL + "/favorites/destroy/" + status.id + ".json";
    http.getPage(uri, null, true);
  }

  /**
   * The most recent 20 favourite tweets for the given user. (Note: This can use
   * page - and page only - to fetch older favourites).
   * 
   * @param screenName
   *          login-name.
   */
  public List<Status> getFavorites(String screenName) {
    Map<String, String> vars = newMap("screen_name", screenName);
    return getStatuses(TWITTER_URL + "/favorites.json",
        addStandardishParameters(vars), http.canAuthenticate());
  }

  /**
   * Returns a list of the users currently featured on the site with their
   * current statuses inline.
   * <p>
   * Note: This is no longer part of the Twitter API. Support is provided via
   * other methods.
   */
  public List<User> getFeatured() throws TwitterException {
    List<User> users = new ArrayList<User>();
    List<Status> featured = getPublicTimeline();
    for (Status status : featured) {
      User user = status.getUser();
      users.add(user);
    }
    return users;
  }

  /**
   * Returns the IDs of the authenticating user's followers.
   * 
   * @throws TwitterException
   */
  public List<Long> getFollowerIDs() throws TwitterException {
    return getUserIDs(TWITTER_URL + "/followers/ids.json", null);
  }

  /**
   * Returns the IDs of the specified user's followers.
   * 
   * @param The
   *          screen name of the user whose followers are to be fetched.
   * @throws TwitterException
   */
  public List<Long> getFollowerIDs(String screenName) throws TwitterException {
    return getUserIDs(TWITTER_URL + "/followers/ids.json", screenName);
  }

  /**
   * Returns the authenticating user's (latest) followers, each with current
   * status inline. Occasionally contains duplicates.
   */
  public List<User> getFollowers() throws TwitterException {
    return getUsers(TWITTER_URL + "/statuses/followers.json", null);
  }

  /**
   * 
   * Returns the (latest 100) given user's followers, each with current status
   * inline. Occasionally contains duplicates.
   * 
   * @param username
   *          The screen name of the user for whom to request a list of friends.
   * @throws TwitterException
   */

  public List<User> getFollowers(String username) throws TwitterException {
    return getUsers(TWITTER_URL + "/statuses/followers.json", username);
  }

  /**
   * Returns the IDs of the authenticating user's friends. (people who the user
   * follows).
   * 
   * @throws TwitterException
   */
  public List<Long> getFriendIDs() throws TwitterException {
    return getUserIDs(TWITTER_URL + "/friends/ids.json", null);
  }

  /**
   * Returns the IDs of the specified user's friends. Occasionally contains
   * duplicates.
   * 
   * @param The
   *          screen name of the user whose friends are to be fetched.
   * @throws TwitterException
   */
  public List<Long> getFriendIDs(String screenName) throws TwitterException {
    return getUserIDs(TWITTER_URL + "/friends/ids.json", screenName);
  }

  /**
   * Returns the authenticating user's (latest 100) friends, each with current
   * status inline. NB - friends are people who *you* follow. Occasionally
   * contains duplicates.
   * <p>
   * Note that there seems to be a small delay from Twitter in updates to this
   * list.
   * 
   * @throws TwitterException
   * @see #getFriendIDs()
   * @see #isFollowing(String)
   */
  public List<User> getFriends() throws TwitterException {
    return getUsers(TWITTER_URL + "/statuses/friends.json", null);
  }

  /**
   * 
   * Returns the (latest 100) given user's friends, each with current status
   * inline. Occasionally contains duplicates.
   * 
   * @param username
   *          The screen name of the user for whom to request a list of friends.
   * @throws TwitterException
   */
  public List<User> getFriends(String username) throws TwitterException {
    return getUsers(TWITTER_URL + "/statuses/friends.json", username);
  }

  /**
   * Returns the 20 most recent statuses posted in the last 24 hours from the
   * authenticating user and that user's friends.
   */
  public List<Status> getFriendsTimeline() throws TwitterException {
    return getStatuses(TWITTER_URL + "/statuses/friends_timeline.json",
        standardishParameters(), true);
  }

  /**
   * Returns the 20 most recent statuses posted in the last 24 hours from the
   * authenticating user and that user's friends, including retweets.
   */
  public List<Status> getHomeTimeline() throws TwitterException {
    return getStatuses(TWITTER_URL + "/statuses/home_timeline.json",
        standardishParameters(), true);
  }

  /**
   * 
   * @param url
   * @param var
   * @param isPublic
   *          Value to set for Message.isPublic
   * @return
   */
  private List<Message> getMessages(String url, Map<String, String> var) {
    // Default: 1 page
    if (maxResults < 1) {
      List<Message> msgs = Message.getMessages(http.getPage(url, var, true));
      msgs = dateFilter(msgs);
      return msgs;
    }
    // Fetch all pages until we run out
    // -- or Twitter complains in which case you'll get an exception
    pageNumber = 1;
    List<Message> msgs = new ArrayList<Message>();
    while (msgs.size() <= maxResults) {
      String p = http.getPage(url, var, true);
      List<Message> nextpage = Message.getMessages(p);
      nextpage = dateFilter(nextpage);
      msgs.addAll(nextpage);
      if (nextpage.size() < 20)
        break;
      pageNumber++;
      var.put("page", Integer.toString(pageNumber));
    }
    return msgs;
  }

  /**
   * @return Login name of the authenticating user, or null if not set.
   */
  public String getScreenName() {
    return name;
  }

  /**
   * Returns the 20 most recent statuses from non-protected users who have set a
   * custom user icon. Does not require authentication.
   * <p>
   * Note: Twitter cache-and-refresh this every 60 seconds, so there is little
   * point calling it more frequently than that.
   */
  public List<Status> getPublicTimeline() throws TwitterException {
    return getStatuses(TWITTER_URL + "/statuses/public_timeline.json",
        standardishParameters(), false);
  }

  /**
   * @return the remaining number of API requests available to the
   *         authenticating user before the API limit is reached for the current
   *         hour. <i>If this is negative you should stop using Twitter with
   *         this login for a bit.</i> Note: Calls to rate_limit_status do not
   *         count against the rate limit.
   */
  public int getRateLimitStatus() {
    String json = http.getPage(TWITTER_URL + "/account/rate_limit_status.json",
        null, http.canAuthenticate());
    try {
      JSONObject obj = new JSONObject(json);
      int hits = obj.getInt("remaining_hits");
      return hits;
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Returns the 20 most recent replies/mentions (status updates with
   * 
   * @username) to the authenticating user. Replies are only available to the
   *            authenticating user; you can not request a list of replies to
   *            another user whether public or protected.
   *            <p>
   *            The Twitter API now refers to replies as <i>mentions</i>. We
   *            have kept the old terminology here.
   */
  public List<Status> getReplies() throws TwitterException {
    return getStatuses(TWITTER_URL + "/statuses/replies.json",
        standardishParameters(), true);
  }

  /**
   * @return those of your tweets that have been retweeted. It's a bit of a
   *         strange one this. You can then query who retweeted you.
   */
  public List<Status> getRetweetsOfMe() {
    String url = TWITTER_URL + "/statuses/retweets_of_me.json";
    Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
    String json = http.getPage(url, vars, true);
    return Status.getStatuses(json);
  }

  public List<TwitterList> getLists() {
    return getLists(name);
  }

  /**
   * @param screenName
   * @return the (first 20) lists from the given user
   */
  public List<TwitterList> getLists(String screenName) {
    assert screenName != null;
    try {
      String url = TWITTER_URL + "/" + screenName + "/lists.json";
      String listsJson = http.getPage(url, null, true);
      JSONObject wrapper = new JSONObject(listsJson);
      JSONArray jarr = (JSONArray) wrapper.get("lists");
      List<TwitterList> lists = new ArrayList<TwitterList>();
      for (int i = 0; i < jarr.length(); i++) {
        JSONObject li = jarr.getJSONObject(i);
        TwitterList twList = new TwitterList(li, this);
        lists.add(twList);
      }
      return lists;
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * @return *new-style* retweets of this tweet. This does not include old-style
   *         "manual" retweets.
   *         <p>
   *         Dear developers: we could have it that this method made two calls
   *         to Twitter and merged the two lists, thereby getting both types of
   *         retweet. There are +s and -s to this, so I'm undecided. -- Daniel
   */
  public List<Status> getRetweets(Status tweet) {
    String url = TWITTER_URL + "/statuses/retweets/" + tweet.id + ".json";
    Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
    String json = http.getPage(url, vars, true);
    List<Status> users = Status.getStatuses(json);
    return users;
    // // Should we also do by search and merge the two lists?
    // String[] words = tweet.text.split("\\s");
    // StringBuilder sq = new StringBuilder();
    // sq.append("rt @"+tweet.getUser().getScreenName());
    // for (String w : words) {
    // // TODO ignore some stop words - esp those which can be shortened by txt
    // speak
    // sq.append(" ");
    // sq.append(w);
    // }
    // return search(sq.toString());
  }

  /**
   * Show users who (new-style) retweeted the given tweet. Can use count (up to
   * 100) and page.
   * 
   * @param tweet
   */
  public List<User> getRetweeters(Status tweet) {
    String url = TWITTER_URL + "/statuses/" + tweet.id + "/retweeted_by.json";
    Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
    String json = http.getPage(url, vars, true);
    List<User> users = User.getUsers(json);
    return users;
  }

  /**
   * @return The current status of the user. Warning: this is null if (a) unset
   *         (ie if this user has never tweeted), or (b) their last tweet was a
   *         new-style retweet!
   */
  public Status getStatus() throws TwitterException {
    Map<String, String> vars = asMap("count", 1);
    String json = http.getPage(TWITTER_URL + "/statuses/user_timeline.json",
        vars, true);
    List<Status> statuses = Status.getStatuses(json);
    if (statuses.size() == 0)
      return null;
    return statuses.get(0);
  }

  /**
   * Returns a single status, specified by the id parameter below. The status's
   * author will be returned inline.
   * 
   * @param id
   *          The numerical ID of the status you're trying to retrieve.
   */
  public Status getStatus(long id) throws TwitterException {
    String json = http.getPage(TWITTER_URL + "/statuses/show/" + id + ".json",
        null, true);
    try {
      return new Status(new JSONObject(json), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * @return The current status of the given user.
   */
  public Status getStatus(String username) throws TwitterException {
    assert username != null;
    // why 2? workaround for weird bug found by user aagha, where
    // Twitter was blanking one of his tweets.
    Map<String, String> vars = asMap("id", username, "count", 2);
    String json = http.getPage(TWITTER_URL + "/statuses/user_timeline.json",
        vars, false);
    List<Status> statuses = Status.getStatuses(json);
    if (statuses.size() == 0)
      return null;
    return statuses.get(0);

  }

  /**
   * 
   * @param url
   * @param var
   * @param authenticate
   * @return
   */
  private List<Status> getStatuses(String url, Map<String, String> var,
      boolean authenticate) {
    // Default: 1 page
    if (maxResults < 1) {
      List<Status> msgs = Status.getStatuses(http.getPage(url, var,
          authenticate));
      msgs = dateFilter(msgs);
      return msgs;
    }
    // Fetch all pages until we run out
    // -- or Twitter complains in which case you'll get an exception
    pageNumber = 1;
    List<Status> msgs = new ArrayList<Status>();
    while (msgs.size() <= maxResults) {
      List<Status> nextpage = Status.getStatuses(http.getPage(url, var,
          authenticate));
      nextpage = dateFilter(nextpage);
      msgs.addAll(nextpage);
      if (nextpage.size() < 20)
        break;
      pageNumber++;
      var.put("page", Integer.toString(pageNumber));
    }
    return msgs;
  }

  /**
   * 
   * @param url
   *          API method to call
   * @param screenName
   * @return twitter-id numbers for friends/followers of screenName Is affected
   *         by {@link #maxResults}
   */
  private List<Long> getUserIDs(String url, String screenName) {
    Long cursor = -1L;
    List<Long> ids = new ArrayList<Long>();
    Map<String, String> vars = newMap("screen_name", screenName);
    while (cursor != 0 && !enoughResults(ids)) {
      vars.put("cursor", String.valueOf(cursor));
      String json = http.getPage(url, vars, http.canAuthenticate());
      try {
        JSONObject jobj = new JSONObject(json);
        JSONArray jarr = (JSONArray) jobj.get("ids");
        for (int i = 0; i < jarr.length(); i++) {
          ids.add(jarr.getLong(i));
        }
        cursor = new Long(jobj.getString("next_cursor"));
      } catch (JSONException e) {
        throw new TwitterException("Could not parse id list" + e);
      }
    }
    return ids;
  }

  /**
   * Have we got enough results for the current search?
   * 
   * @param list
   * @return false if maxResults is set to -1 (ie, unlimited) or if list
   *         contains less than maxResults results.
   */
  private <X> boolean enoughResults(List<X> list) {
    return (maxResults != -1 && list.size() >= maxResults);
  }

  /**
   * Convenience method for building small maps.
   * 
   * @param keyValuePairs
   * @return map with these settings
   */
  private Map<String, String> newMap(String... keyValuePairs) {
    HashMap<String, String> map = new HashMap<String, String>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      map.put(keyValuePairs[i], keyValuePairs[i + 1]);
    }
    return map;
  }

  /**
   * Low-level method for fetching e.g. your friends
   * 
   * @param url
   * @param screenName
   *          e.g. your screen-name
   * @return
   */
  private List<User> getUsers(String url, String screenName) {
    Map<String, String> vars = newMap("screen_name", screenName);
    List<User> users = new ArrayList<User>();
    Long cursor = -1L;
    while (cursor != 0 && !enoughResults(users)) {
      vars.put("cursor", cursor.toString());
      JSONObject jobj;
      try {
        jobj = new JSONObject(http.getPage(url, vars, http.canAuthenticate()));
        users.addAll(User.getUsers(jobj.getString("users")));
        cursor = new Long(jobj.getString("next_cursor"));
      } catch (JSONException e) {
        throw new TwitterException("Could not parse user listing: " + e);
      }
    }
    return users;
  }

  /**
   * Returns the 20 most recent statuses posted in the last 24 hours from the
   * authenticating user.
   */
  public List<Status> getUserTimeline() throws TwitterException {
    return getStatuses(TWITTER_URL + "/statuses/user_timeline.json",
        standardishParameters(), true);
  }

  /**
   * Returns the most recent statuses posted in the last 24 hours from the given
   * user.
   * <p>
   * This method will authenticate if it can (i.e. if the Twitter object has a
   * username and password). Authentication is needed to see the posts of a
   * private user.
   * 
   * @param screenName
   *          Can be null. Specifies the screen name of the user for whom to
   *          return the user_timeline.
   */
  public List<Status> getUserTimeline(String screenName)
      throws TwitterException {
    Map<String, String> vars = asMap("screen_name", screenName);
    addStandardishParameters(vars);
    // Should we authenticate?
    boolean authenticate = http.canAuthenticate();
    String json = http.getPage(TWITTER_URL + "/statuses/user_timeline.json",
        vars, authenticate);
    return Status.getStatuses(json);
  }

  /**
   * Is the authenticating user <i>followed by</i> userB?
   * 
   * @param userB
   *          The screen name of a Twitter user.
   * @return Whether or not the user is followed by userB.
   */
  public boolean isFollower(String userB) {
    return isFollower(userB, name);
  }

  /**
   * @return true if followerScreenName <i>is</i> following followedScreenName
   * 
   * @throws TwitterException.E403
   *           if one of the users has protected their updates and you don't
   *           have access. This can be counter-intuitive (and annoying) at
   *           times!
   */
  public boolean isFollower(String followerScreenName, String followedScreenName) {
    assert followerScreenName != null && followedScreenName != null;
    String page = http.getPage(TWITTER_URL + "/friendships/exists.json", aMap(
        "user_a", followerScreenName, "user_b", followedScreenName), http
        .canAuthenticate());
    return Boolean.valueOf(page);
  }

  /**
   * Does the authenticating user <i>follow</i> userB?
   * 
   * @param userB
   *          The screen name of a Twitter user.
   * @return Whether or not the user follows userB.
   */
  public boolean isFollowing(String userB) {
    if (isFollower(name, userB))
      return true;
    // // hopefully temporary workarounds for hopefully temporary issues with
    // Twitter's follower API
    // // Note: These do not appear to help! Left in 'cos what harm can they do?
    // List<User> friends = getFriends();
    // User b = new User(userB);
    // if (friends.contains(b)) return true;
    // long bid = show(userB).getId();
    // List<Long> fids = getFriendIDs();
    // if (fids.contains(bid)) return true;
    return false;
  }

  /**
   * Convenience for {@link #isFollowing(String)}
   * 
   * @param user
   */
  public boolean isFollowing(User user) {
    return isFollowing(user.screenName);
  }

  /**
   * Are the login details used for authentication valid?
   */
  public boolean isValidLogin() {
    try {
      getDirectMessages();
      return true;
    } catch (TwitterException.E403 e) {
      return false;
    } catch (TwitterException.E401 e) {
      return false;
    } catch (TwitterException e) {
      throw e;
    }
  }

  /**
   * Switches off notifications for updates from the specified user <i>who must
   * already be a friend</i>.
   * 
   * @param screenName
   *          Stop getting notifications from this user, who must already be one
   *          of your friends.
   * @return the specified user
   */
  public User leaveNotifications(String screenName) {
    Map<String, String> vars = newMap("screen_name", screenName);
    String page = http.getPage(TWITTER_URL + "/notifications/leave.json", vars,
        true);
    try {
      return new User(new JSONObject(page), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Enables notifications for updates from the specified user <i>who must
   * already be a friend</i>.
   * 
   * @param username
   *          Get notifications from this user, who must already be one of your
   *          friends.
   * @return the specified user
   */
  public User notify(String username) {
    Map<String, String> vars = newMap("screen_name", username);
    String page = http.getPage(TWITTER_URL + "/notifications/follow.json",
        vars, true);
    try {
      return new User(new JSONObject(page), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Wrapper for {@link IHttpClient#post(String, Map, boolean)}.
   */
  private String post(String uri, Map<String, String> vars, boolean authenticate)
      throws TwitterException {
    String page = http.post(uri, vars, authenticate);
    return page;
  }

  /**
   * Perform a search of Twitter.
   * <p>
   * Warning: the User objects returned by a search (as part of the Status
   * objects) are dummy-users. The only information that is set is the user's
   * screen-name and a profile image url. This reflects the current behaviour of
   * the Twitter API. If you need more info, call {@link #show(String)} with the
   * screen name.
   * <p>
   * This supports {@link #maxResults} and pagination. A language filter can be
   * set via {@link #setLanguage(String)} TODO parameters: geocode: Optional.
   * Returns tweets by users located within a given radius of the given
   * latitude/longitude, where the user's location is taken from their Twitter
   * profile. The parameter value is specified by "latitude,longitude,radius",
   * where radius units must be specified as either "mi" (miles) or "km"
   * (kilometers). Note that you cannot use the near operator via the API to
   * geocode arbitrary locations; however you can use this geocode parameter to
   * search near geocodes directly.
   * 
   * @param searchTerm
   * @param callback
   *          an object whose process() method will be called on each new page
   *          of results.
   * @param the
   *          number of results to fetch per page
   * @return search results - up to maxResults / rpp if maxResults is positive,
   *         or rpp if maxResults is negative.
   */
  public List<Status> search(String searchTerm, ICallback callback, int rpp) {
    Map<String, String> vars;
    if (maxResults < 100 && maxResults > 0) {
      // Default: 1 page
      vars = getSearchParams(searchTerm, maxResults);
    } else {
      vars = getSearchParams(searchTerm, rpp);
    }
    // Fetch all pages until we run out
    // -- or Twitter complains in which case you'll get an exception
    pageNumber = 1;
    List<Status> allResults = new ArrayList<Status>(Math.max(maxResults, rpp));
    String url = "http://search.twitter.com/search.json";
    pageNumber = 1; // pageNumber is nulled by getSearchParams
    do {
      vars.put("page", Integer.toString(pageNumber));
      String json = http.getPage(url, vars, false);
      List<Status> stati = Status.getStatusesFromSearch(this, json);
      int numResults = stati.size();
      stati = dateFilter(stati);
      allResults.addAll(stati);
      if (callback != null) {
        // the callback may tell us to stop, by returning true
        if (callback.process(stati))
          break;
      }
      if (numResults < rpp) { // We've reached the end of the results
        break;
      }
      pageNumber++;
    } while (allResults.size() < maxResults);
    return allResults;
  }

  /**
   * Retweet a tweet without any edits. You can also retweet by starting a
   * status using the RT @username microformat (Twitter may not get it, but
   * everyone else will).
   * 
   * @param tweet
   *          This must not be one of your own or Twitter will ignore it.
   * @return your retweet
   */
  public Status retweet(Status tweet) {
    String result = post(TWITTER_URL + "/statuses/retweet/" + tweet.getId()
        + ".json", null, true);
    try {
      return new Status(new JSONObject(result), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Wrapper for {@link #search(String, ICallback, int)} with no callback and
   * fetching 100 results.
   * <p>
   * Perform a search of Twitter.
   * <p>
   * Warning: the User objects returned by a search (as part of the Status
   * objects) are dummy-users. The only information that is set is the user's
   * screen-name and a profile image url. This reflects the current behaviour of
   * the Twitter API. If you need more info, call {@link #show(String)} with the
   * screen name.
   * <p>
   * This supports {@link #maxResults} and pagination. A language filter can be
   * set via {@link #setLanguage(String)} TODO parameters: geocode: Optional.
   * Returns tweets by users located within a given radius of the given
   * latitude/longitude, where the user's location is taken from their Twitter
   * profile. The parameter value is specified by "latitude,longitude,radius",
   * where radius units must be specified as either "mi" (miles) or "km"
   * (kilometers). Note that you cannot use the near operator via the API to
   * geocode arbitrary locations; however you can use this geocode parameter to
   * search near geocodes directly.
   * 
   * @param searchTerm
   * @return search results - up to maxResults + rpp if maxResults is positive,
   *         or rpp if maxResults is negative.
   */
  public List<Status> search(String searchTerm) {
    return search(searchTerm, null, 100);
  }

  /**
   * Warning: there is a bug within twitter.com which means that location-based
   * searches are treated as OR. E.g. "John near:Scotland" will happily return
   * "Andrew from Aberdeen" :(
   * 
   * @param searchTerm
   * @return
   */
  public List<User> searchUsers(String searchTerm) {
    Map<String, String> vars = asMap("q", searchTerm);
    if (pageNumber != null) {
      vars.put("page", pageNumber.toString());
    }
    String json = http.getPage(TWITTER_URL + "/users/search.json", vars, true);
    List<User> users = User.getUsers(json);
    return users;
  }

  /**
   * @param searchTerm
   * @param rpp
   * @return
   */
  private Map<String, String> getSearchParams(String searchTerm, int rpp) {
    Map<String, String> vars = aMap("rpp", "" + rpp, "q", searchTerm);
    if (sinceId != null)
      vars.put("since_id", sinceId.toString());
    // dates are no longer supported
    // DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    // if (sinceDate != null) vars.put("since", df.format(sinceDate));
    // if (untilDate != null) vars.put("until", df.format(untilDate));
    if (lang != null)
      vars.put("lang", lang);
    if (geocode != null)
      vars.put("geocode", geocode);
    addStandardishParameters(vars);
    return vars;
  }

  private String geocode;

  private double[] myLatLong;

  /**
   * Restricts {@link #search(String)} to tweets by users located within a given
   * radius of the given latitude/longitude.
   * <p>
   * The location of a tweet is preferentially taked from the Geotagging API,
   * but will fall back to the Twitter profile.
   * 
   * @param latitude
   * @param longitude
   * @param radius
   *          E.g. 3.5mi or 2km
   */
  public void setSearchLocation(double latitude, double longitude, String radius) {
    assert radius.endsWith("mi") || radius.endsWith("km") : radius;
    geocode = latitude + "," + longitude + "," + radius;
  }

  /**
   * Set the location for your tweets.<br>
   * 
   * Warning: geo-tagging parameters are ignored if geo_enabled for the user
   * is false (this is the default setting for all users unless the user has
   * enabled geolocation in their settings)!
   * 
   * @param latitudeLongitude
   *            Can be null (which is the default), in which case your tweets
   *            will not carry location data.
   *            <p>
   *            The valid ranges for latitude is -90.0 to +90.0 (North is
   *            positive) inclusive. The valid ranges for longitude is -180.0
   *            to +180.0 (East is positive) inclusive.
   * 
   * @see #setSearchLocation(double, double, String) which is completely
   *      separate.
   */
  public void setMyLocation(double[] latitudeLongitude) {
    myLatLong = latitudeLongitude;
    if (myLatLong == null)
      return;
    if (Math.abs(myLatLong[0]) > 90)
      throw new IllegalArgumentException(myLatLong[0]
          + " is not within +/- 90");
    if (Math.abs(myLatLong[1]) > 180)
      throw new IllegalArgumentException(myLatLong[1]
          + " is not within +/- 180");
  }

  /**
   * Sends a new direct message to the specified user from the authenticating
   * user.
   * 
   * @param recipient
   *          Required. The screen name of the recipient user.
   * @param text
   *          Required. The text of your direct message. Keep it under 140
   *          characters! This should *not* include the "d username" portion
   * @return the sent message
   * @throws TwitterException.E403
   *           if the recipient is not following you. (you can \@mention anyone
   *           but you can only dm people who follow you).
   */
  public Message sendMessage(String recipient, String text)
      throws TwitterException {
    assert recipient != null;
    assert !text.startsWith("d " + recipient);
    if (text.length() > 140)
      throw new IllegalArgumentException("Message is too long.");
    Map<String, String> vars = asMap("user", recipient, "text", text);
    String result = post(TWITTER_URL + "/direct_messages/new.json", vars, true);
    try {
      return new Message(new JSONObject(result));
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * @param maxResults
   *          if greater than zero, requests will attempt to fetch as many pages
   *          as are needed! -1 by default, in which case most methods return
   *          the first 20 statuses/messages.
   *          <p>
   *          If setting a high figure, you should usually also set a sinceId or
   *          sinceDate to limit your Twitter usage. Otherwise you can easily
   *          exceed your rate limit.
   */
  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }

  /**
   * @param pageNumber
   *          null (the default) returns the first page. Pages are indexed from
   *          1. This is used once only! Then it is reset to null
   */
  public void setPageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
  }

  /**
   * Date based filter on statuses and messages. This is done client-side as
   * Twitter have - for their own inscrutable reasons - pulled support for this
   * feature.
   * <p>
   * If using this, you probably also want to increase
   * {@link #setMaxResults(int)} - otherwise you get at most 20, and possibly
   * less (since the filtering is done client side).
   * 
   * @param sinceDate
   */
  public void setSinceDate(Date sinceDate) {
    this.sinceDate = sinceDate;
  }

  /**
   * @param untilDate
   *          the untilDate to set
   */
  public void setUntilDate(Date untilDate) {
    this.untilDate = untilDate;
  }

  /**
   * TODO document this please - DBW
   * 
   * @return the untilDate
   */
  public Date getUntilDate() {
    return untilDate;
  }

  /**
   * Narrows the returned results to just those statuses created after the
   * specified status id. This will be used until it is set to null. Default is
   * null.
   * <p>
   * If using this, you probably also want to increase
   * {@link #setMaxResults(int)} (otherwise you just get the most recent 20).
   * 
   * @param statusId
   */
  public void setSinceId(Long statusId) {
    sinceId = statusId;
  }

  /**
   * Set the source application. This will be mentioned on Twitter alongside
   * status updates (with a small label saying source: myapp).
   * 
   * <i>In order for this to work, you must first register your app with Twitter
   * and get a source name from them! You must also use OAuth to connect.</i>
   * 
   * @param sourceApp
   *          jtwitterlib by default. Set to null for no source.
   */
  public void setSource(String sourceApp) {
    this.sourceApp = sourceApp;
  }

  /**
   * Sets the authenticating user's status.
   * <p>
   * Identical to {@link #updateStatus(String)}, but with a Java-style name
   * (updateStatus is the Twitter API name for this method).
   * 
   * @param statusText
   *          The text of your status update. Must not be more than 160
   *          characters and should not be more than 140 characters to ensure
   *          optimal display.
   * @return The posted status when successful.
   */
  public Status setStatus(String statusText) throws TwitterException {
    return updateStatus(statusText);
  }

  /**
   * Returns information of a given user, specified by screen name.
   * 
   * @param screenName
   *          The screen name of a user.
   * @throws exception
   *           if the user does not exist - or has been terminated (as happens
   *           to spam bots).
   * @see #show(long)
   */
  public User show(String screenName) throws TwitterException {
    Map vars = newMap("screen_name", screenName);
    String json = http.getPage(TWITTER_URL + "/users/show.json", vars, http
        .canAuthenticate());
    User user;
    try {
      user = new User(new JSONObject(json), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
    return user;
  }

  /**
   * Returns information of a given user, specified by user-id.
   * 
   * @param userId
   *          The user-id of a user.
   * @throws exception
   *           if the user does not exist - or has been terminated (as happens
   *           to spam bots).
   */
  public User show(long userId) {
    Map<String, String> vars = asMap("user_id", "" + userId);
    String json = http.getPage(TWITTER_URL + "/users/show.json", vars, http
        .canAuthenticate());
    User user;
    try {
      user = new User(new JSONObject(json), null);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
    return user;
  }

  /**
   * Lookup user info. This is done in batches of 100. Users can look up at most
   * 1000 users in an hour.
   * 
   * @param screenNames
   * @return user objects for screenNames. Warning 1: This may be less than the
   *         full set if Twitter returns an error part-way through (e.g. you hit
   *         your rate limit). Warning 2: the ordering may be different from the
   *         screenNames parameter
   * @see #bulkShowById(List)
   */
  public List<User> bulkShow(List<String> screenNames) {
    return bulkShow2(String.class, screenNames);
  }

  /**
   * Lookup user info. Same as {@link #bulkShow(List)}, but works with Twitter
   * user-ID numbers.
   * 
   * @param userIds
   */
  public List<User> bulkShowById(List<Long> userIds) {
    return bulkShow2(Long.class, userIds);
  }

  /**
   * Common backend for {@link #bulkShow(List)} and {@link #bulkShowById(List)}.
   * 
   * @param stringOrLong
   * @param screenNamesOrIds
   */
  private List<User> bulkShow2(Class stringOrLong, List screenNamesOrIds) {
    int batchSize = 100;
    ArrayList<User> users = new ArrayList<Twitter.User>(screenNamesOrIds.size());
    for (int i = 0; i < screenNamesOrIds.size(); i += batchSize) {
      StringBuilder names = new StringBuilder();
      for (int si = i, n = Math.min(i + batchSize, screenNamesOrIds.size()); si < n; si++) {
        names.append(screenNamesOrIds.get(si));
        names.append(",");
      }
      // pop final ,
      names.delete(names.length() - 1, names.length());
      String var = stringOrLong == String.class ? "screen_name" : "user_id";
      Map<String, String> vars = asMap(var, names);
      try {
        String json = http.getPage(TWITTER_URL + "/users/lookup.json", vars,
            http.canAuthenticate());
        List<User> usersi = User.getUsers(json);
        users.addAll(usersi);
      } catch (TwitterException e) {
        // stop here
        break;
      }
    }
    return users;
  }

  /**
   * Synonym for {@link #show(String)}. show is the Twitter API name, getUser
   * feels more Java-like.
   * 
   * @param screenName
   *          The screen name of a user.
   * @return the user info
   */
  public User getUser(String screenName) {
    return show(screenName);
  }

  /**
   * Synonym for {@link #show(long)}. show is the Twitter API name, getUser
   * feels more Java-like.
   * 
   * @param userId
   *          The user-id of a user.
   * @return the user info
   * @see #getUser(String)
   */
  public User getUser(long userId) {
    return show(userId);
  }

  /**
   * Split a long message up into shorter chunks suitable for use with
   * {@link #setStatus(String)} or {@link #sendMessage(String, String)}.
   * 
   * @param longStatus
   * @return longStatus broken into a list of max 140 char strings
   */
  public List<String> splitMessage(String longStatus) {
    // Is it really long?
    if (longStatus.length() <= 140)
      return Collections.singletonList(longStatus);
    // Multiple tweets for a longer post
    List<String> sections = new ArrayList<String>(4);
    StringBuilder tweet = new StringBuilder(140);
    String[] words = longStatus.split("\\s+");
    for (String w : words) {
      // messages have a max length of 140
      // plus the last bit of a long tweet tends to be hidden on
      // twitter.com, so best to chop 'em short too
      if (tweet.length() + w.length() + 1 > 140) {
        // Emit
        tweet.append("...");
        sections.add(tweet.toString());
        tweet = new StringBuilder(140);
        tweet.append(w);
      } else {
        if (tweet.length() != 0)
          tweet.append(" ");
        tweet.append(w);
      }
    }
    // Final bit
    if (tweet.length() != 0)
      sections.add(tweet.toString());
    return sections;
  }

  /**
   * Map with since_id, page and count, if set. This is called by methods that
   * return lists of statuses or messages.
   */
  private Map<String, String> standardishParameters() {
    return addStandardishParameters(new HashMap<String, String>());
  }

  /**
   * Destroy: Discontinues friendship with the user specified in the ID
   * parameter as the authenticating user.
   * 
   * @param username
   *          The screen name of the user with whom to discontinue friendship.
   * @return the un-friended user (if they were a friend), or null if the method
   *         fails because the specified user was not a friend.
   */
  public User stopFollowing(String username) {
    assert getScreenName() != null;
    try {
      Map<String, String> vars = newMap("screen_name", username);
      String page = post(TWITTER_URL + "/friendships/destroy.json", vars, true);
      // ?? is this needed to make Twitter update its cache? doesn't seem to fix
      // things
      // http.getPage(TWITTER_URL+"/friends", null, true);
      User user;
      try {
        user = new User(new JSONObject(page), null);
      } catch (JSONException e) {
        throw new TwitterException(e);
      }
      return user;
    } catch (TwitterException e) {
      // were they a friend anyway?
      if (!isFollower(getScreenName(), username)) {
        return null;
      }
      // Something else went wrong
      throw e;
    }
  }

  /**
   * Convenience for {@link #stopFollowing(String)}
   * 
   * @param user
   */
  public void stopFollowing(User user) {
    stopFollowing(user.screenName);
  }

  /**
   * Updates the authenticating user's status.
   * 
   * @param statusText
   *          The text of your status update. Must not be more than 160
   *          characters and should not be more than 140 characters to ensure
   *          optimal display.
   * @return The posted status when successful.
   */
  public Status updateStatus(String statusText) {
    return updateStatus(statusText, -1);
  }

  /**
   * Updates the authenticating user's status and marks it as a reply to the
   * tweet with the given ID.
   * 
   * @param statusText
   *          The text of your status update. Must not be more than 160
   *          characters and should not be more than 140 characters to ensure
   *          optimal display.
   * 
   * 
   * @param inReplyToStatusId
   *          The ID of the tweet that this tweet is in response to. The
   *          statusText must contain the username (with an "@" prefix) of the
   *          owner of the tweet being replied to for for Twitter to agree to
   *          mark the tweet as a reply. 0 or less to leave this unset.
   * 
   * @return The posted status when successful.
   *         <p>
   *         Warning: the microformat for direct messages is supported. BUT: the
   *         return value from this method will be null, and not the direct
   *         message. Other microformats (such as follow) may result in an
   *         exception being thrown.
   * 
   * @throws TwitterException
   *           if something goes wrong. There is a rare (but not rare enough)
   *           bug whereby Twitter occasionally returns a success code but the
   *           wrong tweet. If this happens, the update may or may not have
   *           worked - wait a bit & check.
   */
  public Status updateStatus(String statusText, long inReplyToStatusId)
      throws TwitterException {
    // should we trim statusText??
    if (statusText.length() > 160) {
      throw new IllegalArgumentException(
          "Status text must be 160 characters or less: " + statusText.length()
              + " " + statusText);
    }
    Map<String, String> vars = asMap("status", statusText);
    
    // add in long/lat if set
    if (myLatLong != null) {
      vars.put("lat", Double.toString(myLatLong[0]));
      vars.put("long", Double.toString(myLatLong[1]));
    }

    if (sourceApp != null)
      vars.put("source", sourceApp);
    if (inReplyToStatusId > 0) {
      vars.put("in_reply_to_status_id", inReplyToStatusId + "");
    }
    String result = post(TWITTER_URL + "/statuses/update.json", vars, true);
    try {
      Status s = new Status(new JSONObject(result), null);
      // sanity check
      String targetText = statusText.trim();
      String returnedStatusText = s.text.trim();
      if (returnedStatusText.equals(targetText))
        return s;
      // weird bug: Twitter occasionally rejects tweets?
      String st = statusText.toLowerCase();
      // is it a direct message? - which doesn't return the true status
      if (st.startsWith("dm") || st.startsWith("d")) {
        return null;
      }
      // try waiting and rechecking - maybe it did work after all
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      Status s2 = getStatus();
      if (s2 != null && targetText.equals(s2.text)) {
        // Log.report("Weird transitory bug in Twitter update status with "+targetText);
        return s2;
      }
      throw new TwitterException.Unexplained(
          "Unexplained failure for tweet: expected \"" + statusText
              + "\" but got " + s2);
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Does a user with the specified name or id exist?
   * 
   * @param screenName
   *          The screen name or user id of the suspected user.
   * @return False if the user doesn't exist or has been suspended, true
   *         otherwise.
   */
  public boolean userExists(String screenName) {
    try {
      show(screenName);
    } catch (TwitterException.E404 e) {
      return false;
    }
    return true;
  }

  /**
   * Set a language filter for search results. Note: This only applies to search
   * results.
   * 
   * @param language
   *          ISO code for language. Can be null for all languages.
   */
  public void setLanguage(String language) {
    lang = language;
  }

  /**
   * @return the latest trending topics on Twitter
   */
  public List<String> getTrends() {
    String jsonTrends = http.getPage("http://search.twitter.com/trends.json",
        null, false);
    try {
      JSONObject json1 = new JSONObject(jsonTrends);
      JSONArray json2 = json1.getJSONArray("trends");
      List<String> trends = new ArrayList<String>();
      for (int i = 0; i < json2.length(); i++) {
        JSONObject ti = json2.getJSONObject(i);
        String t = ti.getString("name");
        trends.add(t);
      }
      return trends;
    } catch (JSONException e) {
      throw new TwitterException(e);
    }
  }

  /**
   * Provides access to the {@link IHttpClient} which manages the low-level
   * authentication, posts and gets.
   */
  public IHttpClient getHttpClient() {
    return http;
  }

  /**
   * Provides support for fetching many pages. -1 indicates "give me as much as
   * Twitter will let me have."
   */
  public int getMaxResults() {
    return maxResults;
  }

  /**
   * @return retweets that you have made using "new-style" retweets rather than
   *         the RT microfromat. These are your tweets, i.e. they begin
   *         "RT @whoever: ". You can get the original tweet via
   *         {@link Status#getOriginal()}
   */
  public List<Status> getRetweetsByMe() {
    String url = TWITTER_URL + "/statuses/retweeted_by_me.json";
    Map<String, String> vars = addStandardishParameters(new HashMap<String, String>());
    String json = http.getPage(url, vars, true);
    return Status.getStatuses(json);
  }

}
