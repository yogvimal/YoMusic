package com.example.yogi.yomusic.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.yogi.yomusic.R;
import com.example.yogi.yomusic.adapter.Fragment_Album_Adapter;
import com.example.yogi.yomusic.adapter.Fragment_Track_Adapter;
import com.example.yogi.yomusic.util.MusicLibrary;

/**
 * Created by YOGI on 30-01-2018.
 */

public class Fragment_Track extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View track_view;
    private Fragment_Track_Adapter fragment_track_adapter;

    public Fragment_Track(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        track_view = inflater.inflate(R.layout.fragment_track,container,false);
        swipeRefreshLayout = track_view.findViewById(R.id.fragment_album_swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        recyclerView = track_view.findViewById(R.id.fragment_album_recycler_view);

        new LoadSongs().execute("");
        return track_view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        recyclerView = null;
        if(fragment_track_adapter!=null)
        {
            fragment_track_adapter.clear();
        }
    }
    public void onRefresh() {
        new AsyncTask<String,Void,String>() {
            @Override
            protected String doInBackground(String... strings) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                swipeRefreshLayout.setRefreshing(false);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,"");
    }

    private class LoadSongs extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            fragment_track_adapter = new Fragment_Track_Adapter(getContext(), MusicLibrary.getInstance().getDataItemsForTracks());
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                recyclerView.setAdapter(fragment_track_adapter);
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setHasFixedSize(true);
            }

        }
    }
}
