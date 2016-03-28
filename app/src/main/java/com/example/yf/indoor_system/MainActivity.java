package com.example.yf.indoor_system;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private String[] lunch_1 = {
            "I2408", "I2411", "I2412-1", "I2412-2", "Exit", "Toilet-M1", "I1402", "No.7", "I2415",
            "No.9", "I1401", "I2401", "I2404", "I2405", "No.14", "No.15", "I3401", "Exit", "I3402", "Exit", "Toilet-F1",
            "Office", "Meeting Room", "I5401", "No.24", "I4402", "I4401", "Exit", "I5402", "Exit", "Toilet-M2"
    };
    //
    int simulationminor = 2;
    TextView textView1;
    private BeaconManager beaconManager;
    Collection<Beacon> max;
    int minor, dest;
    private String major = null;
    private HttpConnector jsonHC;
    private Boolean json_OK = false;
    String showname = null;
    private ShortestPath SP;
    private Spinner mSpinner;
    private Context mContext;
    private ArrayAdapter<String> lunchList;
    private ArrayAdapter<String> adapter;
    String[] routename;
    int[] mDijk_result;
    ListView show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textView1 = (TextView) findViewById(R.id.list_1);

        jsonHC = new HttpConnector("http://120.114.104.31/test/get_map2.php");

//        ------simulation location
//        if (classroom_name != null) {
//            name = classroom_name[simulationminor];
//            try {
//                textView.setText(name);
//
//            } catch (Exception e) {
//            }
//        }
        checkjson("I-4");

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));     //0215為讀取iBeacon  beac為altBeacon
//        beaconManager.bind(this);


        mContext = this.getApplicationContext();
        mSpinner = (Spinner) findViewById(R.id.list_2);


//---------------------------------下拉選單---------------------------------------------------------------------

        lunchList = new ArrayAdapter<String>(MainActivity.this, R.layout.myspinner, lunch_1);
        mSpinner.setAdapter(lunchList);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Toast.makeText(mContext, "你選的是" + lunch_1[position], Toast.LENGTH_SHORT).show();

                checkDijkstra(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
//---------------------------------下拉選單---------------------------------------------------------------------

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (beaconManager != null) {
            beaconManager.bind(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (beaconManager != null) {
            beaconManager.unbind(this);
        }
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

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {


                    if (max == null) {
                        max = beacons;
                    } else if (max.iterator().next().getRssi() > beacons.iterator().next().getRssi()) {
                        max = beacons;
                    }
                    minor = Integer.parseInt(max.iterator().next().getId3().toString());
                    major = max.iterator().next().getId2().toString();
                    Log.w("mydebug_minor", String.valueOf(minor));
//                    checkclassroom(minor);

                }

            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }
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
                Toast.makeText(MainActivity.this, getString(R.string.change_fail), Toast.LENGTH_SHORT).show();
                break;
            }
            Attempts--;

        }

    }

    public void jsonparse(String json) {
        String[][] classroomName;

        int node;
        try {
            JSONObject jsonData = new JSONObject(json);
            JSONObject information = jsonData.getJSONObject("information");

            node = information.getInt("node");
            classroomName = new String[node][2];
            routename = new String[node];

            JSONArray algorithm = jsonData.getJSONArray("algorithm");

            for (int i = 0; i < algorithm.length(); i++) {
                JSONObject nowNode = algorithm.getJSONObject(i);

                classroomName[i][0] = nowNode.getString("this");
                classroomName[i][1] = nowNode.getString("room_name");

                routename[i] = nowNode.getString("room_name");

            }


            checkclassroom(classroomName);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void checkclassroom(String[][] class_Name) {

        Log.w("mydebug_ck_classroom", "OK!");
        if (major != null)
            showname = class_Name[minor][1];
        new GetJsonTask().execute();
        Log.w("mydebug_showname", showname);

    }

    public void checkDijkstra(int mdest) {

        try {
            dest = mdest;
        } catch (Exception e) {
            return;
        }


    }

    public void btn2(View view) {
        String[] Dijk;
        if (major == null) {
            return;
        }
        calculateShortestPath(minor, dest, 2);
        mDijk_result = SP.dijkstra.getPath();
        Dijk = new String[mDijk_result.length]; //第一次會有小問題
        for (int j = 0; j < mDijk_result.length; j++) {
            int Path_number=mDijk_result[j];
            Dijk[j]=routename[Path_number];
        }
//
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                Dijk);


        show = (ListView) findViewById(R.id.listView);
        List<HashMap<String, String>> list = new ArrayList<>();
        //使用List存入HashMap，用來顯示ListView上面的文字。

        for (int i = 0; i < Dijk.length; i++) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("store_name", Dijk[i]);
            //把title , text存入HashMap之中
            list.add(hashMap);
            //把HashMap存入list之中
        }

        SimpleAdapter listAdapter = new SimpleAdapter(
                this,
                list,
                R.layout.list_notice,
                new String[]{"store_name"},
                new int[]{R.id.store_name});
        // 5個參數 : context , List , layout , key1 & key2 , text1 & text2

        show.setAdapter(listAdapter);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class GetJsonTask extends AsyncTask<String, Void, String> {

        String JSON = null;
        int NodeTotal = 0;

        @Override

        protected String doInBackground(String... params) {
            JSON = jsonHC.SendPost("MAP", "I-4");

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
                JSONObject jsonData = new JSONObject(JSON);
                JSONObject information = jsonData.getJSONObject("information");
                NodeTotal = information.getInt("node");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (NodeTotal > 0) {
                SP = new ShortestPath(NodeTotal);
                SP.setMatrixWithJson(JSON);

            } else {
                return null;
            }


            return null;

        }

        protected void onPostExecute(String s) {


            if (showname != null) {
                textView1.setText(showname);
                Log.w("mydebug_onpost", showname);
                Log.w("mydebug_onpost_2", "OK");
            }
        }

    }
}
