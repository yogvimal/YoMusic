package com.example.yogi.yomusic.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.yogi.yomusic.R;
import com.example.yogi.yomusic.activity.MainActivity;
import com.example.yogi.yomusic.model.Audio;
import com.example.yogi.yomusic.model.PlaybackStatus;
import com.example.yogi.yomusic.util.StorageUtil;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by YOGI on 17-12-2017.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,AudioManager.OnAudioFocusChangeListener{

    public static final String ACTION_PLAY = "com.example.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.musicplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.musicplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.musicplayer.ACTION_STOP";

    //AudioPlayer Notification Id
    private static final int NOTIFICATION_ID = 101;

    //MediaPlayer
    private MediaPlayer mediaPlayer;

    //For MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSessionCompat;
    private MediaControllerCompat.TransportControls transportControls;

    //Audio Player


    //For pause/resume position
    private int resumePosition;

    /**
     * AudioFocus Start here
     */

    private AudioManager audioManager;

    //For API level 26(O) and above
    private AudioFocusRequest mFocusRequest;
    private AudioAttributes mAudioAttributes;
    //

    /**
     * AudioFocus End here
     */

    //Binder to given to clients
    private final IBinder binder = new LocalBinder();

    //List of Available audio Files
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio;

    //Handle Incoming Phone Calls
    private boolean ongoingCall = false;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;


    /**
     *
     * Service LifeCycle Methods
     *
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d("YOGI","onBind Service Thread : "+Thread.currentThread());
        return binder;
    }


    //First call received when an activity starts this service

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("YOGI","onCreate Service Thread : "+Thread.currentThread());
        /*Log.d("YOGI","onCreate");
        mediaPlayer = MediaPlayer.create(this,R.raw.lollipop);
        mediaPlayer.setOnCompletionListener(this);*/

        //Perform One Time Setups

        //Manage Incoming Phone Calls
        //Pause the player on incoming Call
        //Resume on Hangup;

        callStateListener();

        //ACTION_AUDIO_BECOME_NOISY : Change in audio outputs

        registerNoisyReceiver();

        //Listen for new Audio To Play
        registerPlayNewAudio();

    }



    //Going to be called for every Intent Request.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*Log.d("YOGI","onStartCommand");
        if(!mediaPlayer.isPlaying())
        {
            Log.d("YOGI",""+mediaPlayer);
            mediaPlayer.start();
        }*/

        //Load Data from sharedPreferences
        Log.d("YOGI","onStartCommand Service Thread : "+Thread.currentThread());

        StorageUtil storageUtil = new StorageUtil(getApplicationContext());
        audioList = storageUtil.loadAudio();
        audioIndex = storageUtil.loadAudioIndex();

        if(audioIndex != -1 && audioIndex <= audioList.size()-1)
        {
            //audioIndex is in a valid range
            activeAudio = audioList.get(audioIndex);
        }
        else
        {
            stopSelf();
        }

        //RequestAudioFocus
        if(requestAudioFocus())
        {
            stopSelf();
        }

        if(mediaSessionManager == null)
        {
            initMediaSession();
            initMediaPlayer();

            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Incoming intents from MediaSession.TransportControls
        handleIncomingIntents(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {

        Log.d("YOGI","onUnbind Service");
        mediaSessionCompat.release();
        removeNotification();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {

        Log.d("YOGI","onDestroy");
        /*if(mediaPlayer.isPlaying())
        {
            mediaPlayer.stop();
        }
        mediaPlayer.release();*/
        super.onDestroy();

        if(mediaPlayer!=null)
        {
            stopMedia();
            mediaPlayer.release();
        }

        removeAudioFocus();


        //Disable the phone state lisntner
        if(telephonyManager!=null)
        {
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister Broadcast receivers

        unregisterReceiver(noisyReceiver);
        unregisterReceiver(newAudioReceiver);


        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlayList();
    }

    /**
     *
     * Binder Class
     *
     */

    public class LocalBinder extends Binder
    {
        public MediaPlayerService getService()
        {
            return MediaPlayerService.this;
        }
    }

    /**
     *
     *MediaPlayer Callback Methods
     *
     */

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();

        removeNotification();

        //stop the service
        stopSelf();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //MediaPlayer is now in Prepared State
        //and ready to playback.
        playMedia();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what)
        {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onAudioFocusChange(int focusState)
    {
        //Invoked when the Audio Focus of the system has been update

        switch (focusState)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                //resume player
                if(mediaPlayer==null)
                    initMediaPlayer();
                else if(!mediaPlayer.isPlaying())
                    mediaPlayer.start();

                mediaPlayer.setVolume(1.0f,1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //Lost focus for indefinite amount of time
                //stop playback and release mediaplayer
                if(mediaPlayer.isPlaying())
                {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer=null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //AudioFocus has been lost for a short time
                //and is likely to resume. So just pause the media playback
                if(mediaPlayer.isPlaying())
                {
                    mediaPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //Lost focus for short time. but we can continue our playback
                //but with volume ducked down.
                if(mediaPlayer.isPlaying())
                {
                    mediaPlayer.setVolume(0.1f,0.1f);
                }
                break;
        }
    }

    /**
     *
     * Request Audio Focus
     *
     */

    private boolean requestAudioFocus()
    {
        //Initialize the audio Manager
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            mAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mAudioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .setWillPauseWhenDucked(true)
                    .build();

            int result = audioManager.requestAudioFocus(mFocusRequest);
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    private void removeAudioFocus()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            audioManager.abandonAudioFocusRequest(mFocusRequest);
        }
        else
        {
            audioManager.abandonAudioFocus(this);
        }
    }

    /**
     *
     * MediaPlayer Actions
     *
     */

    private void initMediaPlayer(){
        if(mediaPlayer==null)
        {
            mediaPlayer = new MediaPlayer();//New MediaPlayer instance
        }

        //Set up the MediaPlayer Event Listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);

        //Reset the MediaPlayer so that it may not be pointing to another data source
        mediaPlayer.reset();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            mediaPlayer.setAudioAttributes(mAudioAttributes);
        }
        else
        {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        Log.d("YOGI","data = "+activeAudio.getData()+"\n"
                +"title = "+activeAudio.getTitle()+"\n"
                +"album = "+activeAudio.getAlbum()+"\n"
                +"artist = "+activeAudio.getArtist()+"\n"
                +"id = "+activeAudio.getId()+"\n"
                +"album_id = "+activeAudio.getAlbum_id());

        try {

            mediaPlayer.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();
    }

    private void playMedia()
    {
        if(!mediaPlayer.isPlaying())
        {
            mediaPlayer.start();
        }
    }

    private void pauseMedia()
    {
        if(mediaPlayer.isPlaying())
        {
            //Note that pause transaction is an async Transaction.
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void stopMedia()
    {
        if(mediaPlayer==null)
            return;
        if(mediaPlayer.isPlaying())
        {
            //Note : You can call stop() from prepared,started,paused
            // and playbackcompleted state
            mediaPlayer.stop();
        }
    }

    private void resumeMedia()
    {
        if(!mediaPlayer.isPlaying())
        {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    private void skipToNext()
    {
        if(audioIndex == (audioList.size()-1))
        {
            //If Last in PlayList
            audioIndex = 0;
            activeAudio = audioList.get(audioIndex);
        }
        else
        {
            //Next in PlayList
            activeAudio = audioList.get(++audioIndex);
        }

        //Update Stored Index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        //Stop the MediaPlayer
        stopMedia();
        //reset the MediaPlayer
        mediaPlayer.reset();
        //initialize the MediaPlayer Again.
        initMediaPlayer();

    }

    private void skipToPrevious()
    {
        if(audioIndex == 0)
        {
            //If first in PlayList
            //set index to the last
            audioIndex = audioList.size()-1;
            activeAudio = audioList.get(audioIndex);
        }
        else
        {
            //set to previous in the playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update the stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
        //stop the media
        stopMedia();
        //reset the mediaplayer
        mediaPlayer.reset();
        //intialize the media player
        initMediaPlayer();
    }

    /**
     *
     * Handling Incoming Call's
     *
     */

    private void callStateListener()
    {
        //Get the Telephony Manager
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        //start listening for PhoneState Changes
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                switch (state)
                {
                    //if atleast one call exists or Phone is rining
                    //pause the media player
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if(mediaPlayer!=null)
                        {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if(mediaPlayer!=null)
                        {
                            if(ongoingCall)
                            {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };


        //Register the listener with the telephony manager
        //Listen for the changes to the device call state

        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     *
     *Reveiver for ACTION_AUDIO_BECOMES_NOISY
     *
     */

    private BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //pause audio for this event
            if(intent.getAction()==AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            {
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }
        }
    };

    private void registerNoisyReceiver() {
        //Register before playing and unregister after playing
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        registerReceiver(noisyReceiver,intentFilter);
    }

    /**
     *
     *Receiver for listening to new Audio.
     *
     */

    private BroadcastReceiver newAudioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get the New MediaIndex From SharedPreferences

            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();

            if(audioIndex!=-1 && audioIndex<=audioList.size()-1)
            {
                //audioIndex is in a valid range
                activeAudio = audioList.get(audioIndex);
            }
            else
            {
                stopSelf();
            }

            //reset Media to play new Music
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetadata();
            buildNotification(PlaybackStatus.PLAYING);

        }
    };

    private void registerPlayNewAudio() {

        IntentFilter intentFilter = new IntentFilter(MainActivity.ACTION_PLAY_NEW_AUDIO);

        registerReceiver(newAudioReceiver,intentFilter);
    }

    /**
     *
     * MediaSession and Notification Actions
     *
     */

    private void initMediaSession()
    {
        if(mediaSessionManager!=null)
            return;//mediaSession exists

        mediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);

        //Create new Media Session
        mediaSessionCompat = new MediaSessionCompat(getApplicationContext(),"AudioPlayer");

        //Get Media Session Transport Controls
        transportControls = mediaSessionCompat.getController().getTransportControls();

        //set Media Session as active so that it can receive
        //media buttons and transport commands

        mediaSessionCompat.setActive(true);

        //set the flag so that it can handle transport controls through
        //mediaSessionCompat.Callback

        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set MediaSession's MetaData
        updateMetadata();

        //Attach Callback to receive transport control events

        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {

            //Implement CallBack's
            @Override
            public void onPlay() {
                super.onPlay();

                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();

                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();

                skipToNext();
                updateMetadata();
                buildNotification(PlaybackStatus.PLAYING);

            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();

                skipToPrevious();
                updateMetadata();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();

                removeNotification();
                stopSelf();

            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });


    }



    private void updateMetadata()
    {
        Bitmap albumArt = BitmapFactory.decodeFile(getAlbumArt());

        //Update the current Metadata

        mediaSessionCompat.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,activeAudio.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,activeAudio.getAlbum())
                .build());
    }


    public String getAlbumArt()
    {
        Cursor albumCursor = getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums.ALBUM_ART,
                        MediaStore.Audio.Albums._ID},
                MediaStore.Audio.Albums._ID+" = ?",
                new String[]{activeAudio.getAlbum_id()},
                null);

        boolean queryResult = albumCursor.moveToFirst();
        String result = null;

        /*Log.d("YOGI","CURRENT_ID : "+albumCursor.getString(albumCursor
                        .getColumnIndex(MediaStore.Audio.Albums._ID))+"\n"
        +"CURRENT_ALBUM_ART : "+albumCursor.getString(albumCursor
                .getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)));*/

        if(queryResult)
        {
            result = albumCursor.getString(albumCursor
                    .getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }
        return result;
    }
    private void buildNotification(PlaybackStatus playbackStatus)
    {
        /**
         * Notification Actions -> playbackAction()
         * 0 -> Play
         * 1 -> Pause
         * 2 -> Next Track
         * 3 -> Previous Track
         */


        //Initialize default notification icon to ic_media_pause
        int notificationAction = android.R.drawable.ic_media_pause;

        //Initialize Pending Intent
        PendingIntent play_pauseAction = null;

        //Build a Notification according to current state of the player

        if(playbackStatus == PlaybackStatus.PLAYING)
        {
            //set the norification icon to ic_media_pause
            notificationAction = android.R.drawable.ic_media_pause;

            //Create the Pause action as a Pending Intent
            //So that it can be triggered later on

            play_pauseAction = playBackAction(1);

        }

        else if(playbackStatus == PlaybackStatus.PAUSED)
        {
            notificationAction = android.R.drawable.ic_media_play;

            play_pauseAction = playBackAction(0);
        }




        //We have to provide our own image
        //Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),R.drawable.image5);
        Bitmap largeIcon = BitmapFactory.decodeFile(getAlbumArt());
        //Create New Notification

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                //Hide the time stamp
                .setShowWhen(true)
                //set the notification style
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        //Attach our media session
                        .setMediaSession(mediaSessionCompat.getSessionToken())
                        //show our playback controls in compat view
                        .setShowActionsInCompactView(0,1,2))
                //set the notification color
                .setColor(getResources().getColor(R.color.colorAccent))
                //set the large and small icons
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setLargeIcon(largeIcon)

                .setPriority(Notification.PRIORITY_HIGH)
                //set Notification Content information

                //Notification Tilte = First Row
                .setContentTitle(activeAudio.getAlbum())
                //Notification Text = Second Row
                .setContentText(activeAudio.getArtist())
                //Large Text at the right hand side of the notification
                .setContentInfo(activeAudio.getTitle())
                //Add Playback Actions
                .addAction(android.R.drawable.ic_media_previous,"previous",playBackAction(3))
                .addAction(notificationAction,"pause",play_pauseAction)
                .addAction(android.R.drawable.ic_media_next,"next",playBackAction(2));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID,notificationBuilder.build());
    }

    private PendingIntent playBackAction(int actionNumber)
    {
        Intent intent = new Intent(this,MediaPlayerService.class);
        switch (actionNumber)
        {
            case 0:
                //Play
                intent.setAction(ACTION_PLAY);
                //Why flag has been set to zero
                return PendingIntent.getService(this,actionNumber,intent,0);
            case 1:
                //Pause
                intent.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this,actionNumber,intent,0);
            case 2:
                //Next Track
                intent.setAction(ACTION_NEXT);
                return PendingIntent.getService(this,actionNumber,intent,0);
            case 3:
                //Previous Track
                intent.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this,actionNumber,intent,0);
            default:
                break;
        }
        return null;
    }

    private void removeNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        Log.d("YOGI","NOTIFICATION REMOVED");
    }

    private void handleIncomingIntents(Intent intent)
    {
        if(intent == null || intent.getAction()==null)
            return;

        String playBackAction = intent.getAction();

        if(playBackAction.equalsIgnoreCase(ACTION_PLAY))
        {
            transportControls.play();
        }
        else if(playBackAction.equalsIgnoreCase(ACTION_PAUSE))
        {
            transportControls.pause();
        }
        else if(playBackAction.equalsIgnoreCase(ACTION_NEXT))
        {
            transportControls.skipToNext();
        }
        else if(playBackAction.equalsIgnoreCase(ACTION_PREVIOUS))
        {
            transportControls.skipToPrevious();
        }
        else if(playBackAction.equalsIgnoreCase(ACTION_STOP))
        {
            transportControls.stop();
        }
    }
}
