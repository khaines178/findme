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

package com.swiftkaydevelopment.findme.data;

import android.content.ContentValues;
import android.database.Cursor;

import com.swiftkaydevelopment.findme.data.datainterfaces.DatabaseObject;
import com.swiftkaydevelopment.findme.data.datainterfaces.JsonCreatable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Kevin Haines on 10/21/15.
 */
public class Post implements Serializable, JsonCreatable<Post>, DatabaseObject<Post> {

    public static final String TAG = "FindMe-Post";
    private String mPostId;
    private String mPostingUsersId;
    private User mUser;
    private String mPostText;
    private String mTime;
    private String mPostImage;
    private int mNumComments;
    public int numReactions;
    private boolean mLiked;
    public boolean disliked;
    private ArrayList<Comment> comments;

    /**
     * Creates a new Post object
     *
     * @return new Post object
     */
    public static Post createPost(){
        return new Post();
    }

    public static Post createPostFromJson(JSONObject object) {
        return Post.createPost().createObjectFromJson(object);
    }

    @Override
    public Post cursorToObject(Cursor c) {

        this.mPostId = c.getString(c.getColumnIndexOrThrow("mPostId"));
        this.mPostingUsersId = c.getString(c.getColumnIndexOrThrow("mPostingUserId"));
        this.mPostText = c.getString(c.getColumnIndexOrThrow("mPostText"));
        this.mNumComments = Integer.parseInt(c.getString(c.getColumnIndexOrThrow("mNumComments")));
        this.mLiked = Boolean.valueOf(c.getString(c.getColumnIndexOrThrow("mLiked")));

        //Need to make an additional call to db to get the user and a list of comments
        //if comments is greater than 0

        return this;
    }

    @Override
    public ContentValues objectToContentValues(Post object) {
        ContentValues values = new ContentValues();
        values.put("mPostId", mPostId);
        values.put("mPostingUsersId", mPostingUsersId);
        values.put("mPostText", mPostText);
        values.put("mPostImage", mPostImage);
        values.put("mNumComments", mNumComments);
        values.put("mLiked", mLiked);
        return values;
    }

    @Override
    public Post createObjectFromJson(JSONObject object) {
        try {
            this.setPostText(object.getString("post"));
            this.setPostingUsersId(object.getString("postingusersid"));
            this.setNumComments(object.getInt("numcomments"));
            this.numReactions = object.getInt("numreactions");
            this.setTime(object.getString("time"));
            this.setPostId(object.getString("postid"));
            this.setLiked(object.getBoolean("liked"));
            this.setPostImage(object.getString("postpicloc"));
            this.disliked = object.getBoolean("disliked");
            JSONObject user = object.getJSONObject("user");
            User u = SimpleUser.createUserFromJson(user);
            this.setUser(u);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * sets the id of the posting user
     * @param id uid of posting user
     */
    public void setPostingUsersId(String id){
        mPostingUsersId = id;
    }

    /**
     * sets the url location of the posts posted image
     * @param imgloc url String pointing to posted image
     */
    public void setPostImage(String imgloc){
        mPostImage = imgloc;
    }

    /**
     * gets the string url location of the posted image
     * @return url String location pointing to posted image
     */
    public String getPostImage(){
        return mPostImage;
    }

    /**
     * gets the uid of the posting user
     * @return uid of posting user
     */
    public String getPostingUsersId(){
        return mPostingUsersId;
    }

    /**
     * gets the user
     * @return User who posted post
     */
    public User getUser() {
        return mUser;
    }

    /**
     * sets the user who posted the post
     * @param user User who posted post
     */
    public void setUser(User user) {
        this.mUser = user;
    }

    /**
     * gets the post text
     * @return String post text
     */
    public String getPostText() {
        return mPostText;
    }

    /**
     * sets the post text
     * @param postText String text for post
     */
    public void setPostText(String postText) {
        this.mPostText = postText;
    }

    /**
     * gets arraylist of comments for post
     * @return List of comments for post
     */
    public ArrayList<Comment> getComments() {
        return comments;
    }

    /**
     * sets the list of comments for post
     * @param comments List of comments
     */
    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    /**
     * gets the number of comments for this post
     * @return Integer number of comments
     */
    public int getNumComments() {
        return mNumComments;
    }

    /**
     * sets the number of comments on this post
     * @param mNumComments Integer number of comments on this post
     */
    public void setNumComments(int mNumComments) {
        this.mNumComments = mNumComments;
    }

    /**
     * gets the amount of time since this post was posted
     * @return String time since post was posted
     */
    public String getTime() {
        return mTime;
    }

    /**
     * sets the amount of time since this post was posted
     * @param mTime time since post was posted
     */
    public void setTime(String mTime) {
        this.mTime = mTime;
    }

    /**
     * gets the id associated with this post
     * @return this post's id
     */
    public String getPostId(){
        return mPostId;
    }

    /**
     * sets the id for this post
     * @param postId String for post id
     */
    public void setPostId(String postId){
        mPostId = postId;
    }

    /**
     * sets whether the user has liked this post
     * @param liked true if user has liked this post
     */
    public void setLiked(boolean liked){
        mLiked = liked;
    }

    /**
     * gets whether the user has liked this post
     * @return true if user has liked this post
     */
    public boolean getLiked(){
        return mLiked;
    }
}
