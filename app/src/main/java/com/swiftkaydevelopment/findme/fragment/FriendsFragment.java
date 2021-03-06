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
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.swiftkaydevelopment.findme.R;
import com.swiftkaydevelopment.findme.adapters.FriendsPagerAdapter;

public class FriendsFragment extends BaseFragment{
    public static final String TAG = "FriendsFragment";

    private FragmentPagerAdapter friendsPagerAdapter;

    private ViewPager mViewPager;

    public static FriendsFragment newInstance(String uid) {
        FriendsFragment frag = new FriendsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_UID, uid);
        frag.setArguments(b);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            uid = savedInstanceState.getString(ARG_UID);
        } else {
            if (getArguments() != null) {
                uid = getArguments().getString(ARG_UID);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.friendsfrag, container, false);
        TabLayout tabLayout = (TabLayout) layout.findViewById(R.id.tabs);
        mViewPager = (ViewPager) layout.findViewById(R.id.viewPagerFriendsFragment);
        if (friendsPagerAdapter == null) {
            friendsPagerAdapter = new FriendsPagerAdapter(getActivity().getSupportFragmentManager(), uid);
        }
        mViewPager.setAdapter(friendsPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_UID, uid);
        super.onSaveInstanceState(outState);
    }
}
