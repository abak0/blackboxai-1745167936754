package com.example.ezviztvapp;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private ListView listView;
    private ProgressBar progressBar;
    private TextView statusText;

    private ArrayList<String> streamUrls = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        listView = findViewById(R.id.stream_list);
        progressBar = findViewById(R.id.progress_bar);
        statusText = findViewById(R.id.status_text);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, streamUrls);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedStream = streamUrls.get(position);
                playStream(selectedStream);
            }
        });

        scanNetworkForStreams();
    }

    private void scanNetworkForStreams() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Buscando streams RTSP en la red local...");
        new NetworkScannerTask().execute();
    }

    private void playStream(String streamUrl) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
        }
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        isPlaying = true;
        statusText.setText("Reproduciendo: " + streamUrl);
        listView.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private class NetworkScannerTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String baseIp = "192.168.1.";
            int rtspPort = 554;

            for (int i = 1; i < 255; i++) {
                String ip = baseIp + i;
                if (isRtspOpen(ip, rtspPort)) {
                    String streamUrl = "rtsp://" + ip + ":" + rtspPort + "/live";
                    publishProgress(streamUrl);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String streamUrl = values[0];
            streamUrls.add(streamUrl);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressBar.setVisibility(View.GONE);
            if (streamUrls.isEmpty()) {
                statusText.setText("No se encontraron streams RTSP en la red local.");
            } else {
                statusText.setText("Seleccione un stream para reproducir.");
            }
        }

        private boolean isRtspOpen(String ip, int port) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), 200);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isPlaying && keyCode == KeyEvent.KEYCODE_BACK) {
            playerView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            statusText.setText("Seleccione un stream para reproducir.");
            isPlaying = false;
            if (player != null) {
                player.release();
                player = null;
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
