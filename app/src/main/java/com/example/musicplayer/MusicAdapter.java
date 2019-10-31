package com.example.musicplayer;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import model.Music;

public class MusicAdapter extends ArrayAdapter<Music> {

    private int resourceId;

    public MusicAdapter(Context context, int textViewResourceId, List<Music> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Music music = getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
        ImageView musicImage = (ImageView)view.findViewById(R.id.music_image);
        TextView musicName = (TextView)view.findViewById(R.id.music_name);
        TextView artistName = (TextView)view.findViewById(R.id.artist_name);
        Bitmap bitmap = getAlbumArt(music.getAlbum_id());
        musicImage.setImageBitmap(bitmap);
        musicName.setText(music.getTitle());
        artistName.setText(music.getArtist());
        return view;
    }
    //获取专辑图片
    private Bitmap getAlbumArt(int album_id) {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = getContext().getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
            cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        Bitmap bm = null;
        if (album_art != null) {
            bm = BitmapFactory.decodeFile(album_art);
        } else {
            bm = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.default_cover);
        }
        return bm;
    }
}
