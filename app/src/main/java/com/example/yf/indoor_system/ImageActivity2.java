package com.example.yf.indoor_system;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yf on 2016/4/11.
 */
public class ImageActivity2 extends AppCompatActivity {

    private ImageMap imageMap2;
    HttpConnector photoHC,jsonHC;
    int Idest,Iminor;
    String JSONI=null;
    Toolbar toolbar;
    int[] mtest;


    private boolean turn = false;
    private boolean map_OK = false;
    private String MapName;
    private Boolean dangerous=false;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_main_display);

        Intent intent=getIntent();
//        Iminor=intent.getIntExtra("minor", Iminor);
//        Idest=intent.getIntExtra("dest", Idest);
        JSONI=intent.getStringExtra("JSON");
        mtest=intent.getIntArrayExtra("mfire");

        ImageView imageView = (ImageView) findViewById(R.id.image_view);

        Bitmap fire = BitmapFactory.decodeResource(getResources(), R.drawable.fire);

        photoHC = new HttpConnector("http://120.114.104.31/json/api");

        imageMap2 = new ImageMap(imageView, fire);

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

        Toast.makeText(ImageActivity2.this, getString(R.string.change_map) + " " + newMapName, Toast.LENGTH_SHORT).show();

        while (!map_OK) {
            try {
                new GetMapTask().execute(newMapName);
                MapName = newMapName;
                map_OK = true;
            } catch (Exception e) {
                if (Attempts == 0) {
                    Toast.makeText(ImageActivity2.this, getString(R.string.change_fail), Toast.LENGTH_SHORT).show();
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



            return "ok!";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // Add your image to your view
            if (s != null) {
                imageMap2.setMapImage(bitmap);
                imageMap2.setMapDot(JSON);
            }
            new DrawMap().execute();
        }
    }

    private class DrawMap extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {

                boolean draw = false;
            Log.w("mydebug_mapok0","ok");
                if (map_OK) {
                    Log.w("mydebug_mapok", "ok");
//                    imageMap.drawUserLocation(3);
//                    imageMap.drawUserLocation(2);
                    if (mtest!=null) {
                        imageMap2.drawPath2(mtest);
                    }
                    draw = true;
                }

                return draw;

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if (aBoolean){
                imageMap2.reloadImage();
            }
        }
    }


}
