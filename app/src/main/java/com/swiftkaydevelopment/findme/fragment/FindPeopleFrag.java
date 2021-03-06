package com.swiftkaydevelopment.findme.fragment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.activity.ProfileActivity;
import com.swiftkaydevelopment.findme.adapters.FindPeopleAdapter;
import com.swiftkaydevelopment.findme.data.User;
import com.swiftkaydevelopment.findme.database.DatabaseManager;
import com.swiftkaydevelopment.findme.events.ConnectionsFoundEvent;
import com.swiftkaydevelopment.findme.managers.PrefManager;
import com.swiftkaydevelopment.findme.managers.UserManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Created by Kevin Haines on 2/25/2015.
 */
public class FindPeopleFrag extends BaseFragment implements UserManager.UserManagerListener, FindPeopleAdapter.ConnectAdapterListener, SwipeRefreshLayout.OnRefreshListener{
    public static final String TAG = "FindPeopleFrag";
    private static final String ARG_USERS = "ARG_USERS";

    public static final String UID_ARGS = "UID_ARGS";

    private ArrayList<User> users = new ArrayList<>();
    private FindPeopleAdapter mAdapter;
    private boolean mLoadingMore = false;

    private RecyclerView mRecyclerView;
    private ProgressBar mLoadingMorePb;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mRefreshLayout;

    public static FindPeopleFrag newInstance(String id){
        FindPeopleFrag frag = new FindPeopleFrag();
        Bundle b = new Bundle();
        b.putString(UID_ARGS,id);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(UID_ARGS);
            users = (ArrayList) savedInstanceState.getSerializable(ARG_USERS);
        } else {
            if(getArguments() != null){
                uid = getArguments().getString(UID_ARGS);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.findpeople, container, false);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerView);
        mLoadingMorePb = (ProgressBar) layout.findViewById(R.id.loadingMorePb);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progressBar);
        mRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.refreshLayout);

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingMorePb.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            mProgressBar.setVisibility(View.GONE);
            users = (ArrayList) savedInstanceState.getSerializable(ARG_USERS);
        } else {
            users = UserManager.getInstance(uid).findPeopleSync(uid, "0", PrefManager.getZip(getActivity()), getActivity());
            if (users.isEmpty()) {
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.setVisibility(View.GONE);
            }
        }

        if (mAdapter == null) {
            mAdapter = new FindPeopleAdapter(getActivity(), users, uid);
        }

        mAdapter.setListener(this);

        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(
                getActivity().getResources().getInteger(R.integer.grid_layout_columns_connect),
                StaggeredGridLayoutManager.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(UID_ARGS, uid);
        outState.putSerializable(ARG_USERS, users);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        UserManager.getInstance(uid).addListener(this);
        if (mAdapter != null) {
            mAdapter.setListener(this);
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        UserManager.getInstance(uid).removeListener(this);
        mAdapter.setListener(null);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onRefresh() {
        UserManager.getInstance(uid).clearUsers(getActivity());
        UserManager.getInstance(uid).findPeopleSync(uid, "0", PrefManager.getZip(getActivity()), getActivity());
    }

    @Override
    public void onMatchesRetrieved(ArrayList<User> users) {}

    @Override
    public void onProfileViewsFetched(ArrayList<User> users) {}

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(ConnectionsFoundEvent event) {
        EventBus.getDefault().removeStickyEvent(event);

        for (User user : event.users) {
            DatabaseManager.instance(getActivity()).createUser(user);
        }

        mLoadingMore = false;
        mLoadingMorePb.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);
        mRefreshLayout.setRefreshing(false);
        mAdapter.addUsers(event.users);
    }

    @Override
    public void onLastItem(User lastUser) {
        if (!mLoadingMore) {
            mLoadingMore = true;
            mLoadingMorePb.setVisibility(View.VISIBLE);
            UserManager.getInstance(uid).findMorePeople(uid, lastUser.getOuid(), PrefManager.getZip(getActivity()));
        }
    }

    @Override
    public void onUserSelected(User user) {
        getActivity().startActivity(ProfileActivity.createIntent(getActivity(), user));
    }
}
