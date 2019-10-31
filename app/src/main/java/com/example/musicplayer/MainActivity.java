package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import model.Music;

public class MainActivity extends AppCompatActivity {

    public static MediaPlayer mediaPlayer = new MediaPlayer();
    public static int playingposition;

    private static final String TAG = "MainActivity";
    private ListView musicLists;
    private String[] Listsdata;
    private List<Music> musics = new ArrayList<Music>();
    private static final Uri URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private static final String[] MUSIC_PROJECTION = new String[] {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM,
            //专辑id
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.SIZE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //判断是否拥有读取数据的权限
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }else {
            musics = getAllMediaList(MainActivity.this);
        }

        musicLists = findViewById(R.id.musicLists);


        String[] expRe = new String[musics.size()];
        for (int i = 0; i < musics.size(); i++) {
            expRe[i] = musics.get(i).getTitle();
        }
        MusicAdapter adapter = new MusicAdapter(MainActivity.this, R.layout.music_item, musics);
        musicLists.setAdapter(adapter);
        musicLists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("musics", (Serializable) musics);
                intent.putExtra("position", position);
                startActivity(intent);
            }
        });

    }
//    private void openAlbum(){
//        Intent intent = new Intent("android.intent.action.GET_CONTENT");
//        intent.setType("image/*");
//        startActivityForResult(intent,CHOOSE_PHOTO);
//    }

    public static List<Music> getAllMediaList(Context context) {
        Cursor cursor = null;
        List<Music> musicList = new ArrayList<Music>();
        try {
            cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MUSIC_PROJECTION,
                    null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            if(cursor == null) {
                Log.d(TAG, "The getMediaList cursor is null.");
                return musicList;
            }
            int count= cursor.getCount();
            if(count <= 0) {
                Log.d(TAG, "The getMediaList cursor count is 0.");
                return musicList;
            }

//			String[] columns = cursor.getColumnNames();
            while (cursor.moveToNext()) {
                Music music = new Music();
                music.setId(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                music.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                music.setDisplay_name(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));
                music.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                music.setAlbums(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                music.setAlbum_id(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
                music.setSize(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)));
                if(!checkIsMusic(music.getDuration(), music.getSize())) {
                    continue;
                }
                music.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                music.setPath(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                musicList.add(music);
            }
        } catch (Exception e) {
            Log.d(TAG, "getAllMediaList: "+"error get music");
            e.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return musicList;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    musics = getAllMediaList(MainActivity.this);
                }else {
                    Toast.makeText(MainActivity.this, "拒绝权限将无法使用程序",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
    /**
     * 根据时间和大小，来判断所筛选的media 是否为音乐文件，具体规则为筛选小于30秒以下的
     */
    public static boolean checkIsMusic(int time, long size) {
        if(time <= 0 || size <= 0) {
            return  false;
        }
        //初始单位是毫秒
        time /= 1000;
        int minute = time / 60;
        //	int hour = minute / 60;
        //
        int second = time % 60;

        //minute/60的余数
        minute %= 60;
        //判断条件为小于一分钟且小于30秒
        if(minute <= 0 && second <= 30) {
            return  false;
        }
        if(size <= 1024 * 1024){
            return false;
        }
        return true;
    }




}
