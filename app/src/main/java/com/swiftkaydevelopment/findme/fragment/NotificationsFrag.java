package com.swiftkaydevelopment.findme.fragment;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.activity.ProfileActivity;
import com.swiftkaydevelopment.findme.adapters.NotificationsAdapter;
import com.swiftkaydevelopment.findme.data.Notification;
import com.swiftkaydevelopment.findme.events.OnNotificationsRecieved;
import com.swiftkaydevelopment.findme.managers.NotificationManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Created by Kevin Haines on 8/24/15.
 */
public class NotificationsFrag extends BaseFragment implements NotificationsAdapter.NotifactionsAdapterListener {
    public static final String TAG = "NotificationsFrag";
    private static final String ARG_NOTES = "ARG_NOTES";

    private ArrayList<Notification> mNotifications = new ArrayList<>();
    private NotificationsAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;

    /**
     * Factory method to create a new instance of this fragment
     *
     * @param uid User's id
     * @return new instance of this fragment
     */
    public static NotificationsFrag newInstance(String uid) {
        NotificationsFrag frag = new NotificationsFrag();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        frag.setArguments(b);
        return  frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mNotifications = (ArrayList) savedInstanceState.getSerializable(ARG_NOTES);
            uid = savedInstanceState.getString(ARG_UID);
        } else {
            if (getArguments() != null) {
                uid = getArguments().getString(ARG_UID);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.notificationfrag, container,false);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerViewNotifications);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progressBar);

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mProgressBar.setVisibility(View.GONE);
            uid = savedInstanceState.getString(ARG_UID);
            mNotifications = (ArrayList) savedInstanceState.getSerializable(ARG_NOTES);
        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            NotificationManager.getInstance(getActivity()).getNotifications(uid, "0");
        }

        if (mAdapter == null) {
            mAdapter = new NotificationsAdapter(getActivity(), mNotifications);
        }

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_UID, uid);
        outState.putSerializable(ARG_NOTES, mNotifications);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        mAdapter.setListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.setListener(null);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(OnNotificationsRecieved event) {
        EventBus.getDefault().removeStickyEvent(event);
        mProgressBar.setVisibility(View.GONE);
        mAdapter.addNotifications(event.notifications);
    }

    @Override
    public void onNotificationClicked(Notification note) {
        if (note.type.equals(Notification.TYPE_LIKE) || note.type.equals(Notification.TYPE_COMMENT)) {
            SinglePostFragment singlePostFragment = SinglePostFragment.newInstance(uid, note.dataParentId);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, singlePostFragment, SinglePostFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        } else if (note.type.equals(Notification.TYPE_NEW_FRIEND) && note.user != null) {
            getActivity().startActivity(ProfileActivity.createIntent(getActivity(), note.user));
        }
    }

    @Override
    public void onLastItem(Notification item) {
        //todo: pagination notifications
    }
}
