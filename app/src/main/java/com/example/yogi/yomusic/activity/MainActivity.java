package com.example.yogi.yomusic.activity;

import android.Manifest;
import android.app.Notification;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.yogi.yomusic.R;
import com.example.yogi.yomusic.adapter.RecyclerViewAdapter;
import com.example.yogi.yomusic.model.Audio;
import com.example.yogi.yomusic.service.MediaPlayerService;
import com.example.yogi.yomusic.util.StorageUtil;
import com.example.yogi.yomusic.util.recyclerViewHelper.OnItemClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity{

    public static final String ACTION_PLAY_NEW_AUDIO = "com.example.musicplayer.PLAY_NEW_AUDIO";
    private static final int MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    //RecyclerView Related
    public RecyclerView recyclerView;
    public RecyclerViewAdapter recyclerViewAdapter;

    private MediaPlayerService mediaplayerService;
    boolean serviceBound = false;
    ArrayList<Audio> audioList;


    ImageView collapsingImageView;
    int imageIndex=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.collapse_toolbar);
        setSupportActionBar(toolbar);

        collapsingImageView = findViewById(R.id.collapse_imageview);

        loadCollapsingImage(imageIndex);

        if(checkAndRequestPermissions())
        {
            loadAudioList();
        }
        else
        {
            loadAudioList();
        }

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageIndex == 4)
                {
                    imageIndex = 0;
                    loadCollapsingImage(imageIndex);
                }
                else
                {
                    loadCollapsingImage(++imageIndex);
                }
            }
        });

    }

    private void loadAudioList()
    {
        loadAudio();
        initRecyclerView();
    }


    /**
     *
     *  ServiceConnection for Binding
     *
     */

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //We have bounded to service.Now get the refrence of service

            MediaPlayerService.LocalBinder localBinder = (MediaPlayerService.LocalBinder) service;
            mediaplayerService = localBinder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("YOGI","service disconnected");
            serviceBound =false;
        }
    };


    /**
     *Play the audio
     */

    private void playAudio(int audioIndex)
    {
        //check if service is active
        if(!serviceBound)
        {

            Log.d("YOGI","Activity Thread : "+Thread.currentThread());
            //Store the Serializable AudioList to SharedPreferences
            StorageUtil storageUtil = new StorageUtil(this);
            storageUtil.storeAudio(audioList);
            storageUtil.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this,MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent,serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        {
            //Store the new audioIndex
            StorageUtil storageUtil = new StorageUtil(this);
            storageUtil.storeAudioIndex(audioIndex);

            //Service is active so just trigger a broadcast;
            Intent broadcastIntent = new Intent(ACTION_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }

        Cursor albumCursor = getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID+" = ?",
                new String[]{audioList.get(audioIndex).getAlbum_id()},
                null);

        boolean queryResult = albumCursor.moveToFirst();
        String result = null;

        if(queryResult)
        {
            result = albumCursor.getString(albumCursor
                    .getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }

        Bitmap largeIcon = BitmapFactory.decodeFile(result);

        collapsingImageView.setImageBitmap(largeIcon);
    }

    /**
     *
     *
     * Load the Audio Files using Content Provider's or using other way as well
     *
     */

    private void loadAudio()
    {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.IS_MUSIC +"!=0";
        String sortOrder = MediaStore.Audio.Media.TITLE +" ASC";

        Cursor cursor = contentResolver.query(uri,null,selection,null,sortOrder);

        Cursor cursor1 = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID,MediaStore.Audio.Albums.ALBUM_ART},
                null,null,null);
        if(cursor!=null && cursor.getCount()>0)
        {
            audioList = new ArrayList<>();
            while(cursor.moveToNext())
            {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String album_id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                //save to audioList
                audioList.add(new Audio(data,title,album,artist,id,album_id));
            }
        }

        if(cursor!=null)
        {
            cursor.close();
        }
    }

    private void initRecyclerView()
    {
        if(audioList!=null && audioList.size()>0)
        {
            recyclerView = findViewById(R.id.recyclerview);

            recyclerViewAdapter = new RecyclerViewAdapter(this,audioList,
                    new OnItemClickListener() {
                        @Override
                        public void onClick(View view, int index) {
                            playAudio(index);
                        }
                    });
            recyclerView.setAdapter(recyclerViewAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(this,3));

            //Need to Read about Item Decoration and ItemAnimator thouroughly
            /*recyclerView.addItemDecoration(new GridSpacingItemDecoration(
                    2,dpToPx(10),true));*/
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            //

            //Implementing Custom ItemTouchListener
            //This listener used GestureDetector to intercept the touch event before
            //the touch event is delivered to any other view in the recyclerview
            //That's why we were getting no popup menu while touching overflow menu icon

            /*recyclerView.addOnItemTouchListener(new CustomTouchListener(
                    this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
                }
            }));*/
        }
    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
     */
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }



    private boolean checkAndRequestPermissions()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int permissionReadPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
            int permissionStorage = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE);

            List<String> listPermissionNeeded = new ArrayList<>();

            if(permissionReadPhoneState != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionNeeded.add(Manifest.permission.READ_PHONE_STATE);
            }

            if(permissionStorage != PackageManager.PERMISSION_GRANTED)
            {
                listPermissionNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if(!listPermissionNeeded.isEmpty())
            {
                ActivityCompat.requestPermissions(this,listPermissionNeeded.toArray(new String[listPermissionNeeded.size()]),MULTIPLE_PERMISSION_REQUEST_CODE);
                return false;
            }
            else
            {
                return true;
            }
        }

        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case MULTIPLE_PERMISSION_REQUEST_CODE:
                Map<String,Integer> perms = new HashMap<>();

                //Initialize the map with both permissions
                perms.put(Manifest.permission.READ_PHONE_STATE,PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE,PackageManager.PERMISSION_GRANTED);

                //fill with actual result from the user
                for(int i=0;i<permissions.length;i++)
                {
                    perms.put(permissions[i],grantResults[i]);
                }

                //Check for both permissions

                if(perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                        perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    //Both permission has been granted
                    loadAudioList();
                }
                else
                {
                    //Some permissions are not granted
                    //So if the user has not clicked on "do not ask again" check box
                    //Then show him the suggestion as to why this app need these permissions

                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_PHONE_STATE) ||
                            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE))
                    {
                        showDailogOK("Phone state and Storage permissions are required for this app",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which)
                                        {
                                            case DialogInterface.BUTTON_POSITIVE:
                                                checkAndRequestPermissions();
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                break;
                                        }
                                    }
                                });
                    }
                    else
                    {
                        Toast.makeText(this,"Revoke permissions in settings",Toast.LENGTH_LONG).show();
                    }
                }
        }
    }

    private void showDailogOK(String message, DialogInterface.OnClickListener clickListener)
    {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Ok",clickListener)
                .setNegativeButton("Cancel",clickListener)
                .create()
                .show();
    }

    private void loadCollapsingImage(int imageIndex)
    {

        //To get the set of values returned from a particular id:

        TypedArray typedArray = getResources().obtainTypedArray(R.array.images);
        collapsingImageView.setImageDrawable(typedArray.getDrawable(imageIndex));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if(serviceBound)
        {
            Log.d("YOGI","unbinding the service");
            unbindService(serviceConnection);

            //service is active
        }
        Log.d("YOGI","stoping the service");
        mediaplayerService.stopSelf();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("BOUND_STATE",serviceBound);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("BOUND_STATE");
    }
}

