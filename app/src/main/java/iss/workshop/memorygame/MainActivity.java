package iss.workshop.memorygame;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private GridViewAdapter gridAdapter;
    private Context context;
    private GridView gridView;
    private Button findLink;
    private EditText urlText;
    private String urlString;
    private ArrayList<String> sourceOfImages;
    private ArrayList<String> fileNames;
    private ProgressBar mProgressBar;
    private TextView mProgressTxt;
    private final String IMAGE_DESTINATION_FOLDER = Environment.DIRECTORY_PICTURES;
    private Thread bkgdThread;
    private Button nextPage;
    private ArrayList<String> selectedImages;
    private FindImageInURLLink async;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup action bar
        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.topbar);

        setContentView(R.layout.activity_main);
        context = this;
        sourceOfImages = new ArrayList<String>();
        fileNames = new ArrayList<String>();
        selectedImages = new ArrayList<String>();

        findLink = findViewById(R.id.submit_btn);
        urlText = (EditText) findViewById(R.id.url_input);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        mProgressTxt = findViewById(R.id.progressTxt);
        nextPage = findViewById(R.id.next_page_btn);

        //setting onclicklistener to FETCH button
        findLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if async thread currently exists, end it
                if (async != null){
                    async.cancel(true);
                }

                //remove existing images to save storage space
                removeOldImages();

                //initializing progress bar to 0/100
                mProgressBar.setMax(100);
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressTxt.setText("");

                sourceOfImages = new ArrayList<String>();
                fileNames = new ArrayList<String>();
                selectedImages = new ArrayList<String>();

                //resetting data for gridview
                gridAdapter = new GridViewAdapter(context, R.layout.cell, populateGridView(new ArrayList<String>(), new ArrayList<String>() ));

                //set adapter of gridview to the array of images
                gridView.setAdapter(gridAdapter);

                //getting url string
                urlString = urlText.getText().toString();

                //jsoup code starts
                async = new FindImageInURLLink();
                async.execute();

            }
        });


        //nothing after this code

        nextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGame();
            }
        });

        //find gridview
        gridView = (GridView) findViewById(R.id.grid_view);

        //this is where you fetch the data (after scanning the website)
        gridAdapter = new GridViewAdapter(this, R.layout.cell, populateGridView(sourceOfImages, fileNames));

        //set adapter of gridview to the array of images
        gridView.setAdapter(gridAdapter);

        //setting action when clicked
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ImageItem item = (ImageItem) adapterView.getItemAtPosition(i);
                LinearLayout lin = view.findViewById(R.id.bgcol);

                if (!item.getDrawableTag().equals("no_img")) {
                    if (!item.isCurrentlySelected()){
                        if (selectedImages.size() < 6){
                            lin.setBackgroundColor(getResources().getColor(R.color.red));
                            System.out.println(item.getDrawableTag());
                            selectedImages.add(item.getDrawableTag());

                            item.changeSelectedState();
                        } else {
                            Toast.makeText(context, "You can only choose up to six!", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        lin.setBackgroundColor(getResources().getColor(R.color.white));
                        System.out.println(item.getDrawableTag());
                        selectedImages.remove(item.getDrawableTag());
                        item.changeSelectedState();
                    }
                }
            }
        });
    }

    //starting the game
    public void startGame() {
        if (selectedImages.size() != 6){
            Toast.makeText(context, "Choose exactly six images before starting the game.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Game Time! Start whenever you're ready!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra("images", selectedImages);
            startActivity(intent);
        }
    }

    //jsoup implementation class
    public class FindImageInURLLink extends AsyncTask<Void, Void, Void>{

        //initializing some datastores
        ArrayList<String> dummySource = new ArrayList<String>();
        ArrayList<String> dummyNames = new ArrayList<String>();
        Random random = new Random();

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                //getting html doc
                Document doc = Jsoup.connect(urlString).get();

                //saving all img inside website
                Elements elems = doc.getElementsByTag("img");

                //for counting how many items total
                int counter = 0;

                //for loop for ALL elements, even if > 20
                for(Element elem : elems){
                    //20 because the limit is 20 images
                    if (counter < 20){
                        //if statement to check if image can be bitmapped
                        if (elem.attr("src").startsWith("https://")) {
                            //saving link to image
                            sourceOfImages.add(elem.attr("src"));

                            //subarray of sources
                            dummySource.add(sourceOfImages.get(counter));

                            //generating uuid for name of image
                            String destFilename = UUID.randomUUID().toString() + sourceOfImages.get(counter).lastIndexOf(".") + 1;

                            //saving filename to array
                            fileNames.add(destFilename);

                            //subarray of file names
                            dummyNames.add(fileNames.get(counter));

                            //doing the download
                            startDownloadService(sourceOfImages.get(counter), fileNames.get(counter));



                            //this is where the grid view updates
                            publishProgress();

                            Thread.sleep(100 * random.nextInt(20));
                            counter ++;
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            mProgressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //has to be runOnUiThread because if not, UI will not be updated
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //set new grid adapter to dummySource and dummyNames
                            gridAdapter = new GridViewAdapter(context, R.layout.cell, populateGridView(dummySource, dummyNames));

                            gridView.setAdapter(gridAdapter);

                            //update progress bar
                            mProgressBar.setProgress(dummySource.size() * 5);
                            mProgressTxt.setText("Downloading "+ dummySource.size() + " of 20 images");
                            updateProgressText(dummySource);
                        }
                    });
                }
            }, 1000); //how many milliseconds to delay
        }
    }

    protected void startDownloadService(String url, String filename) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction("download");
        intent.putExtra("filename", filename);
        intent.putExtra("where", url);
        startService(intent);
    }

    protected ArrayList<ImageItem> populateGridView(ArrayList<String> urls, ArrayList<String> filenames){
        final ArrayList<ImageItem> imageItems = new ArrayList<>();
        final int ABSOLUTE_SIZE = 20;

        if (urls.size() != 0 && filenames.size() != 0){
            for (int i = 0; i < filenames.size(); i++){
                Bitmap btmp = BitmapFactory.decodeFile(getExternalFilesDir(IMAGE_DESTINATION_FOLDER) + "/" + filenames.get(i));
                ImageItem imgItem = new ImageItem(btmp);
                imgItem.setDrawableTag(getExternalFilesDir(IMAGE_DESTINATION_FOLDER) + "/" + filenames.get(i));
                imageItems.add(imgItem);
            }
        }

        if (urls.size() < ABSOLUTE_SIZE && filenames.size() < ABSOLUTE_SIZE) {
            for (int i = 0; i < ABSOLUTE_SIZE - filenames.size(); i++){
                Bitmap btmp = BitmapFactory.decodeResource(getResources(), R.drawable.no_img);
                ImageItem imgItem = new ImageItem(btmp);
                imgItem.setDrawableTag("no_img");
                imageItems.add(imgItem);
            }
        }
        return imageItems;
    }
    public void updateProgressText(ArrayList<String> dummySource){
        if(dummySource.size() == 20){
            mProgressTxt.setText(getString(R.string.progressDone));
        }
    }

    protected void removeOldImages() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (dir.exists()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    new File(dir, child).delete();
                }
            }
        }
    }
}