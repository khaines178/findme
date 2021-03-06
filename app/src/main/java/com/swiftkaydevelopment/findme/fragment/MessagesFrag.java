package com.swiftkaydevelopment.findme.fragment;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.activity.ProfileActivity;
import com.swiftkaydevelopment.findme.adapters.MessagesAdapter;
import com.swiftkaydevelopment.findme.data.Message;
import com.swiftkaydevelopment.findme.data.ThreadInfo;
import com.swiftkaydevelopment.findme.data.User;
import com.swiftkaydevelopment.findme.database.DatabaseManager;
import com.swiftkaydevelopment.findme.events.MessageReceivedEvent;
import com.swiftkaydevelopment.findme.events.MessageSeenEvent;
import com.swiftkaydevelopment.findme.managers.MessagesManager;
import com.swiftkaydevelopment.findme.managers.UserManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Kevin Haines on 3/11/2015.
 */
public class MessagesFrag extends BaseFragment implements View.OnClickListener, MessagesManager.MessagesListener, MessagesAdapter.MessagesAdapterListener, TextWatcher {
    public static final String  TAG = "MessagesFrag";
    private static final String ARG_USER = "ARG_USER";
    private static final String ARG_MESSAGES = "ARG_MESSAGES";

    public interface PictureMessageListener {
        void onStartImageSelection();
    }

    private ArrayList<Message>  mMessagesList = new ArrayList<>();
    private User user;
    private ThreadInfo          mThreadInfo;
    private MessagesAdapter     mMessageAdapter;

    private EditText            etmessage;
    private ImageView           ivsend;
    private ImageView           ivImage;
    private RecyclerView        mRecyclerView;
    private View                mEmptyView;
    private ProgressBar         mProgressBar;
    private AdView mAdView;

    public static MessagesFrag instance(String uid, User user) {
        MessagesFrag frag = new MessagesFrag();
        Bundle b = new Bundle();
        b.putSerializable(ARG_USER, user);
        b.putString(ARG_UID, uid);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(ARG_UID);
            user = (User) savedInstanceState.getSerializable(ARG_USER);
            mMessagesList = (ArrayList) savedInstanceState.getSerializable(ARG_MESSAGES);
        } else {
            if (getArguments() != null) {
                uid = getArguments().getString(ARG_UID);
                user = (User) getArguments().getSerializable(ARG_USER);
            }
        };
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.messageinline, container, false);

        etmessage = (EditText) layout.findViewById(R.id.etmessaget);
        ivsend = (ImageView) layout.findViewById(R.id.tvsendmessage);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.messagesInlineRecyclerView);
        mEmptyView = layout.findViewById(R.id.messageInlineEmptyView);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
        mAdView = (AdView) layout.findViewById(R.id.ad_view);

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);

        ivsend.setOnClickListener(this);
        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mMessagesList = (ArrayList) savedInstanceState.getSerializable(ARG_MESSAGES);
            mProgressBar.setVisibility(View.GONE);
            if (mMessagesList == null || mMessagesList.isEmpty()) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            mEmptyView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            if (mThreadInfo != null) {
                MessagesManager.getInstance(uid).getMoreMessages("0", mThreadInfo, getActivity());
            } else {
                mMessagesList = MessagesManager.getInstance(uid).getMoreMessagesSync("0", user, getActivity());
                Log.e(TAG, "messages size: " + mMessagesList.size());
            }
        }

        if (mMessageAdapter == null) {
            mMessageAdapter = new MessagesAdapter(getActivity(), mMessagesList, uid);
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mMessageAdapter);
        etmessage.addTextChangedListener(this);

        if (etmessage.getText().toString().isEmpty()) {
            ivsend.setImageResource(R.mipmap.ic_photo_camera_white_24dp);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_USER, user);
        outState.putString(ARG_UID, uid);
        outState.putSerializable(ARG_MESSAGES, mMessagesList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMessageAdapter != null) {
            mMessageAdapter.setMessagesAdapterListener(this);
        }
        MessagesManager.getInstance(uid).addMessagesListener(this);
        EventBus.getDefault().register(this);

        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMessageAdapter != null) {
            mMessageAdapter.setMessagesAdapterListener(null);
        }
        MessagesManager.getInstance(uid).removeMessagesListener(this);
        EventBus.getDefault().unregister(this);

        if (mAdView != null) {
            mAdView.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    /**
     * Called when message is sent
     *
     * @param message message that was sent
     */
    public void notifyNewMessage(Message message) {
        Log.i(TAG, "new message");
        Log.e(TAG, "message user id: " + message.getUser().getOuid());
        if (message.getUser().getOuid().equals(user.getOuid()) || message.getUser().getOuid().equals(uid)) {
            if (mMessageAdapter != null) {
                Log.i(TAG, "Message Adapter isn't null");
                mMessageAdapter.addMessage(message);
                if (!message.getUser().getOuid().equals(uid)) {
                    MessagesManager.getInstance(uid).markThreadAsSeen(uid, message.getThreadId());
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getActivity(), notification);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                int size = mRecyclerView.getLayoutManager().getItemCount() - 1;
                mRecyclerView.smoothScrollToPosition(size);
            }
        } else {
            //todo: should we show push notification or just make notification sound
        }

        if (mMessagesList.size() < 1) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public String getThreadId() {
        if (mMessageAdapter != null && mMessageAdapter.getMessages().size() > 0) {
            return mMessageAdapter.getMessages().get(0).getThreadId();
        }
        return "";
    }

    @Override
    public void onMessageLongClick(View itemView, final Message message) {
        PopupMenu popup = new PopupMenu(getActivity(), itemView);

        popup.getMenuInflater().inflate(R.menu.message_pop_up_menu, popup.getMenu());
        if (!message.getUser().getOuid().equals(uid)) {
            popup.getMenu().findItem(R.id.messagesPopUpMenuUnsend).setVisible(false);
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.messagePopUpMenuDelete) {
                    MessagesManager.getInstance(uid).deleteMessage(message);
                } else if (item.getItemId() == R.id.messagesPopUpMenuUnsend) {
                    MessagesManager.getInstance(uid).unSendMessage(message);
                }
                return true;
            }
        });
        popup.show();
    }

    @Override
    public void onMessageImageClickd(Message message) {
        FullImageFragment fullImageFragment = FullImageFragment.newInstance(uid, message.mMessageImageLocation, uid.equals(message.getSenderId()));
        getActivity().getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fullImageFragment, FullImageFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onProfileImageClicked(Message message) {
        getActivity().startActivity(ProfileActivity.createIntent(getActivity(), message.getUser()));
    }

    @Override
    public void onLastItem(Message item) {
        MessagesManager.getInstance(uid).getMoreMessages(item.getMessageId(), null, getActivity());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(MessageReceivedEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        if (event.message.getUser().getOuid().equals(user.getOuid())) {
            mMessageAdapter.addMessage(event.message);
            MessagesManager.getInstance(uid).markThreadAsSeen(uid, user.getOuid());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.toString().length() > 0) {
            ivsend.setImageResource(R.mipmap.ic_send_white_24dp);
        } else {
            ivsend.setImageResource(R.mipmap.ic_photo_camera_white_24dp);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onRetrieveMoreMessages(ArrayList<Message> messages) {
        mProgressBar.setVisibility(View.GONE);
        if (messages != null && !messages.isEmpty() && messages.get(0).getOuid().equals(user.getOuid())) {
            for (Message message : messages) {
                DatabaseManager.instance(getActivity()).createMessage(message);
            }
            mMessageAdapter.addAllMessages(messages);
            mMessageAdapter.setMessagesAdapterListener(this);
            int size = mRecyclerView.getLayoutManager().getItemCount() - 1;
            if (size > 0) {
                mRecyclerView.smoothScrollToPosition(size);
            }

            if (messages.size() < 1) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onMessageDeleted(Message message) {
        mMessageAdapter.removeMessage(message);
    }

    @Override
    public void onMessageSentComplete(Message message) {
        notifyNewMessage(message);
    }

    @Override
    public void onMessageUnsent(Message message) {
        mMessageAdapter.removeMessage(message);
    }

    public void onImageSelected(String filePath) {
        ivImage.setVisibility(View.VISIBLE);
        File file = new File(filePath);

        Message message = Message.instance();
        message.setDeletedStatus(0);
        message.setMessage("");
        message.mMessageImageLocation = filePath;
        message.setOuid(uid);
        message.setReadStatus(1);
        message.setSeenStatus(0);
        message.setUser(UserManager.getInstance(uid).me());
        message.setTime("Just Now");
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromInputMethod(etmessage.getWindowToken(), 0);
        etmessage.setText("");

        Toast.makeText(getActivity(), "Sending picture", Toast.LENGTH_SHORT).show();
        MessagesManager.getInstance(uid).sendPictureMessage(uid, message, user);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(MessageSeenEvent event) {
        if (event.ouid.equals(user.getOuid())) {
            mMessageAdapter.markAllAsSeen();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tvsendmessage) {

            if (!TextUtils.isEmpty(etmessage.getText().toString())) {
                Message message = Message.instance();
                message.setDeletedStatus(0);
                message.setMessage(etmessage.getText().toString());
                message.setOuid(uid);
                message.setReadStatus(1);
                message.setSeenStatus(0);
                message.setUser(UserManager.getInstance(uid).me());
                message.setTime("Just Now");
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromInputMethod(etmessage.getWindowToken(), 0);
                etmessage.setText("");

                MessagesManager.getInstance(uid).sendMessage(message, user);
            } else {
                if (getActivity() instanceof PictureMessageListener) {
                    ((PictureMessageListener) getActivity()).onStartImageSelection();
                }
            }
        }
    }
}
