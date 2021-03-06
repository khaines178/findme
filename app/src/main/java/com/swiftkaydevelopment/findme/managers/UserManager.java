package com.swiftkaydevelopment.findme.managers;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.swiftkaydevelopment.findme.data.SimpleUser;
import com.swiftkaydevelopment.findme.data.User;
import com.swiftkaydevelopment.findme.database.DatabaseManager;
import com.swiftkaydevelopment.findme.events.ConnectionsFoundEvent;
import com.swiftkaydevelopment.findme.events.OnFriendRequestRetrievedEvent;
import com.swiftkaydevelopment.findme.events.OnFriendsRetrievedEvent;
import com.swiftkaydevelopment.findme.events.OnGetUserDetailEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Kevin Haines on 10/21/15.
 */
public class UserManager {
    public interface UserManagerListener{
        void onMatchesRetrieved(ArrayList<User> users);
        void onProfileViewsFetched(ArrayList<User> users);
    }
    private static final String TAG = "UserManager";
    private String mUid;
    private static User me;

    private CopyOnWriteArrayList<UserManagerListener> mListeners = new CopyOnWriteArrayList<>();

    private static UserManager manager = null;


    public static UserManager getInstance(String uid){
        synchronized (UserManager.class) {
            if (manager == null) {
                manager = new UserManager();
            }
            manager.mUid = uid;
        }

        return manager;
    }

    public void addListener(UserManagerListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(UserManagerListener listener) {
        mListeners.remove(listener);
    }

    public User me() {
        if (me == null) {
            try {
                me = new FetchUserTask(mUid, User.createUser()).execute().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return me;
    }

    /**
     * Gets the list of profile views for the user
     *
     * @param uid User's id
     * @param lastpage lastpage of views fetched
     */
    public void getProfileViews(String uid, String lastpage) {
        new GetProfileViewsTask(uid, lastpage).execute();
    }

    public void invalidateMe() {
        me = null;
    }

    /**
     * Retrieves a list of Friend Requests
     * @param uid current users id
     */
    public void getFriendRequests(String uid) {
        new GetFriendsRequestsTask(uid).execute();
    }

    /**
     * Sends a friend request to the other user
     * @param uid String user's id
     * @param otherUser User to send friend request to
     */
    public void sendFriendRequest(String uid, User otherUser) {
        new SendFriendRequestTask(uid, otherUser).execute();
    }

    public void addProfileView(String uid, User user) {
        new AddProfileViewTask(uid, user).execute();
    }

    /**
     * Sends request to get list of friends
     * @param uid Current User's id
     */
    public void getFriends(String uid) {
        new GetFriendsTask(uid).execute();
    }

    /**
     * Sends a request to match with the other user
     * @param uid String current User's id
     * @param otherUser User to match with
     */
    public void sendMatchRequest(String uid, User otherUser) {
        new SendMatchRequestTask(uid, otherUser).execute();
    }

    /**
     * Gets a list of the current User's matches
     * @param uid String Current User's id
     */
    public void getMatches(String uid) {
        new GetMatchesTask(uid).execute();
    }

    /**
     * Sends a request to block a User
     * @param uid Current User's id
     * @param otherUser User to block
     */
    public void blockUser(String uid, User otherUser) {
        new BlockUserTask(uid, otherUser).execute();
    }

    /**
     * Sends request to server to unfriend the specified user
     * @param uid String current User's id
     * @param otherUser User to unfriend
     */
    public void unfriend(String uid, User otherUser) {
        new UnfriendTask(uid, otherUser).execute();
    }

    /**
     * First gets the stored list of users from the db then goes
     * and fetches more from the server.
     *
     * @param uid User's account id
     * @param lastpost last post
     * @param context Current context
     * @return List of users
     */
    public ArrayList<User> findPeopleSync(String uid, String lastpost, String zip, Context context) {
//        ArrayList<User> users = DatabaseManager.instance(context).getUsers();
//        if (!users.isEmpty()) {
//            lastpost = "0";
//        }
        new FindPeopleTask(uid, lastpost, zip).execute();
        return new ArrayList<>();
    }

    public void getUserDetail(String uid, User user) {
        new FetchUserTask(uid, user).execute();
    }

    /**
     * Pulls the list of users from the database
     *
     * @param uid User's id
     * @param lastpost lastpost
     * @return List of users
     */
    public void findMorePeople(String uid, String lastpost, String zip) {
        new FindPeopleTask(uid, lastpost, zip).execute();
    }

    public void clearUsers(Context context) {
        DatabaseManager.instance(context).clearUsers();
    }

    public void updateProfile(User user, String uid) {
        new UpdateProfileTask(user, uid).execute();
    }

    /**
     * Sends a request to the server to deny the friend request
     * @param uid String current User's id
     * @param otherUser User whose friend request is getting denied
     */
    public void denyFriendRequest(String uid, User otherUser) {
        new DenyFriendRequestTask(uid, otherUser).execute();
    }

    private class UpdateProfileTask extends AsyncTask<Void, Void, Void> {
        User user;
        String uid;

        public UpdateProfileTask(User user, String uid) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.setUri("updateprofile.php");
            connectionManager.addParam("uid", uid);
            connectionManager.addParam("aboutme", user.getAboutMe());
            connectionManager.addParam("orientation", user.getOrientation().toString());
            connectionManager.addParam("status", user.mRelationshipStatus);
            connectionManager.addParam("haskids", user.hasKids);
            connectionManager.addParam("wantskids", user.wantsKids);
            connectionManager.addParam("weed", user.weed);
            connectionManager.addParam("profession", user.profession);
            connectionManager.addParam("school", user.school);
            connectionManager.addParam("looking_for", Integer.toString(user.mLookingFor));
            connectionManager.sendHttpRequest();

            return null;
        }
    }

    /**
     * Class to fetch all of the users details
     *
     */
    private class FetchUserTask extends AsyncTask<Void,Void,User> implements Serializable {
        String uid;
        User user;

        public FetchUserTask(String uid, User user){
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected User doInBackground(Void... params) {
            Log.i(TAG, "fetchUser-doInBackground");
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.setUri("getuser.php");
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("ouid", uid);
            final String result = connectionManager.sendHttpRequest();

            if (result != null) {

                try {
                    JSONObject jsonObject = new JSONObject(result);
                    user  = User.createUserFromJson(jsonObject);

                } catch (final JSONException e) {
                    e.printStackTrace();
                }
            }
            return user;
        }

        @Override
        protected void onPostExecute(User user) {
            super.onPostExecute(user);
            EventBus.getDefault().postSticky(new OnGetUserDetailEvent(user));
        }
    }

    private class GetProfileViewsTask extends AsyncTask<Void, Void, ArrayList<User>> {
        String uid;
        String lastpage;

        public GetProfileViewsTask(String uid, String lastpage) {
            this.uid = uid;
            this.lastpage = lastpage;
        }

        @Override
        protected ArrayList<User> doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.addParam("lastpage", lastpage);
            connectionManager.setUri("getprofileviewsv_1_6_1.php");
            String result = connectionManager.sendHttpRequest();
            ArrayList<User> users = new ArrayList<>();

            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonArray = jsonObject.getJSONArray("users");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject child = jsonArray.getJSONObject(i);
                        User u = SimpleUser.createUserFromJson(child);
                        users.add(u);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return users;
        }

        @Override
        protected void onPostExecute(ArrayList<User> users) {
            super.onPostExecute(users);
            for (UserManagerListener l : mListeners) {
                if (l != null) {
                    l.onProfileViewsFetched(users);
                }
            }
        }
    }

    private  class AddProfileViewTask extends AsyncTask<Void, Void, Void> {
        String uid;
        User user;

        public AddProfileViewTask(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.addParam("ouid", user.getOuid());
            connectionManager.setUri("addprofileview.php");
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class GetFriendsRequestsTask extends AsyncTask<Void, Void, ArrayList<User>> {
        String uid;

        public GetFriendsRequestsTask(String uid) {
            this.uid = uid;
        }

        @Override
        protected ArrayList<User> doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("getfriendrequestsv_1_6_1.php");
            String result = connectionManager.sendHttpRequest();
            ArrayList<User> users = new ArrayList<>();

            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonArray = jsonObject.getJSONArray("ppl");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject child = jsonArray.getJSONObject(i);
                        User u = SimpleUser.createUserFromJson(child.getJSONObject("user"));
                        users.add(u);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return users;
        }

        @Override
        protected void onPostExecute(ArrayList<User> users) {
            super.onPostExecute(users);
            EventBus.getDefault().postSticky(new OnFriendRequestRetrievedEvent(users));
        }
    }

    private class SendFriendRequestTask extends AsyncTask<Void, Void, Void> {
        String uid;
        User user;

        public SendFriendRequestTask(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("sendfriendrequest.php");
            connectionManager.addParam("ouid", user.getOuid());
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class GetFriendsTask extends AsyncTask<Void, Void, ArrayList<User>> {
        String uid;

        public GetFriendsTask(String uid) {
            this.uid = uid;
        }

        @Override
        protected ArrayList<User> doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("getfriendsv_1_6_1.php");
            String result = connectionManager.sendHttpRequest();
            ArrayList<User> users = new ArrayList<>();

            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonArray = jsonObject.getJSONArray("ppl");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject child = jsonArray.getJSONObject(i);
                        User u = SimpleUser.createUserFromJson(child.getJSONObject("user"));
                        users.add(u);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return users;
        }

        @Override
        protected void onPostExecute(ArrayList<User> users) {
            super.onPostExecute(users);
            EventBus.getDefault().postSticky(new OnFriendsRetrievedEvent(users));
        }
    }

    private class SendMatchRequestTask extends AsyncTask<Void, Void, Void> {
        String uid;
        User user;

        public SendMatchRequestTask(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("sendmatchrequest.php");
            connectionManager.addParam("ouid", user.getOuid());
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class GetMatchesTask extends AsyncTask<Void, Void, Void> {
        String uid;

        public GetMatchesTask(String uid) {
            this.uid = uid;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("getmatches.php");
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class BlockUserTask extends AsyncTask<Void, Void, Void> {
        String uid;
        User user;

        public BlockUserTask(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("blockuser.php");
            connectionManager.addParam("ouid", user.getOuid());
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class DenyFriendRequestTask extends AsyncTask<Void, Void, Void>{
        String uid;
        User user;

        public DenyFriendRequestTask(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("denyfriendrequest.php");
            connectionManager.addParam("ouid", user.getOuid());
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class UnfriendTask extends AsyncTask<Void, Void, Void> {
        String uid;
        User user;

        public UnfriendTask(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("unfriend.php");
            connectionManager.addParam("ouid", user.getOuid());
            String result = connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class FindPeopleTask extends AsyncTask<Void, Void, ArrayList<User>> {
        String uid;
        String lastpost;
        String zip;

        public FindPeopleTask(String uid, String lastpost, String zip) {
            this.uid = uid;
            this.lastpost = lastpost;
            this.zip = zip;
        }

        @Override
        protected ArrayList<User> doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.addParam("lastpost", lastpost);
            connectionManager.addParam("zip", zip);
            connectionManager.setUri("findconnections.php");
            String result = connectionManager.sendHttpRequest();
            ArrayList<User> users = new ArrayList<>();

            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonArray = jsonObject.getJSONArray("ppl");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject child = jsonArray.getJSONObject(i);
                        User u = SimpleUser.createUserFromJson(child);
                        if (u != null) {
                            users.add(u);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return users;
        }

        @Override
        protected void onPostExecute(ArrayList<User> users) {
            super.onPostExecute(users);
            EventBus.getDefault().postSticky(new ConnectionsFoundEvent(users));
        }
    }
}
