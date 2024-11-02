package com.example.mussic_nguyenthanhphat2304;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private ArrayList<Uri> songUris;
    private ArrayList<String> songNames;
    private ListView songListView;
    private int currentSongIndex = -1;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;

    private SeekBar seekBar;
    private TextView timer, duration;

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        songListView = (ListView) findViewById(R.id.song_list);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
        } else {
            loadSongs();
        }

        Button playButton = (Button) findViewById(R.id.btn_play);
        Button shuffleButton = (Button) findViewById(R.id.btn_shuffle);
        Button stopButton = (Button) findViewById(R.id.btn_end);
        Button pauseButton = findViewById(R.id.btn_pause);

        seekBar = findViewById(R.id.seekBar);
        timer = findViewById(R.id.timer);
        duration = findViewById(R.id.duration);

        playButton.setOnClickListener(v -> playMusic());
        stopButton.setOnClickListener(v -> stopMusic());
        shuffleButton.setOnClickListener(v -> shuffleSongs());
        pauseButton.setOnClickListener(v -> pauseMusic());

        songListView.setOnItemClickListener((parent, view, position, id) -> {
            currentSongIndex = position;
            Toast.makeText(this, "Đã chọn: " + songNames.get(position), Toast.LENGTH_SHORT).show();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b && mediaPlayer != null) {
                    mediaPlayer.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void pauseMusic() {

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
        } else {
            Toast.makeText(this, "Vui lòng phát một bài hát trước", Toast.LENGTH_SHORT).show();
        }

    }

    public void loadSongs(){
            songUris = new ArrayList<>();
            songNames = new ArrayList<>();
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,null);

            if(cursor != null) {
                while(cursor.moveToNext()) {
                    int id = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                    long songId = cursor.getLong(id);
                    Uri songUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(songId));

                    songUris.add(songUri);
                    songNames.add(cursor.getString(nameColumn));

                }
                cursor.close();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songNames);
            songListView.setAdapter(adapter);
        }

        private void playMusic() {
            if(currentSongIndex == -1)
            {
                Toast.makeText(this, "Vui lòng chọn một bài hát để phát", Toast.LENGTH_SHORT).show();
                return;
            }
            if(mediaPlayer != null){
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, songUris.get(currentSongIndex));
            mediaPlayer.start();

            seekBar.setMax(mediaPlayer.getDuration());
            duration.setText(formatTime(mediaPlayer.getDuration()));
            mediaPlayer.setOnCompletionListener(mp -> {
                currentSongIndex = (currentSongIndex + 1) % songUris.size(); // Chuyển sang bài tiếp theo
                playMusic(); // Phát bài tiếp theo
            });

            new Thread(() -> {
                while (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    runOnUiThread(() -> timer.setText(formatTime(mediaPlayer.getCurrentPosition())));
                    try {
                        Thread.sleep(1000); // Cập nhật mỗi giây
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private void stopMusic() {
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        }

        private void shuffleSongs() {
        if(songUris.size() > 0) {
            currentSongIndex = (int) (Math.random() * songUris.size());
            playMusic();
        }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            stopMusic();
        }

        @Override
        public void onRequestPermissionsResult(int requestcode, String[] permissions, int[] grantResult) {
            super.onRequestPermissionsResult(requestcode, permissions, grantResult);
            if(requestcode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
                if(grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                    loadSongs();
                } else {
                    Toast.makeText(this, "Quyền truy cập bị từ chối", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }