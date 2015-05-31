package com.longdo.mjpegview;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.longdo.mjpegviewer.MjpegView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {
    private final String SERVER = "bma-itic1.iticfoundation.org";

    private MjpegView mjpegView;
    private ListView cameraList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mjpegView = (MjpegView) findViewById(R.id.mjpegview);
        mjpegView.setAdjustHeight(true);
        //view.setAdjustWidth(true);
        mjpegView.setMode(MjpegView.MODE_FIT_WIDTH);
        mjpegView.setUrl("http://" + SERVER + "/mjpeg2.php?camid=61.91.212.226");
        //view.seturl("http://trackfield.webcam.oregonstate.edu/axis-cgi/mjpg/video.cgi?resolution=800x600&amp%3bdummy=1333689998337");

        cameraList = (ListView) findViewById(R.id.cameraList);
        cameraList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String cameraName = adapterView.getItemAtPosition(i).toString();
                Toast.makeText(MainActivity.this,cameraName,Toast.LENGTH_SHORT).show();
                setTitle(cameraName);
                mjpegView.stopStream();
                mjpegView.setUrl("http://" + SERVER + "/mjpeg2.php?camid=" + cameraName.replaceAll(".* : ",""));
                mjpegView.startStream();
            }
        });

        new CameraListLoader().execute();
    }

    @Override
    protected void onResume() {
        mjpegView.startStream();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mjpegView.stopStream();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
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

    @Override
    protected void onStop() {
        mjpegView.stopStream();
        super.onStop();
    }

    class CameraListLoader extends AsyncTask<Void, Void, List<String>>{

        @Override
        protected List<String> doInBackground(Void... voids) {
            Log.i("CameraListLoader","Start download camera list.");

            ArrayList<String> list = new ArrayList<>();
            try {
                Socket socket = new Socket(SERVER,8001);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pw = new PrintWriter(socket.getOutputStream());
                pw.println("cameras");
                pw.flush();

                String read;
                while((read = br.readLine()) != null){
                    if(read.equals("--end-of-list--")){
                        break;
                    }
                    Log.i("CameraListLoader",read);

                    String type = read.replaceFirst("Ip:.*Type:\\s*","").replaceFirst(",.*","");
                    String name = read.replaceFirst("Ip:\\s*","").replaceFirst(",.*","");

                    list.add(type + " : " + name);
                }

                br.close();
                pw.close();
                socket.close();
            } catch (IOException e) {
                Log.e("CameraListLoader","Can not download camera list: " + e.getMessage());
                list.add("Can not download camera list.");
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<String> res) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, res);
            cameraList.setAdapter(adapter);
            super.onPostExecute(res);
        }
    }
}
