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

package com.swiftkaydevelopment.findme.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.activity.ProfileActivity;
import com.swiftkaydevelopment.findme.adapters.CommentAdapter;
import com.swiftkaydevelopment.findme.data.Comment;
import com.swiftkaydevelopment.findme.data.Post;
import com.swiftkaydevelopment.findme.managers.CommentsManager;
import com.swiftkaydevelopment.findme.managers.PostManager;
import com.swiftkaydevelopment.findme.managers.UserManager;
import com.swiftkaydevelopment.findme.utils.ImageLoader;
import com.swiftkaydevelopment.findme.views.ExpandableLinearLayout;
import com.swiftkaydevelopment.findme.views.tagview.TagView;

import java.util.ArrayList;

public class SinglePostFragment extends BaseFragment {
    public static final String TAG = "SinglePostFragment";

    private static final String ARG_COMMENTS = "ARG_COMMENTS";
    private static final String ARG_POST = "ARG_POST";

    private Post mPost;

    TextView tvName, tvTime, tvLocation, tvPost, tvNumLikes, tvNumComments;
    ImageView ivProfilePicture, ivPostImage, ivPostLike, ivPostToggle;
    ExpandableLinearLayout extrasContainer;
    TagView tagView;

    private ArrayList<Comment> mComments;
    private ImageView ivPostComment;
    private ImageView emptyView;
    private EditText etComment;
    private ListView lv;

    private CommentAdapter mAdapter;

    public static SinglePostFragment newInstance (String uid, Post post) {
        SinglePostFragment frag = new SinglePostFragment();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        b.putSerializable(ARG_POST, post);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(ARG_UID);
            mPost = (Post) savedInstanceState.getSerializable(ARG_POST);
            mComments = (ArrayList) savedInstanceState.getSerializable(ARG_COMMENTS);
        } else {
            if (getArguments() != null) {
                uid = getArguments().getString(ARG_UID);
                mPost = (Post) getArguments().getSerializable(ARG_POST);
            }
            mComments = CommentsManager.getInstance(uid, getActivity()).fetchComments(mPost.getPostId());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.single_post_item,container, false);

        tvName = (TextView) layout.findViewById(R.id.postUsername);
        tvTime = (TextView) layout.findViewById(R.id.postTime);
        tvLocation = (TextView) layout.findViewById(R.id.tvPostLocation);
        tvPost = (TextView) layout.findViewById(R.id.tvPostText);
        tvNumLikes = (TextView) layout.findViewById(R.id.tvPostNumLikes);
        tvNumComments = (TextView) layout.findViewById(R.id.tvPostNumComments);
        ivPostLike = (ImageView) layout.findViewById(R.id.ivPostLike);
        ivPostImage = (ImageView) layout.findViewById(R.id.postImage);
        ivProfilePicture = (ImageView) layout.findViewById(R.id.ivPostProfilePicture);
        extrasContainer = (ExpandableLinearLayout) layout.findViewById(R.id.postExtrasContainer);
        ivPostToggle = (ImageView) layout.findViewById(R.id.ivPostToggleButton);
        tagView = (TagView) layout.findViewById(R.id.postTagView);
        lv = (ListView) layout.findViewById(R.id.lvcommentspop);
        emptyView = (ImageView) layout.findViewById(R.id.commentsEmptyView);
        etComment = (EditText) layout.findViewById(R.id.etcommentonpost);
        ivPostComment = (ImageView) layout.findViewById(R.id.ivCommentSend);

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(ARG_UID);
            mPost = (Post) savedInstanceState.getSerializable(ARG_POST);
            mComments = (ArrayList) savedInstanceState.getSerializable(ARG_COMMENTS);
        }

        if (mPost.getNumLikes() == 0) {
            tvNumLikes.setText("No likes");
        } else {
            if (mPost.getNumLikes() > 1) {
                tvNumLikes.setText(Integer.toString(mPost.getNumLikes()) + " people liked this!");
            } else {
                tvNumLikes.setText(Integer.toString(mPost.getNumLikes()) + " person liked this!");
            }
        }

        if (mPost.getNumComments() == 0) {
            tvNumComments.setText("No comments");
        } else if (mPost.getNumComments() == 1) {
            tvNumComments.setText("1 comment");
        } else {
            tvNumComments.setText(Integer.toString(mPost.getNumComments()) + " comments");
        }

        tvPost.setText(mPost.getPostText());
        int distance = (int) mPost.getUser().getLocation().getDistance();
        String append = "";
        if (distance == 1) {
            append = " mile away";
        } else {
            append = " miles away";
        }
        tvLocation.setText(Integer.toString(distance) + append);
        tvName.setText(mPost.getUser().getName());
        tvTime.setText(mPost.getTime());
        ImageLoader imageLoader = new ImageLoader(getActivity());
        if (mPost.getUser().getPropicloc().equals("")) {
            ivProfilePicture.setImageResource(R.drawable.ic_placeholder);
        } else {
            imageLoader.DisplayImage(mPost.getUser().getPropicloc(), ivProfilePicture, false);
        }
        ivProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPost.getUser().getOuid().equals(uid)) {
                    getActivity().startActivity(ProfileActivity.createIntent(getActivity(), mPost.getUser()));
                }
            }
        });
        if (mPost.getLiked()) {
            ivPostLike.setImageResource(R.drawable.checkmark_liked);
        } else {
            ivPostLike.setImageResource(R.drawable.checkmark);
        }

        if (!mPost.getPostImage().equals("")) {
            Log.e(TAG, "showing post image");
            ivPostImage.setVisibility(View.VISIBLE);
            ivPostImage.setImageResource(R.drawable.ic_placeholder);
            imageLoader.DisplayImage(mPost.getPostImage(), ivPostImage, true);
            ivPostImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FullImageFragment fullImageFragment = FullImageFragment.newInstance(uid, mPost.getPostImage(), mPost.getPostingUsersId().equals(uid));
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, fullImageFragment, FullImageFragment.TAG)
                            .addToBackStack(null)
                            .commit();
                }
            });
        } else {
            ivPostImage.setVisibility(View.GONE);
        }


        ivPostToggle.setRotation(0);
        ivPostLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostManager.getInstance(uid, getActivity()).likePost(mPost.getPostId());
            }
        });

        if (mAdapter == null) {
            mAdapter = new CommentAdapter(getActivity(), mComments, mPost.getPostId());
        }
        lv.setAdapter(mAdapter);

        if (mComments.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }

        ivPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = etComment.getText().toString();
                if (!comment.equals("")) {
                    CommentsManager.getInstance(uid, getActivity()).postComment(mPost.getPostId(), comment);
                    Comment c = Comment.createComment(uid);
                    c.setComment(comment);
                    c.setUser(UserManager.getInstance(uid, getActivity()).me());
                    c.setTime("Just Now");
                    c.setPostId(mPost.getPostId());
                    mAdapter.addComment(c);
                    etComment.setText("");
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_UID, uid);
        outState.putSerializable(ARG_POST, mPost);
        outState.putSerializable(ARG_COMMENTS, mComments);
        super.onSaveInstanceState(outState);
    }
}