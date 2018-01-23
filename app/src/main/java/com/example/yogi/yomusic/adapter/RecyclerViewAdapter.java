package com.example.yogi.yomusic.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.yogi.yomusic.R;
import com.example.yogi.yomusic.model.Audio;
import com.example.yogi.yomusic.util.recyclerViewHelper.OnItemClickListener;

import java.util.Collections;
import java.util.List;

/**
 * Created by YOGI on 31-12-2017.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>
{

    private List<Audio> list = Collections.emptyList(); //Return Immutable Empty List
    private static Context context;
    private static OnItemClickListener clickListener;

    public RecyclerViewAdapter(Context context, List<Audio> list,OnItemClickListener clickListener)
    {
        this.context = context;
        this.list = list;
        this.clickListener = clickListener;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate the view
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        Audio audio = list.get(position);

        holder.title.setText(audio.getTitle());
        holder.artist.setText(audio.getArtist());
        /*holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("@YOGI","OnCardCLick");
                clickListener.onClick(holder.cardView,position);
            }
        });*/

        //fetching from MediaStore
        Cursor albumCursor = context.getContentResolver().query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID+" = ?",
                new String[]{list.get(position).getAlbum_id()},
                null);

        boolean queryResult = albumCursor.moveToFirst();
        String result = null;

        if(queryResult)
        {
            result = albumCursor.getString(albumCursor
                    .getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }

        Bitmap largeIcon = BitmapFactory.decodeFile(result);

        //Loading album cover using Glide Library
        Glide.with(context).load(largeIcon).into(holder.thumbnail);

        /*holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopUpMenu(holder.overflow);
            }
        });*/

    }

    private static void showPopUpMenu(ImageView overflow)
    {
        PopupMenu popupMenu = new PopupMenu(context,overflow);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_album,popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                switch (id)
                {
                    case R.id.action_add_favourite:
                        Toast.makeText(context,"Add to Favourite",Toast.LENGTH_LONG).show();
                        return true;
                    case R.id.action_play_next:
                        Toast.makeText(context,"Play Next",Toast.LENGTH_LONG).show();
                        return true;
                    default:
                }
                return false;
            }
        });

        Log.d("YOGI","Inside PopUp menu");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder
    {

        ImageView thumbnail,overflow;
        TextView title,artist;
        CardView cardView;
        public MyViewHolder(View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.thumbnail);
            overflow = itemView.findViewById(R.id.overflow);
            title = itemView.findViewById(R.id.title);
            artist = itemView.findViewById(R.id.artist);
            cardView = itemView.findViewById(R.id.cardView);

            overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("YOGI","Show PopUp menu");
                    showPopUpMenu((ImageView) v);
                }

            });

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(clickListener!=null)
                    {
                        int position = getAdapterPosition();
                        if(position!=RecyclerView.NO_POSITION)
                        {
                            Log.d("YOGI","CardView Clicked");
                            clickListener.onClick(cardView,position);
                        }
                    }

                }
            });
        }


    }
}
