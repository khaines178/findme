package com.swiftkaydevelopment.findme.newsfeed;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.activity.PrepareImageActivity;
import com.swiftkaydevelopment.findme.adapters.PostAdapter;
import com.swiftkaydevelopment.findme.data.Post;
import com.swiftkaydevelopment.findme.events.NewsFeedPostsRetrieved;
import com.swiftkaydevelopment.findme.fragment.BaseFragment;
import com.swiftkaydevelopment.findme.fragment.CommentsDialog;
import com.swiftkaydevelopment.findme.fragment.FullImageFragment;
import com.swiftkaydevelopment.findme.managers.PostManager;
import com.swiftkaydevelopment.findme.managers.UserManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Created by kevin haines on 2/8/2015.
 */
public class NewsFeedFrag extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,View.OnClickListener,
        PostManager.PostsListener, PostAdapter.PostAdapterListener {

    public static final String TAG = "NewsFeedFrag";
    public static final String ARG_POSTS_LIST = "ARG_POSTS_LIST";

    private View                fab;
    private View                fabstatus;
    private View                fabphoto;
    private RecyclerView        mRecyclerView;
    private SwipeRefreshLayout  swipeLayout;
    private ProgressBar         mProgressBar;
    private ProgressBar         mInitialPb;
    private boolean             loadingMore;
    private ArrayList<Post>     mPostsList = new ArrayList<>();

    private PostAdapter mPostAdapter;

    private AdView mAdView;

    public static NewsFeedFrag getInstance(String uid){
        NewsFeedFrag newsFeedFrag = new NewsFeedFrag();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        newsFeedFrag.setArguments(b);
        return newsFeedFrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(ARG_UID);
            mPostsList = (ArrayList<Post>) savedInstanceState.getSerializable(ARG_POSTS_LIST);
        } else {
            if (getArguments() != null) {
                uid = getArguments().getString(ARG_UID);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View layout = inflater.inflate(R.layout.newsfeedfrag, container, false);

        fab = layout.findViewById(R.id.fab);
        fabstatus = layout.findViewById(R.id.fabpencil);
        fabphoto = layout.findViewById(R.id.fabcamera);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.recyclerViewNewsFeed);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.newsfeedProgressBar);
        mProgressBar.setVisibility(View.GONE);

        mAdView = (AdView) layout.findViewById(R.id.ad_view);

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);


        mInitialPb = (ProgressBar) layout.findViewById(R.id.initialPb);

        swipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.black,
                android.R.color.holo_blue_bright,
                android.R.color.black);

        fab.setOnClickListener(this);
        fabstatus.setOnClickListener(this);
        fabphoto.setOnClickListener(this);

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mPostsList = (ArrayList) savedInstanceState.getSerializable(ARG_POSTS_LIST);
            mProgressBar.setVisibility(View.GONE);
            mInitialPb.setVisibility(View.GONE);
        } else {
            loadingMore = true;
            mInitialPb.setVisibility(View.VISIBLE);
            PostManager.getInstance().fetchPosts(uid, "0");
        }

        if (mPostAdapter == null) {
            mPostAdapter = new PostAdapter(getActivity(), mPostsList, UserManager.getInstance(uid).me(), false, false, uid);
            mRecyclerView.setAdapter(mPostAdapter);
            mPostAdapter.setPostAdapterListener(this);
        }

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        PostManager.getInstance().removeListener(this);
        if (mPostAdapter != null) {
            mPostAdapter.setPostAdapterListener(null);
        }
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

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        PostManager.getInstance().addPostListener(this);
        if (mPostAdapter != null) {
            mPostAdapter.setPostAdapterListener(this);
        }
        if (fab.getRotation() > 0) {
            rotate(fab);
        }
        fabstatus.setVisibility(View.INVISIBLE);
        fabphoto.setVisibility(View.INVISIBLE);
        EventBus.getDefault().register(this);

        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_UID, uid);
        outState.putSerializable(ARG_POSTS_LIST, mPostsList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRefresh() {
        Log.i(TAG, "onRefresh");
        swipeLayout.setRefreshing(true);
        PostManager.getInstance().refreshPosts(uid);
    }

    /**
     * Rotates the View either to 45 degrees or back to zero
     *
     * @param v View to rotate
     */
    private void rotate(View v) {
        AnimatorSet set = new AnimatorSet();
        float startRotation = v.getRotation();
        float endRotation = 0;
        if (startRotation == 0) {
            endRotation = 45;
        }
        set.play(ObjectAnimator.ofFloat(v, View.ROTATION, startRotation, endRotation));
        set.setDuration(200);
        set.start();
    }

    /**
     * Animates the fabstatus and fabphoto when the base fab
     * is clicked
     *
     */
    private void fabClickAnimation() {
        float startRotation = fab.getRotation();
        float endRotation = 0;
        if (startRotation == 0) {
            endRotation = 45;
        }

        AnimatorSet set = new AnimatorSet();
        ObjectAnimator outAnimFabStatus = ObjectAnimator.ofFloat(fabstatus, View.Y, fab.getTop(), fabstatus.getTop());
        ObjectAnimator outAnimFabPhoto = ObjectAnimator.ofFloat(fabphoto, View.X, fab.getLeft(), fabphoto.getLeft());
        ObjectAnimator inAnimFabStatus = ObjectAnimator.ofFloat(fabstatus, View.Y, fabphoto.getTop(), fab.getTop());
        ObjectAnimator inAnimFabPhoto = ObjectAnimator.ofFloat(fabphoto, View.X, fabphoto.getLeft(), fab.getLeft());
        set.play(outAnimFabStatus)
                .with(outAnimFabPhoto)
                .with(ObjectAnimator.ofFloat(fab, View.ROTATION, startRotation, endRotation));
        set.setDuration(300);
        set.start();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(NewsFeedPostsRetrieved event) {
        EventBus.getDefault().removeStickyEvent(event);
        ArrayList<Post> posts = event.posts;
        Log.i(TAG, "onPostsRetrieved size: " + posts.size() + " original size: " + mPostsList.size());

        if (swipeLayout.isRefreshing()) {
            mPostAdapter.clearAdapter();
            mPostAdapter.addPosts(posts);
            swipeLayout.setRefreshing(false);
        } else {
            if (mPostAdapter == null) {
                mPostAdapter = new PostAdapter(getActivity(), mPostsList, UserManager.getInstance(uid).me(), false, false, uid);
                mRecyclerView.setAdapter(mPostAdapter);
                mPostAdapter.setPostAdapterListener(this);
            } else {
                mPostAdapter.addPosts(posts);
            }
        }
        loadingMore = false;
        mProgressBar.setVisibility(View.GONE);
        mInitialPb.setVisibility(View.GONE);
    }

    @Override
    public void onCommentsClicked(Post post) {
//        if (getActivity().getSupportFragmentManager().findFragmentByTag(CommentsDialog.TAG) == null) {
            CommentsDialog dialog = CommentsDialog.newInstance(post.getPostId(), uid);
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fragment_transition_expand_from_bottom, R.anim.slide_out_to_bottom, R.anim.fragment_transition_expand_from_bottom, R.anim.slide_out_to_bottom)
                    .replace(android.R.id.content, dialog, CommentsDialog.TAG)
                    .addToBackStack(null)
                    .commit();

//            dialog.show(getActivity().getSupportFragmentManager(), CommentsDialog.TAG);

    }

    @Override
    public void onImageClicked(Post post) {
        FullImageFragment fullImageFragment = FullImageFragment.newInstance(uid, post, (post.getPostingUsersId().equals(uid)));
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fullImageFragment, FullImageFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onLastPost(Post post) {
        Log.d(TAG, "onLastPost");
        if (!loadingMore) {
            PostManager.getInstance().fetchPosts(uid, post.getPostId());
            loadingMore = true;
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProfilePictureClicked() {}

    @Override
    public void onPostLongClicked(final Post post, View itemView) {
        if (post.getPostingUsersId().equals(uid)) {
            PopupMenu popup = new PopupMenu(getActivity(), itemView);

            popup.getMenuInflater().inflate(R.menu.postpopupmenu, popup.getMenu());

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.postMenuDelete) {
                        PostManager.getInstance().deletePost(post);
                        mPostAdapter.removePost(post);
                    }
                    return true;
                }
            });
            popup.show();
        }
    }

    @Override
    public void onProfilePostsRetrieved(ArrayList<Post> posts) {}

    @Override
    public void onSinglePostRetrieved(Post post) {}

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fabcamera) {
            startActivity(PrepareImageActivity.createIntent(getActivity()));
        } else if (v.getId() == R.id.fabpencil) {
            Intent i = new Intent("com.swiftkaytech.findme.UPDATESTATUS");
            startActivity(i);
        } else if (v.getId() == R.id.fab) {
            if (fabstatus.getVisibility() == View.INVISIBLE) {
                fabstatus.setVisibility(View.VISIBLE);
                fabphoto.setVisibility(View.VISIBLE);
                fabClickAnimation();
            } else {
                fabstatus.setVisibility(View.INVISIBLE);
                fabphoto.setVisibility(View.INVISIBLE);
                rotate(v);
            }
        }
    }
}