package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import model.Music;

import static com.example.musicplayer.MainActivity.mediaPlayer;
import static com.example.musicplayer.MainActivity.playingposition;

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "PlayerActivity";
    //定时调用，实现固定时间执行（会开启一个线程）
    private Timer timer= new Timer();


    private TextView musicName;
    private TextView artistName;
    private TextView startTime;
    private TextView overTime;

    private Button start_pause;
    //播放模式的标志
    private Button playMode;
    private int mode = 0;
    private Button lastMusic;
    private Button nextMusic;
    private Button stop;

    private ImageView albumArt;
    private SeekBar seekBar;
    private int currentposition;
    private List<Music> musics = new ArrayList<Music>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_player);
        musicName = findViewById(R.id.Music_name);
        artistName = findViewById(R.id.Artist_name);
        startTime = findViewById(R.id.startTime);
        overTime = findViewById(R.id.overTime);
        albumArt = findViewById(R.id.album_art);
        seekBar = findViewById(R.id.seekbar);
        start_pause = findViewById(R.id.pause_continue);
        start_pause.setOnClickListener(new Start_Pause());
        playMode = findViewById(R.id.playMode);
        playMode.setOnClickListener(new PlayMode());
        lastMusic = findViewById(R.id.last);
        lastMusic.setOnClickListener(new LastMusic());
        nextMusic = findViewById(R.id.next);
        nextMusic.setOnClickListener(new NextMusic());


        Intent intent = getIntent();
        currentposition = intent.getIntExtra("position", 0);
        Log.d(TAG, "onCreate: currentposition:"+currentposition);

        musics = (List<Music>) intent.getSerializableExtra("musics");
        //设置界面显示音乐名，歌手名和专辑图片
        musicName.setText(musics.get(currentposition).getTitle());
        artistName.setText(musics.get(currentposition).getArtist());
        overTime.setText(changeTime(musics.get(currentposition).getDuration()));
        albumArt.setImageBitmap(getAlbumArt(musics.get(currentposition).getAlbum_id()));

        //判断权限
        if(ContextCompat.checkSelfPermission(PlayerActivity.this, Manifest.permission.
                WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(PlayerActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }else {
            initMediaPlayer();
            Log.d(TAG, "onCreate: afterinit: currentposition" + currentposition);
            //stopTimer();
            int duration = mediaPlayer.getDuration();
            //将歌曲总长度赋值非seekBar
            seekBar.setMax(duration);
            //使seekBar实时获取歌曲的进度
            getProgress();
            Log.d(TAG, "onCreate: aftergetProgress(): currentposition" + currentposition);
            //mediaPlayer.start();
        }

        //设置播放模式（循环播放，随机播放）
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer arg0) {
                Log.d(TAG, "onCompletion: Music Completed");
                decideNextMode();
            }
        });




        //
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Restart();
                return false;
            }
        });

        //用于修正播放按钮的背景（播放还是暂停）
        if(mediaPlayer.isPlaying() && playingposition == currentposition){
            start_pause.setBackground(getResources().getDrawable(R.drawable.pause));
        }

        //设置进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int process = seekBar.getProgress();
//                if(mediaPlayer != null && mediaPlayer.isPlaying()){
//                    mediaPlayer.seekTo(process);
//                }
                startTime.setText(changeTime(process));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopTimer();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int process = seekBar.getProgress();
                if(mediaPlayer != null && mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(process);
                }
            }
        });

    }

    public void decideNextMode(){
        switch (mode) {
            case 0:
                if (currentposition == musics.size() - 1) {
                    currentposition = 0;
                    playingposition = currentposition;
                } else {
                    currentposition += 1;
                    playingposition = currentposition;
                }

                musicName.setText(musics.get(currentposition).getTitle());
                artistName.setText(musics.get(currentposition).getArtist());
                albumArt.setImageBitmap(getAlbumArt(musics.get(currentposition).getAlbum_id()));

                Restart();

                break;
            case 1:
                Random random = new Random();
                int number = random.nextInt(musics.size());
                while (number == currentposition) {
                    number = random.nextInt();
                }

                currentposition = number;
                playingposition = number;
                musicName.setText(musics.get(currentposition).getTitle());
                artistName.setText(musics.get(currentposition).getArtist());
                albumArt.setImageBitmap(getAlbumArt(musics.get(currentposition).getAlbum_id()));
                Restart();

                break;
        }
    }


    //初始化播放器
    private void initMediaPlayer(){

        if(!mediaPlayer.isPlaying()) {
            playingposition = currentposition;
            Restart();
        }else if(mediaPlayer.isPlaying() && playingposition != currentposition){
            playingposition = currentposition;
            Restart();
        }

    }

    //获取专辑图片
    private Bitmap getAlbumArt(int album_id) {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = PlayerActivity.this.getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
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
            bm = BitmapFactory.decodeResource(PlayerActivity.this.getResources(), R.drawable.default_cover);
        }
        return bm;
    }

    //设置播放与暂停
    class Start_Pause implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            if(!mediaPlayer.isPlaying()){
                Log.d(TAG, "onClick: start");
                mediaPlayer.start();
                start_pause.setBackground(getResources().getDrawable(R.drawable.pause));
            }else if(mediaPlayer.isPlaying() && playingposition == currentposition){
                Log.d(TAG, "onClick: pause");
                mediaPlayer.pause();
                start_pause.setBackground(getResources().getDrawable(R.drawable.start));
            }
        }
    }
    //设置下一首歌曲
    class NextMusic implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            decideNextMode();
        }
    }

    class LastMusic implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            switch (mode) {
                case 0:
                    if (currentposition == 0) {
                        currentposition = musics.size() - 1;
                        playingposition = currentposition;
                    } else {
                        currentposition -= 1;
                        playingposition = currentposition;
                    }

                    musicName.setText(musics.get(currentposition).getTitle());
                    artistName.setText(musics.get(currentposition).getArtist());
                    albumArt.setImageBitmap(getAlbumArt(musics.get(currentposition).getAlbum_id()));

                    Restart();

                    break;
                case 1:
                    Random random = new Random();
                    int number = random.nextInt(musics.size());
                    while (number == currentposition) {
                        number = random.nextInt();
                    }

                    currentposition = number;
                    playingposition = number;
                    musicName.setText(musics.get(currentposition).getTitle());
                    artistName.setText(musics.get(currentposition).getArtist());
                    albumArt.setImageBitmap(getAlbumArt(musics.get(currentposition).getAlbum_id()));
                    Restart();

                    break;
            }
        }
    }

    //mediaPlayer继续播放(先恢复到初始状态再播放下一首)
    private void Restart(){
        try {
            stopTimer();
            Log.d(TAG, "beforeReset: currentposition:" + currentposition);
//            mediaPlayer.stop();
            mediaPlayer.reset();
//            mediaPlayer.stop();
//            mediaPlayer.release();
//            mediaPlayer = null;
            Log.d(TAG, "afterReset: currentposition:" + currentposition);
//            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musics.get(currentposition).getPath());//指定音频文件（下一个）
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            int duration = mediaPlayer.getDuration();
            //将歌曲总长度赋值非seekBar
            seekBar.setMax(duration);
            //使seekBar实时获取歌曲的进度
            getProgress();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //设置播放模式图标以及mode(int)的变化
    class PlayMode implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            switch(mode){
                case 0:
                    mode = 1;
                    playMode.setBackground(getResources().getDrawable(R.drawable.random));
                    break;
                case 1:
                    mode = 0;
                    playMode.setBackground(getResources().getDrawable(R.drawable.sequential));
            }
        }
    }

    private String changeTime(int mm){
        String result;
        int time = mm / 1000;
        int minute = time / 60;
        int second = time % 60;
        if(minute < 10){
            if(second < 10){
                result = "0"+minute+":0"+second;
            }else{
                result = "0"+minute+":"+second;
            }
        }else{
            if(second < 10){
                result = minute+":0"+second;
            }else{
                result = minute+":"+second;
            }
        }
        return result;
    }

    private void getProgress(){

        timer = new Timer();
        //这个方法是调度一个task，在delay（ms）后开始调度，每次调度完后，最少等待period（ms）后才开始调度
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //只在mediaplayer播放时执行以下操作，避免当mediaplayer.restart()后还没有prepare就执行而产生error(-38, 0)。
                if(mediaPlayer.isPlaying()) {
                    //获取歌曲的进度
                    final int p = mediaPlayer.getCurrentPosition();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startTime.setText(changeTime(p));
                        }
                    });
                    seekBar.setProgress(p);
                }

            }
        }, 1000, 1000);
    }

    private void stopTimer(){
        //System.gc();
        timer.cancel();
        Log.d(TAG, "stopTimer: timerTask over!");
    }
}
