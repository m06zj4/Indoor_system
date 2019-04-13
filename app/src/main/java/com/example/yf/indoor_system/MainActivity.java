package com.example.yf.indoor_system;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
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
            "I2408", "I2411", "I2412-1", "I2412-2", "Elevator-1", "Toilet-M1", "I1402", "I-1三叉點", "I2415",
            "Stairs-1", "I1401", "I2401", "I2404", "I2405", "I-2三叉點", "I-3三叉點", "I3401", "Stairs-2", "I3402", "Elevator-2", "Toilet-F1",
            "Office", "Meeting Room", "I5401", "I-4三叉點", "I4402", "I4401", "Stairs-3", "I5402", "Elevator-3", "Toilet-M2"
    };
    //
    int simulationminor = 2;
    TextView textView1;
    private BeaconManager beaconManager;
    Collection<Beacon> max;
    int minor, dest;
    //    private String major = null;
    private String major;
    private HttpConnector jsonHC;
    private Boolean json_OK = false;
    String showname = null;
    private ShortestPath SP;
    private Spinner mSpinner;
    private Context mContext;
    private ArrayAdapter<String> lunchList;
    private ArrayAdapter<String> adapter;
    private static ExtendedSimpleAdapter listAdapter;
    String[] routename,routenametodanger;
    int[] mDijk_result;
    ListView show;
    String JSON_Image = null;
    private boolean dangerous = false;

    private int BeaconCount = 0,userSelect;
    public Object BeaconDot[];
    private int[] mExit;
    Toolbar toolbar;
    Boolean test;
    int counttodanger=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textView1 = (TextView) findViewById(R.id.list_1);

        jsonHC = new HttpConnector("http://120.114.104.31/indoor/json/map/");

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


//---------------------------------下拉選單---------------------------------------------------------------------


        mContext = this.getApplicationContext();
        mSpinner = (Spinner) findViewById(R.id.list_2);

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
//------------------------------------------------------------------------------------------------------

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

//    -------------------------------------------------------------------------------------------

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
    public int[][] calculateShortestDestination() {
        if (SP == null) {
            return null;
        }

        SP.floydWarshell.calculateDistance();

        return SP.distMatrix;
    }
//--------------------------------------iBeacon-------------------------------------------------------------
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    BeaconFunction(beacons);
                    Log.w("mydebug_beaonctestbb", "ok");
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }
    }
    public void BeaconFunction(Collection<org.altbeacon.beacon.Beacon>beacons){
        String New_UUID = beacons.iterator().next().getId1().toString(),
                New_Major=beacons.iterator().next().getId2().toString(),
                New_Minor=beacons.iterator().next().getId3().toString();
        Log.w("mydebug_beaonctest0","ok");
        int count = BeaconCount;

        // 將舊Beacon資訊以新Beacon資訊取代
        while (count > 0) {
            String Old_UUID = ((Collection<org.altbeacon.beacon.Beacon>) BeaconDot[count - 1]).iterator().next().getId1().toString(),
                   Old_Major=((Collection<org.altbeacon.beacon.Beacon>)BeaconDot[count-1]).iterator().next().getId2().toString(),
                   Old_Minor=((Collection<org.altbeacon.beacon.Beacon>)BeaconDot[count-1]).iterator().next().getId3().toString();
            if (New_UUID.equals(Old_UUID)&&New_Major.equals(Old_Major)&&New_Minor.equals(Old_Minor)) {
                BeaconDot[count - 1] = beacons;
                break;
            } else {
                count--;
            }
        }
        // 若沒發現舊Beacon資訊則新增至陣列
        if (count <= 0) {
            BeaconCount++;
            Object Temp[] = new Object[BeaconCount];
            if (BeaconDot != null) {
                System.arraycopy(BeaconDot, 0, Temp, 0, BeaconDot.length);
            }

            Log.w("mydebug_beaonctest","ok");
            Object newLocateDot;
            newLocateDot = beacons;
            Temp[BeaconCount - 1] = newLocateDot;

            BeaconDot = Temp;
        }
        if (BeaconDot!=null){
            Log.w("BeaconDot","not null");
        }
        // 將所有Beacon資訊以RSSI強度做排序（氣泡排序）
        for (int i = 0; i < BeaconCount - 1; i++) {
            for (int j = 0; j < BeaconCount - i - 1; j++) {
                if (((Collection<org.altbeacon.beacon.Beacon>) BeaconDot[j + 1]).iterator().next().getRssi() > ((Collection<org.altbeacon.beacon.Beacon>) BeaconDot[j]).iterator().next().getRssi()) {
                    Object Temp = BeaconDot[j + 1];
                    BeaconDot[j + 1] = BeaconDot[j];
                    BeaconDot[j] = Temp;
                }
            }
        }
        // 刪除RSSI小於-99的Beacon資訊
        int Remove = 0;
        for (int i = 0; i < BeaconCount; i++) {
            int RSSI = ((Collection<org.altbeacon.beacon.Beacon>) BeaconDot[i]).iterator().next().getRssi();
            if (RSSI <= -99) {
                Remove++;
            }
        }
        if (Remove != 0) {
            BeaconCount = BeaconCount - Remove;
            Object Temp[] = new Object[BeaconCount];
            System.arraycopy(BeaconDot, 0, Temp, 0, BeaconCount);
            BeaconDot = Temp;
        }


        Object Beacon_Dot =BeaconDot[0];
        major =((Collection<org.altbeacon.beacon.Beacon>) Beacon_Dot).iterator().next().getId2().toString();
        minor=Integer.parseInt(((Collection<org.altbeacon.beacon.Beacon>) Beacon_Dot).iterator().next().getId3().toString());
        Log.w("mydebug_minor",String.valueOf(minor));
        Log.w("mydebug_major",major);
    }
//-----------------------------------------------------------------------------------------------------------------

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

        Log.w("mydebug_jsonpar", "ok");

        int node;
        try {
            JSONObject jsonData = new JSONObject(json);
            JSONObject information = jsonData.getJSONObject("information");

            node = information.getInt("node");
            classroomName = new String[node][2];
            routename = new String[node];

            if (counttodanger==0) {
                test = information.getBoolean("status");
            }

            JSONArray algorithm = jsonData.getJSONArray("algorithm");

            int[] temp=new int[node];
            int count=0;

            for (int i = 0; i < algorithm.length(); i++) {
                JSONObject nowNode = algorithm.getJSONObject(i);

                classroomName[i][0] = nowNode.getString("this");
                classroomName[i][1] = nowNode.getString("room_name");

                routename[i] = nowNode.getString("room_name");
                if(nowNode.getBoolean("isExit")){
                    temp[count]=i;
                    count++;
                }
            }

            if(count>0){
                mExit=new int[count];
                System.arraycopy(temp,0,mExit,0,count);
            }

            Log.w("mydebug_name",routename[0]);

            checkclassroom(classroomName);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void checkclassroom(String[][] class_Name) {

        Log.w("mydebug_ck_classroom", "OK!");
        if (major != null) {
            showname = class_Name[minor][1];
        }
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

    public void btn1(View view) {
        if (major == null) {
            return;
        }
        if (minor==dest){
            return;
        }


        Intent intent = new Intent();
        intent.setClass(MainActivity.this, ImageActivity.class);
        intent.putExtra("minor", minor);
        if(!dangerous){
            intent.putExtra("dest", dest);
        }else if (dangerous){
            if (mExit[userSelect]==minor){
                return;
            }
            intent.putExtra("dest",mExit[userSelect]);
            intent.putExtra("dangerous", dangerous);
        }
        intent.putExtra("JSON", JSON_Image);

        startActivity(intent);

    }

    public void btn2(View view) {
        Route_guidance();
    }
    public void Route_guidance(){
        String[] Dijk;
        if (major == null) {
            return;
        }


        if(!dangerous) {
            //演算法回傳路徑 裝進陣列
            calculateShortestPath(minor, dest, 2);
        } else if(dangerous){
            calculateShortestPath(minor,mExit[userSelect],2);
            if (minor==mExit[userSelect]){
                Toast.makeText(MainActivity.this, "目前已位於逃生出口", Toast.LENGTH_SHORT).show();
            }
            Log.w("mydebug_select", String.valueOf(mExit[userSelect]));
        }
            //防止空值
            if (SP.dijkstra.getPath() == null) {
                return;
            }
            mDijk_result = SP.dijkstra.getPath();



        Dijk = new String[mDijk_result.length];

        int count = mDijk_result.length - 1; //因為陣列由0開始算，故減一
        for (int j = 0; j < mDijk_result.length; j++) {
            int Path_number = mDijk_result[count];
            Dijk[j] = routename[Path_number];
            if (count >= 0) {
                count--;
            }
        }



//
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                Dijk);


        show = (ListView) findViewById(R.id.listView);
        List<HashMap<String, Object>> list = new ArrayList<>();
        //使用List存入HashMap，用來顯示ListView上面的文字。

        for (int i = 0; i < Dijk.length; i++) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("shop_name", Dijk[i]);
            //把title , text存入HashMap之中
//
            if (i == 0) {
                hashMap.put("img", R.drawable.me);
            } else if (i == Dijk.length - 1) {
                hashMap.put("img", R.drawable.weapons);
            } else {
                hashMap.put("img", R.drawable.arrows);
            }

            list.add(hashMap);
            //把HashMap存入list之中
        }


        listAdapter = new ExtendedSimpleAdapter(
                this,
                list,
                R.layout.list_view,
                new String[]{"img", "shop_name"},
                new int[]{R.id.img, R.id.shop_name});
        show.setAdapter(listAdapter);
        setListViewHeightBasedOnChildren(show);

    }


    public boolean dangerousMode(){
        if (major==null){
            return false;
        }


//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.android800));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.android900));
        }
        int[][] allDotDest =calculateShortestDestination();
        int[] exitDotDest = new int[mExit.length];
        String[] out = new String[mExit.length];
        if (mExit!=null){
            Log.w("mydebug_mExit","not null");
        }
        for (int i = 0; i < mExit.length; i++) {
            exitDotDest[i] = allDotDest[minor][mExit[i]];
            out[i] = "出口:" + routename[mExit[i]] + "\t\t\t" + "距離:" + String.valueOf(exitDotDest[i]+"m");
//            out[i] = "地點:" + String.valueOf(mExit[i]) + "\t\t\t" + "距離:" + String.valueOf(exitDotDest[i]);
        }

//        --------------------氣泡排序法_實作沒使用導致兜不上故不使用--------------------------------
//        for (int i = 0; i < mExit.length - 1; i++) {
//            for (int j = 0; j < mExit.length - i - 1; j++) {
//                if (exitDotDest[j + 1] < exitDotDest[j]) {
//                    int Temp = exitDotDest[j + 1];
//                    exitDotDest[j + 1] = exitDotDest[j];
//                    exitDotDest[j] = Temp;
//
//                    int Temp2 = mExit[j + 1];
//                    mExit[j + 1] = mExit[j];
//                    mExit[j] = Temp2;
//
//                    String Temp3 = out[j + 1];
//                    out[j + 1] = out[j];
//                    out[j] = Temp3;
//                }
//            }
//        }
//        --------------------------------------------------------------------------------*//-----
        userSelect = 0;

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener ons = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        // Cancel
                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        // User Select
//                        imageMap.drawLockLocation(mExit[userSelect]);
//                        new DrawMap().execute(3, 0);
                        dangerous=true;
                        Route_guidance();

                        break;
                    default:
                        if (which >= 0) {
                            userSelect = which;
                        } else {
                            // error
                            Log.e("Near List", "which error" + which);
                        }
                }
            }
        };
        dialog.setSingleChoiceItems(out, 0, ons);
        dialog.setPositiveButton("Go! Go!", ons);
        dialog.setNegativeButton("Cancel", ons);
        dialog.show();



        return true;
    }

    //------------------------------------異步執行------------------------------------------------------
    private class GetJsonTask extends AsyncTask<String, Void, String> {

        String JSON = null;
        int NodeTotal = 0;

        @Override
        protected String doInBackground(String... params) {
            JSON = jsonHC.SendPost("MAP", "I-4");
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
                if (test) {
                    test = dangerousMode();
                    test=false;
                    counttodanger++;
                    //false跟counttodanger是防止重複
            }
        }
    }

    //    ----------------------------//ExtendedSimpleAdapter 使用--------------------------------------
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

    //    ---------------------------------------------------------------------------------------------
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
        if (id == R.id.dangerous) {
            dangerous=dangerousMode();
            return true;
        }

        if (id==R.id.display){
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, MainActivity_display.class);
            startActivity(intent);
        }


        return super.onOptionsItemSelected(item);
    }

}
