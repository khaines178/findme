/*
 *      Copyright (C) 2015 Kevin Haines
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.swiftkaydevelopment.findme.managers;

import android.content.Context;
import android.os.AsyncTask;

import com.swiftkaydevelopment.findme.data.Message;
import com.swiftkaydevelopment.findme.data.SimpleUser;
import com.swiftkaydevelopment.findme.data.ThreadInfo;
import com.swiftkaydevelopment.findme.data.User;
import com.swiftkaydevelopment.findme.database.DatabaseManager;
import com.swiftkaydevelopment.findme.events.MessageReceivedEvent;
import com.swiftkaydevelopment.findme.events.MessageSentEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessagesManager {
    public static final String TAG = "MessagesManager";

    public interface MessagesListener{
        /**
         * Called when messages or more messages have been retrieved
         * from the server
         *
         * @param moreMessages list of messages retrieved
         */
        void onRetrieveMoreMessages(ArrayList<Message> moreMessages);
        void onMessageDeleted(Message message);
        void onMessageSentComplete(Message message);
        void onMessageUnsent(Message message);
    }

    public interface MessageThreadListener{
        void onThreadDeleted(ThreadInfo threadInfo);
        void onRetrieveMoreThreads(ArrayList<ThreadInfo> threadInfos);
        void onMessageSentComplete(Message message);
        void onMessageUnsent(Message message);
        void onThreadsPurged();
    }

    private static String mUid;
    private static MessagesManager manager = null;
    private ArrayList<Message> mMessages;

    private static CopyOnWriteArrayList<MessageThreadListener> mMessageThreadListeners = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<MessagesListener> mMessagesListeners = new CopyOnWriteArrayList<>();

    public static MessagesManager getInstance(String uid){
        if (manager == null) {
            manager = new MessagesManager();
        }
        manager.mUid = uid;
        return manager;
    }

    public void addThreadsListener(MessageThreadListener listener) {
        mMessageThreadListeners.add(listener);
    }

    public void removeThreadsListener(MessageThreadListener listener) {
        mMessageThreadListeners.remove(listener);
    }

    public void addMessagesListener(MessagesListener listener) {
        mMessagesListeners.add(listener);
    }

    public void removeMessagesListener(MessagesListener listener) {
        mMessagesListeners.remove(listener);
    }

    public void refreshMessages(ThreadInfo threadInfo){
        new FetchMessagesTask("0", threadInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void refreshThreads(){
        new FetchThreadsTask("0").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void getMoreMessages(String lastMessage, ThreadInfo threadInfo, Context context){
        new FetchMessagesTask(lastMessage, threadInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    /**
     * Notifies the server that you saw a message as it came in under a push notification
     *
     * @param message Message that was seen
     */
    public void notifyMessageSeen(Message message) {
        new NotifyMessageSeenTask(message).execute();
    }

    /**
     * First gets the messages for the conversation from the database then
     * kicks off an AsyncTask to fetch messages from the server in case we don't have
     * them.
     *
     * Scheme needs to work as follows:
     *
     * 1. return any existing messages we currently have available in the db
     * 2. We need to get the firstKnowMessage from that list
     * 3. Ping the server to see if we have any more recent messages than that.
     * 4. If we do post a sticky event for that and add them to the db
     *
     * Pagination will need to be performed in a different method
     *
     * @param lastMessage the last message the user has
     * @param user The other user in the conversation
     * @param context Calling Context
     * @return ArrayList of messages if we have any in the db
     */
    public ArrayList<Message> getMoreMessagesSync(String lastMessage, User user, Context context){
        ArrayList<Message> messages = DatabaseManager.instance(context).getMessages(mUid, user.getOuid());
        new FetchMessagesTask(lastMessage, user).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        return messages;
    }

    public void deleteAllThreads(String uid) {
        new DeleteAllThreadsTask(uid).execute();
    }

    public void getMoreThreads(String lastThread){
        new FetchThreadsTask(lastThread).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void sendMessage(Message message, User user){
        new SendMessageTask(message, user).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void deleteMessage(Message message){
        new DeleteMessageTask(message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void deleteThread(ThreadInfo threadInfo){
        new DeleteThreadTask(threadInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void unSendMessage(Message message){
        new UnSendMessageTask(message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void editMessage(Message message){
        new EditMessageTask(message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void reportMessage(Message message, String reason){
        new ReportMessageTask(message, reason).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void markThreadAsRead(ThreadInfo threadInfo){
        new MarkThreadAsReadTask(threadInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void markThreadAsDeleted(ThreadInfo threadInfo){
        new MarkThreadAsDeletedTask(threadInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    public void sendPictureMessage(String uid, Message message, User user) {
        new SendPictureMessageTask(message, uid, user).execute();
    }


    public void markThreadAsSeen(String uid, String ouid) {
        new MarkThreadAsSeenTask(uid, ouid).execute();
    }

    public void messageNotificationReceived(Message msg) {
        EventBus.getDefault().postSticky(new MessageReceivedEvent(msg));
    }

    private class NotifyMessageSeenTask extends AsyncTask<Void, Void, Void> {
        private Message message;

        public NotifyMessageSeenTask(Message message) {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("id", message.getMessageId());
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class FetchMessagesTask extends AsyncTask<Void, Void, ArrayList<Message>>{
        String lastMessage;
        ThreadInfo threadInfo;
        User user;

        public FetchMessagesTask(String lastMessage, ThreadInfo threadInfo) {
            this.lastMessage = lastMessage;
            this.threadInfo = threadInfo;
        }

        public FetchMessagesTask(String lastMessage, User user) {
            this.lastMessage = lastMessage;
            this.user = user;
        }

        @Override
        protected ArrayList<Message> doInBackground(Void... params) {
            ArrayList<Message> mList = new ArrayList<>();

            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("lastmessage", lastMessage);
            if (threadInfo != null) {
                connectionManager.addParam("ouid", threadInfo.ouid);
            } else {
                connectionManager.addParam("ouid", user.getOuid());
            }
            connectionManager.setUri("getmessagesv_1_6_1.php");

            try {
                JSONObject jsonObject = new JSONObject(connectionManager.sendHttpRequest());
                JSONArray jsonArray = jsonObject.getJSONArray("messages");

                int length = jsonArray.length();
                for(int i = 0; i < length; i++) {
                    JSONObject child = jsonArray.getJSONObject(i);
                    Message m = Message.createMessageFromJson(child);
                    mList.add(m);
                }
            } catch(JSONException e) {
                e.printStackTrace();
            }
            return mList;
        }

        @Override
        protected void onPostExecute(ArrayList<Message> messages) {
            super.onPostExecute(messages);

            for (MessagesListener l : mMessagesListeners) {
                l.onRetrieveMoreMessages(messages);
            }
        }
    }

    /**
     * Class to fetch the list of threads for the current user
     *
     */
    private class FetchThreadsTask extends AsyncTask<Void, Void, ArrayList<ThreadInfo>>{
        String lastThread;

        public FetchThreadsTask(String lastThread) {
            this.lastThread = lastThread;
        }

        @Override
        protected ArrayList<ThreadInfo> doInBackground(Void... params) {
            ArrayList<ThreadInfo> tList = new ArrayList<>();

            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("lastthread", lastThread);
            connectionManager.setUri("getthreadsv_1_6_1.php");

            final String result = connectionManager.sendHttpRequest();

            if (result != null) {

                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonArray = jsonObject.getJSONArray("threads");
                    int length = jsonArray.length();
                    for (int i = 0; i < length; i++) {
                        JSONObject child = jsonArray.getJSONObject(i);
                        ThreadInfo t = ThreadInfo.instance(mUid);
                        t.ouid = child.getString("ouid");
                        t.lastMessage = child.getString("message");
                        t.senderId = child.getString("senderid");
                        if (child.getString("readstat").equals("read")) {
                            t.readStatus = 1;
                        } else {
                            t.readStatus = 0;
                        }
                        if (child.getString("seenstat").equals("seen")) {
                            t.seenStatus = 1;
                        } else {
                            t.seenStatus = 0;
                        }
                        t.threadUser = SimpleUser.createUserFromJson(child.getJSONObject("user"));
                        t.time = child.getString("time");
                        tList.add(t);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return tList;
        }

        @Override
        protected void onPostExecute(ArrayList<ThreadInfo> threadInfos) {
            super.onPostExecute(threadInfos);

            for (MessageThreadListener l : mMessageThreadListeners) {
                if (l != null) {
                    l.onRetrieveMoreThreads(threadInfos);
                }
            }
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, String>{
        Message message;
        User user;

        public SendMessageTask(Message message, User user) {
            this.message = message;
            this.user = user;
        }

        @Override
        protected String doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("ouid", user.getOuid());
            connectionManager.addParam("message", message.getMessage());
            connectionManager.setUri("sendmessagev_1_6_1.php");
            return connectionManager.sendHttpRequest();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Message m = Message.instance();
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject child = jsonObject.getJSONObject("lastmessage");

                m.setSeenStatus(0);
                m.setReadStatus(1);
                m.setOuid(child.getString("ouid"));
                m.setDeletedStatus(0);
                m.setMessage(child.getString("message"));
                m.setMessageId("id");
                m.setTime("Just now");
                m.setUser(UserManager.getInstance(mUid).me());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (MessagesListener l : mMessagesListeners) {
                l.onMessageSentComplete(m);
            }

            for (MessageThreadListener l : mMessageThreadListeners) {
                l.onMessageSentComplete(m);
            }

            EventBus.getDefault().postSticky(new MessageSentEvent(m));
        }
    }

    private class DeleteMessageTask extends AsyncTask<Void, Void, Void>{
        Message message;

        public DeleteMessageTask(Message message) {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("messageid", message.getMessageId());
            connectionManager.setUri("deletemessage.php");
            connectionManager.sendHttpRequest();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            for (MessagesListener l : mMessagesListeners) {
                if (l != null) {
                    l.onMessageDeleted(message);
                }
            }
        }
    }

    private class DeleteThreadTask extends AsyncTask<Void, Void, Void>{
        ThreadInfo threadInfo;

        public DeleteThreadTask(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("ouid", threadInfo.ouid);
            connectionManager.setUri("deletethread.php");
            connectionManager.sendHttpRequest();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            for (MessageThreadListener l : mMessageThreadListeners) {
                l.onThreadDeleted(threadInfo);
            }
        }
    }

    private class UnSendMessageTask extends AsyncTask<Void, Void, Void>{
        Message message;

        public UnSendMessageTask(Message message) {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("tag", message.getTag());
            connectionManager.setUri("unsendmessage.php");
            connectionManager.sendHttpRequest();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            for (MessagesListener l : mMessagesListeners) {
                l.onMessageUnsent(message);
            }

            for (MessageThreadListener l : mMessageThreadListeners) {
                l.onMessageUnsent(message);
            }
        }
    }

    private class DeleteAllThreadsTask extends AsyncTask<Void, Void, Void> {
        String uid;

        public DeleteAllThreadsTask(String uid) {
            this.uid = uid;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.setUri("deleteallthreads.php");
            connectionManager.sendHttpRequest();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            for (MessageThreadListener l : mMessageThreadListeners) {
                if (l != null) {
                    l.onThreadsPurged();
                }
            }
        }
    }

    private class EditMessageTask extends AsyncTask<Void, Void, Void>{
        Message message;

        public EditMessageTask(Message message) {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("message", message.getMessage());
            connectionManager.addParam("messageid", message.getMessageId());
            connectionManager.setUri("editmessage.php");
            connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class ReportMessageTask extends AsyncTask<Void, Void, Void>{
        Message message;
        String reportReason;

        public ReportMessageTask(Message message, String reportReason) {
            this.message = message;
            this.reportReason = reportReason;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("messageid", message.getMessageId());
            connectionManager.addParam("reportreason", reportReason);
            connectionManager.setUri("reportmessage.php");
            connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class MarkThreadAsReadTask extends AsyncTask<Void, Void, Void>{
        ThreadInfo threadInfo;

        public MarkThreadAsReadTask(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("ouid", threadInfo.ouid);
            connectionManager.setUri("markthreadasread.php");
            connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class MarkThreadAsDeletedTask extends AsyncTask<Void, Void, Void>{
        ThreadInfo threadInfo;

        public MarkThreadAsDeletedTask(ThreadInfo threadInfo) {
            this.threadInfo = threadInfo;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            connectionManager.addParam("ouid", threadInfo.ouid);
            connectionManager.setUri("markthreadasdeleted.php");
            connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class MarkThreadAsSeenTask extends AsyncTask<Void, Void, Void> {
        //todo: the server is expect uid ouid not threadid to come up
        //thread id would just mark the user's own thread as read
        //not the other user's (which is the only one we care about lol
        String uid;
        String ouid;

        public MarkThreadAsSeenTask(String uid, String ouid) {
            this.uid = uid;
            this.ouid = ouid;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", uid);
            connectionManager.addParam("ouid", ouid);
            connectionManager.setUri("seenmessage.php");
            connectionManager.sendHttpRequest();
            return null;
        }
    }

    private class SendPictureMessageTask extends AsyncTask<Void, Void, String> {
        Message message;
        String uid;
        User user;

        public SendPictureMessageTask(Message message, String uid, User user) {
            this.message = message;
            this.uid = uid;
            this.user = user;
        }

        @Override
        protected String doInBackground(Void... params) {
            ConnectionManager connectionManager = new ConnectionManager();
            connectionManager.setMethod(ConnectionManager.POST);
            connectionManager.addParam("uid", mUid);
            try {
                URL url = new URL("http://www.swiftkay.com/findme/sendpicturemessage.php");
                String result = connectionManager.uploadFile(url, message.mMessageImageLocation, uid, user.getOuid());
                return result;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Message m = Message.instance();
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject child = jsonObject.getJSONObject("lastmessage");

                m.setSeenStatus(0);
                m.setReadStatus(1);
                m.setOuid(child.getString("ouid"));
                m.setDeletedStatus(0);
                m.setMessage(child.getString("message"));
                m.setMessageId("id");
                m.mMessageImageLocation = child.getString("messageimageloc");
                m.setTime("Just now");
                m.setUser(UserManager.getInstance(mUid).me());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (MessagesListener l : mMessagesListeners) {
                l.onMessageSentComplete(m);
            }

            for (MessageThreadListener l : mMessageThreadListeners) {
                l.onMessageSentComplete(m);
            }
        }
    }
}
