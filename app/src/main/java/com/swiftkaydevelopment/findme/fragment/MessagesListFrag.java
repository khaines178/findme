package com.swiftkaydevelopment.findme.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.activity.MessagesActivity;
import com.swiftkaydevelopment.findme.activity.ProfileActivity;
import com.swiftkaydevelopment.findme.adapters.MessageThreadsAdapter;
import com.swiftkaydevelopment.findme.data.Message;
import com.swiftkaydevelopment.findme.data.ThreadInfo;
import com.swiftkaydevelopment.findme.data.User;
import com.swiftkaydevelopment.findme.events.MessageReceivedEvent;
import com.swiftkaydevelopment.findme.events.MessageSeenEvent;
import com.swiftkaydevelopment.findme.events.MessageSentEvent;
import com.swiftkaydevelopment.findme.managers.MessagesManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kevin Haines on 3/9/2015.
 */
public class MessagesListFrag extends BaseFragment implements MessagesManager.MessageThreadListener,
        MessageThreadsAdapter.ThreadSelectedListener, SwipeRefreshLayout.OnRefreshListener{

    /**
     * ISSUES:
     *      Pagination
     *      Order Threads by lastModified
     *      Persist to db
     *
     */
    public static final String      TAG = "MessagesListFrag";
    private static final String     ARG_THREAD_LIST = "ARG_THREAD_LIST";
    private static final String     ARG_REFRESHING = "ARG_REFRESHING";

    private List<ThreadInfo>        mThreadList = new ArrayList<>();
    private MessageThreadsAdapter   mMessagesAdapter;

    private boolean                 mRefreshing;
    private RecyclerView            mRecyclerView;
    private SwipeRefreshLayout      mRefreshLayout;
    private View                    mEmptyView;
    private ProgressBar             mProgressBar;

    /**
     * Factory method for getting a new instance of MessagelistFrag
     *
     * @param uid User's id (handled by super)
     * @return new instance of this fragment
     */
    public static MessagesListFrag getInstance(String uid) {
        MessagesListFrag frag = new MessagesListFrag();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mThreadList = (ArrayList) savedInstanceState.getSerializable(ARG_THREAD_LIST);
            uid = savedInstanceState.getString(ARG_UID);
        } else {
            if (getArguments() != null) {
                uid = getArguments().getString(ARG_UID);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.messageslistfrag, container, false);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerViewMessagesList);
        mRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.messageThreadsSwipeRefreshContainer);
        mEmptyView = layout.findViewById(R.id.messageThreadsEmptyView);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progressBar);

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            mProgressBar.setVisibility(View.GONE);
            mThreadList = (ArrayList) savedInstanceState.getSerializable(ARG_THREAD_LIST);
            mRefreshing = savedInstanceState.getBoolean(ARG_REFRESHING);
            if (mThreadList == null || mThreadList.isEmpty()) {
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            mEmptyView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshing = true;
            MessagesManager.getInstance(uid).refreshThreads();
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (mMessagesAdapter == null) {
            mMessagesAdapter = new MessageThreadsAdapter(getActivity(), mThreadList, uid);
            mMessagesAdapter.setThreadSelectedListener(this);
            mRecyclerView.setAdapter(mMessagesAdapter);
        }
        mRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(ARG_THREAD_LIST, (ArrayList) mThreadList);
        outState.putString(ARG_UID, uid);
        outState.putBoolean(ARG_REFRESHING, mRefreshing);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onThreadDeleted(ThreadInfo threadInfo) {
        mMessagesAdapter.removeThread(threadInfo);
        if (mThreadList.size() < 1) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onThreadsPurged() {
        mMessagesAdapter.clearMessages();
        if (mThreadList.size() < 1) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        MessagesManager.getInstance(uid).addThreadsListener(this);
        EventBus.getDefault().register(this);
        if (mMessagesAdapter != null) {
            mMessagesAdapter.setThreadSelectedListener(this);
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        MessagesManager.getInstance(uid).removeThreadsListener(this);
        EventBus.getDefault().unregister(this);
        if (mMessagesAdapter != null) {
            mMessagesAdapter.setThreadSelectedListener(null);
        }
    }

    @Override
    public void onRefresh() {
        mRefreshLayout.setRefreshing(true);
        mRefreshing = true;
        MessagesManager.getInstance(uid).refreshThreads();
    }

    @Override
    public void onRetrieveMoreThreads(ArrayList<ThreadInfo> threadInfos) {
        Log.i(TAG, "onRetriveMoreThreads");
        mProgressBar.setVisibility(View.GONE);
        if (mRefreshing) {
            mMessagesAdapter.removeAllThreads();
            mMessagesAdapter.addThreads(threadInfos);
            mRefreshLayout.setRefreshing(false);
            mRefreshing = false;
        } else {
            mMessagesAdapter.addThreads(threadInfos);
        }

        if (mMessagesAdapter.getItemCount() < 1) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMessageSentComplete(Message message) {
        mMessagesAdapter.addMessage(message);
    }

    @Override
    public void onMessageUnsent(Message message) {
        //todo: will complete this after we find a way
        //todo: to retrieve the last last message
    }

    @Override
    public void onThreadSelected(ThreadInfo threadInfo) {
        startActivity(MessagesActivity.createIntent(getActivity(), threadInfo.threadUser));
    }

    @Override
    public void onLastItem(ThreadInfo item) {
//        MessagesManager.getInstance(uid).getMoreThreads(item.);
    }

    @Override
    public void onThreadLongClicked(final ThreadInfo threadInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Thread");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MessagesManager.getInstance(uid).deleteThread(threadInfo);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onThreadUserSelected(User user) {
        getActivity().startActivity(ProfileActivity.createIntent(getActivity(), user));
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(MessageReceivedEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        mMessagesAdapter.addMessage(event.message);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(MessageSeenEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        mMessagesAdapter.markSeen(event.ouid);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(MessageSentEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        mMessagesAdapter.addMessage(event.message);
    }
}
