package com.example.yf.indoor_system;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity_display extends AppCompatActivity {

    private HttpConnector jsonHCtodisplay;
    private Boolean json_OK = false;
    String[] routename,temperature,humidity,newroutename;
    private ArrayAdapter<String> adapter;
    private static ExtendedSimpleAdapter listAdapter;
    ListView show;
    String JSON_Image = null;
    private int[] mfire;
    private boolean dangerous = false;
    boolean autodangerous;
    int counttodanger=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_display);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        jsonHCtodisplay = new HttpConnector("http://120.114.104.31/indoor/json/map_sensor/");

        checkjson("I-4");
    }

    public void checkjson(String newjsonname) {
        if (newjsonname == null) {
            return;
        }
        if (newjsonname.equals("")) {
            return;
        }

        int Attempts = 5;

        json_OK = false;
        while (!json_OK) {
            try {
                new GetJsonTask().execute(newjsonname);
                json_OK = true;
            } catch (Exception e) {
                if (Attempts == 0) {
                }
                Toast.makeText(MainActivity_display.this, getString(R.string.change_fail), Toast.LENGTH_SHORT).show();
                break;
            }
            Attempts--;

        }

    }
    public void jsonparse(String json) {

        Log.w("mydebug_jsonpar", "ok");

        int node;

        try {
            JSONObject jsonData = new JSONObject(json);
            JSONObject information = jsonData.getJSONObject("information");

            node = information.getInt("node");
            routename = new String[5];
            temperature=new String[5];
            humidity =new String[5];

            Log.w("Ydebug_test_0","ok");
            if (counttodanger==0) {
                autodangerous = information.getBoolean("status");
            }

            JSONArray algorithm = jsonData.getJSONArray("algorithm");

            int[] fire=new int[node];
            Log.w("Ydebug_test_1", "ok");

            int count=0;
            int count1=0;
            for (int i = 25; i < 30; i++) {
                JSONObject nowNode = algorithm.getJSONObject(i);
                routename[count] = nowNode.getString("room_name");

                JSONObject Sensor=nowNode.getJSONObject("sensor");
                temperature[count]= Sensor.getString("temperature");
                humidity[count]=Sensor.getString("humidity");

                count++;
            }
            for (int j=0;j<algorithm.length();j++){
                JSONObject nownode=algorithm.getJSONObject(j);
                if (nownode.getBoolean("danger")){
                    fire[count1]=j;
                    count1++;
                }
            }
            if(count1>0){
                mfire=new int[count1];
                System.arraycopy(fire,0,mfire,0,count1);
                Log.w("mydebug_mfire", "ok");
                dangerous=true;
            }


            Log.w("Ydebug_rou",routename[0]);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void setlistview(){

        Log.w("Ydebug_setlistview","ok");
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                routename);
        show=(ListView)findViewById(R.id.listView);
        show.setDividerHeight(1);
        List<HashMap<String, Object>> list = new ArrayList<>();

        for (int i=0;i<routename.length;i++){
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("shop_name",routename[i]);
            hashMap.put("temp",temperature[i]+"°C");
            hashMap.put("temp2",humidity[i]+"%");

            list.add(hashMap);
        }


        listAdapter = new ExtendedSimpleAdapter(
                this,
                list,
                R.layout.list_view_display,
                new String[]{"shop_name", "temp","temp2"},
                new int[]{R.id.shop_name, R.id.temp,R.id.temp2});
        show.setAdapter(listAdapter);
        setListViewHeightBasedOnChildren(show);

//
    }
    public void repeatintent(){
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        new GetJsonTask().execute();
    }
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    private class GetJsonTask extends AsyncTask<String, Void, String> {

        String JSON = null;


        @Override
        protected String doInBackground(String... params) {
            JSON = jsonHCtodisplay.SendPost("MAP", "I-4");
            Log.w("mydebug_getjson", "ok");

            if (JSON != null) {
                Log.w("mydebug_doinBack", "ok!");
            }
            if (JSON == null) {
                return null;
            }
            try {
                jsonparse(JSON);

            } catch (Exception e) {
                return null;
            }
            try {
                JSON_Image = JSON;
            } catch (Exception e) {
            }
            return null;
        }

        protected void onPostExecute(String s) {
            setlistview();
            repeatintent();
            if(autodangerous){
                Intent intent = new Intent();
                intent.setClass(MainActivity_display.this, ImageActivity2.class);
                intent.putExtra("JSON", JSON_Image);
                if(dangerous){
                    intent.putExtra("mfire",mfire);
                    Log.w("mydebug_mfire","ok");
                }
                autodangerous=false;
                counttodanger++;
                //false跟counttodanger是防止重複
                startActivity(intent);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            new GetJsonTask().execute();
            Intent intent = new Intent();
            intent.setClass(MainActivity_display.this, ImageActivity2.class);
            intent.putExtra("JSON", JSON_Image);
            if(dangerous){
                intent.putExtra("mfire",mfire);
                Log.w("mydebug_mfire","ok");
            }
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
