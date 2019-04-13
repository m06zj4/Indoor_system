package com.example.yf.indoor_system;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yf on 2016/4/11.
 */
public class ImageActivity extends AppCompatActivity {

    private ImageMap imageMap;
    HttpConnector photoHC,jsonHC;
    private ShortestPath SP1;
    int Idest,Iminor;
    String JSONI=null;
    Toolbar toolbar;


    private boolean turn = false;
    private boolean map_OK = false;
    private ShortestPath SP;
    private String MapName;
    private Boolean dangerous=false;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_main);

        Intent intent=getIntent();
        Iminor=intent.getIntExtra("minor", Iminor);
        Idest=intent.getIntExtra("dest", Idest);
        JSONI=intent.getStringExtra("JSON");
        try {
            dangerous=intent.getBooleanExtra("dangerous",dangerous);
        }catch (Exception e){}

        ImageView imageView = (ImageView) findViewById(R.id.image_view);

        Bitmap user = BitmapFactory.decodeResource(getResources(), R.drawable.user);
        Bitmap lock = BitmapFactory.decodeResource(getResources(), R.drawable.lock);
        Bitmap dire = BitmapFactory.decodeResource(getResources(), R.drawable.direction);

        photoHC = new HttpConnector("http://120.114.104.31/json/api");

        imageMap = new ImageMap(imageView, user, lock, dire);

        toolbar=(Toolbar)findViewById(R.id.toolbar);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setBackgroundColor(ContextCompat.getColor(ImageActivity.this, R.color.android800));

    if(dangerous){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.android900));
        }
    }else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.bluet));
        }
    }
//
        checkAndDownloadMap("I-4");
    }
    public int[] calculateShortestPath(int source, int destination, int option) {
        if (SP == null) {
            return null;
        }

        // SP.floydWarshell.calculateDistance();
        // SP.floydWarshell.output();

        SP.dijkstra.calculateDistance(source);
        SP.dijkstra.output(option, destination);

        //return SP.TextOut;
        return SP.dijkstra.getPath();
    }

    /*-----------------------------------------Map Code-------------------------------------------*/

    public void checkAndDownloadMap(String newMapName) {
        if (newMapName == null) {
            return;
        }
        if (newMapName.equals("")) {
            return;
        }
        if (MapName != null) {
            if (MapName.equals(newMapName)) {
                return;
            }
        }

        int Attempts = 5;

        map_OK = false;

        Toast.makeText(ImageActivity.this, getString(R.string.change_map) + " " + newMapName, Toast.LENGTH_SHORT).show();

        while (!map_OK) {
            try {
                new GetMapTask().execute(newMapName);
                MapName = newMapName;
                map_OK = true;
            } catch (Exception e) {
                if (Attempts == 0) {
                    Toast.makeText(ImageActivity.this, getString(R.string.change_fail), Toast.LENGTH_SHORT).show();
                    break;
                }

                Attempts--;
            }
        }

    }

    private class GetMapTask extends AsyncTask<String, Void, String> {
        String JSON = JSONI;
        Bitmap bitmap = null;
        int NodeTotal = 0;


        @Override
        protected String doInBackground(String... params) {
            if (params.length == 1) {
                JSON=JSONI;
                bitmap = photoHC.GetImage("Map", params[0]);
            } else {
                return null;
            }

            if (JSON == null) {
                return null;
            }

            try {
                JSONObject jsonData = new JSONObject(JSON);
                JSONObject information = jsonData.getJSONObject("information");
                NodeTotal = information.getInt("node");

            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

            if (NodeTotal > 0) {
                SP = new ShortestPath(NodeTotal);
                SP.setMatrixWithJson(JSON);
            } else {
                return null;
            }

            return "ok!";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // Add your image to your view
            if (s != null) {
                imageMap.setMapImage(bitmap);
                imageMap.setMapDot(JSON);
            }
            new DrawMap().execute();
        }
    }

    private class DrawMap extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {

                boolean draw = false;

                if (map_OK) {
                    imageMap.drawLockLocation(Idest);
                    imageMap.drawUserLocation(Iminor);

                    Integer source, destination;
//
                    source=Iminor;
                    destination=Idest;
                    if (source != null && destination != null) {
                        calculateShortestPath(source, destination, 2);
                        imageMap.drawPath(SP.dijkstra.getPath());
                        draw = true;
                    }
                }

                return draw;

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if (aBoolean){
                imageMap.reloadImage();
            }
        }
    }


}
