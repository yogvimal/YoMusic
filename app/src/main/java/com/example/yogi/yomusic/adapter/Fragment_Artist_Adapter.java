package com.example.yogi.yomusic.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.example.yogi.yomusic.MyApplication;
import com.example.yogi.yomusic.R;
import com.example.yogi.yomusic.activity.MainActivity;
import com.example.yogi.yomusic.activity.Secondary_Activity;
import com.example.yogi.yomusic.model.Constants;
import com.example.yogi.yomusic.pojo.SingleDataItem;
import com.example.yogi.yomusic.service.MediaPlayerService;
import com.example.yogi.yomusic.util.MusicLibrary;
import com.example.yogi.yomusic.util.UIElementHelper.FontFactory;

import java.util.ArrayList;

/**
 * Created by YOGI on 31-01-2018.
 */

public class Fragment_Artist_Adapter extends RecyclerView.Adapter<Fragment_Artist_Adapter.Artist_View_Holder>{
    private Context context;
    private ArrayList<SingleDataItem> Artist_List;
    private MediaPlayerService mediaPlayerService;

    private Drawable drawable;

    public Fragment_Artist_Adapter(Context context, ArrayList<SingleDataItem> Artist_List){
        this.context = context;
        this.Artist_List = Artist_List;
        mediaPlayerService = MyApplication.getService();

        drawable = ContextCompat.getDrawable(context, R.drawable.image1).mutate();
        Log.d("YOGI","Constructor Fragment_Artist_Adapter");
    }

    @Override
    public Artist_View_Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout,parent,false);
        return new Artist_View_Holder(view);
    }

    public void clear()
    {

    }

    @Override
    public void onBindViewHolder(Artist_View_Holder holder, int position) {
        holder.artist_name.setText(Artist_List.get(position).title);

        Uri uri = MusicLibrary.getInstance().getAlbumUri(MusicLibrary.getInstance().getFirstAlbumId(Artist_List.get(position).artist_id));

        Log.d("YOGI","Fragment_Artist_Adapter Uri = "+uri);
        //load images using Glide
        Glide
                .with(context)
                .asBitmap()
                .transition(BitmapTransitionOptions.withCrossFade(200))
                .load(uri)
                .apply(new RequestOptions()
                        .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                        .centerCrop()
                        .placeholder(drawable))
                .into(holder.thumbnail);

        holder.track_and_albums
                .setText(Artist_List.get(position).number_of_albums+
                        "|"+Artist_List.get(position).number_of_tracks);
    }

    @Override
    public int getItemCount() {
        return Artist_List.size();
    }

    public class Artist_View_Holder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView thumbnail,overflow;
        TextView artist_name,track_and_albums;

        public Artist_View_Holder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            overflow = itemView.findViewById(R.id.overflow);

            artist_name = itemView.findViewById(R.id.title);
            artist_name.setTypeface(FontFactory.getFont());

            track_and_albums = itemView.findViewById(R.id.artist);
            track_and_albums.setTypeface(FontFactory.getFont());

            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.overflow).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Fragment_Artist_Adapter.this.onClick(v,getLayoutPosition());
        }
    }

    private void onClick(View v, int layoutPosition) {
        String title;
        int key;
        switch (v.getId())
        {
            case R.id.cardView_root_layout:
                title = Artist_List.get(layoutPosition).artist_name;
                key = Artist_List.get(layoutPosition).artist_id;
                Intent intent = new Intent(context, Secondary_Activity.class);
                intent.putExtra("status", Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT);
                intent.putExtra("title",title.trim()); //to trim whitespaces
                intent.putExtra("key",key);
                context.startActivity(intent);
                ((MainActivity)context).overridePendingTransition(R.anim.slide_in_from_right,R.anim.slide_out_from_left);
                Log.d("YOGI","cardView clicked" + layoutPosition);
                break;
        }

    }
}
