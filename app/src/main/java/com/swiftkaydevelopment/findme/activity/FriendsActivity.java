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

package com.swiftkaydevelopment.findme.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.fragment.FriendsFragment;

public class FriendsActivity extends BaseActivity{
    private static final String TAG = "FriendsActivity";
    private static final String ARG_UID = "ARG_UID";

    public static Intent createIntent(Context context) {
        Intent i = new Intent(context, FriendsActivity.class);
        return i;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_base;
    }

    @Override
    protected Context getContext() {
        return this;
    }

    @Override
    protected void createActivity(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(ARG_UID);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.baseActivityToolbar);
        toolbar.setTitle("Friends");
        toolbar.setNavigationIcon(R.mipmap.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        FriendsFragment friendsFragment = (FriendsFragment) getSupportFragmentManager().findFragmentByTag(FriendsFragment.TAG);
        if (friendsFragment == null) {
            friendsFragment = FriendsFragment.newInstance(uid);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activityContainer, friendsFragment, FriendsFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_UID, uid);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
