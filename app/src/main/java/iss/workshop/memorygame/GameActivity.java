package iss.workshop.memorygame;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    ImageView imgView = null;
    int matchCount = 0;
    int posIndex = -1;
    Handler handler = new Handler();

    private Boolean lastImgIsFaceUp;
    private Boolean clickable;
    private String lastImg;
    private ImageView lastClicked;
    private int matchedSets;
    private List<ImageView> matchedViews;
    private ArrayList<String> gridImages;
    private int seconds;
    private int minutes;
    private Boolean started;
    private ArrayList<String> sourceOfImages;
    private ArrayList<String> fileNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup action bar
        ActionBar actionBar = getSupportActionBar();//
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.topbar);

        Intent intent = getIntent();

        gridImages = intent.getStringArrayListExtra("images");

        int[] pos_ = {0, 1, 2, 3, 4, 5};

        List<Integer> pos = new ArrayList<Integer>();
        for (int p : pos_) {
            pos.add(p);
            pos.add(p);
        }

        Collections.shuffle(pos);

        startSettings();

        matchedViews = new ArrayList<ImageView>() {
        };

        setContentView(R.layout.activity_game);
        ImageAdapter imageAdapter = new ImageAdapter(this);
        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(imageAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageView currClick = (ImageView) view;
                if (clickable && matchedSets < pos_.length && !matchedViews.contains(currClick)) {
                    String currPath = gridImages.get(pos.get(position));
                    Bitmap bitmap = BitmapFactory.decodeFile(currPath);
                    currClick.setImageBitmap(bitmap);
                    // first flip
                    if (!lastImgIsFaceUp) {
                        started = true;
                        lastImgIsFaceUp = true;
                        lastImg = currPath;
                        lastClicked = currClick;
                        clickable = true;
                        //if not match
                    } else if (currPath != lastImg) {
                        clickable = false;
                        // timer until user can click again
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // overturn mismatched pair
                                currClick.setImageResource(R.drawable.closed);
                                lastClicked.setImageResource(R.drawable.closed);
                                lastImgIsFaceUp = false;
                                clickable = true;
                            }
                        }, 1000);
                        //if match
                    } else if (currPath == lastImg && lastClicked != currClick) {
                        lastImgIsFaceUp = false;
                        matchedSets++;
                        TextView textScore = findViewById(R.id.textMatches);
                        @SuppressLint("DefaultLocale") String text = String.format(
                                "%d of %d matched", matchedSets, pos_.length);
                        textScore.setText(text);
                        clickable = true;
                        matchedViews.add(currClick);
                        matchedViews.add(lastClicked);
                        if (matchedSets == pos_.length) {
                            started = false;
                            Intent intent = new Intent(GameActivity.this, PopUp.class);
                            intent.putExtra("endTime", String.format(
                                            "%02d:%02d", minutes, seconds));
                            intent.putExtra("images", gridImages);
                            startActivity(intent);
                        }
                    }

                }
            }
        });

        Button btn = findViewById(R.id.Restart);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recreate();
            }
        });

        runTimer();
    }
    private void startSettings() {
        lastImgIsFaceUp = false;
        clickable = true;
        matchedSets = 0;
        seconds = 0;
        minutes = 0;
        started = false;
    }
    private void runTimer() {
        TextView txtTime = findViewById(R.id.textTimer);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (seconds == 60) {
                    seconds = 0;
                    minutes++;
                }
                @SuppressLint("DefaultLocale") String text = String.format(
                        "%02d:%02d", minutes, seconds);
                txtTime.setText(text);

                if (started)
                    seconds++;
                handler.postDelayed(this, 1000);
            }
        });
    }
}