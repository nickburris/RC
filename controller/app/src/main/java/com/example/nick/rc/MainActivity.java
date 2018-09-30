package com.example.nick.rc;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity {

    Socket socket = null;
    // TODO using a printwriter is not the best way to do this
    static PrintWriter socketOut = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            new ConnectionTask().execute();
        } catch(Exception e){
            e.printStackTrace();
        }

        // Event handlers
        ((RadioGroup)findViewById(R.id.motorStateRadioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkid) {
                String newMotorState = "";
                if(checkid == R.id.motorStateRadioReverse) {
                    newMotorState = "reverse";
                }else if(checkid == R.id.motorStateRadioForward){
                    newMotorState = "forward";
                }else{
                    System.err.println("Unexpected motor state id " + checkid);
                }
                System.out.println("sending motorstate");
                sendMessage("motorstate " + newMotorState);
            }
        });

        ((ToggleButton)findViewById(R.id.powerButton)).setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener(){
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendMessage("motorpower " + isChecked);
            }
        });

        ((SeekBar)findViewById(R.id.speedBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress is by default between 0 and 100
                sendMessage(String.format("motorspeed %03d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ((SeekBar)findViewById(R.id.steerBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress is by default between 0 and 100
                sendMessage(String.format("steer %03d", progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        ((Button)findViewById(R.id.resetSpeedSteering)).setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                ((SeekBar)findViewById(R.id.steerBar)).setProgress(50);
                ((SeekBar)findViewById(R.id.speedBar)).setProgress(0);
                sendMessage("steer 50");
                sendMessage("motorspeed 0");
            }
        });
    }

    private void setConnectedIndicator(boolean on){
        ((RadioButton)findViewById(R.id.connectedIndicator)).setChecked(on);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if(connected()) {
                socket.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private boolean connected(){
        return socketOut != null;
    }

    private static class ConnectionTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            PrintWriter socketOut = null;
            try {
                Socket socket = new Socket("192.168.0.26", 303);
                socketOut = new PrintWriter(socket.getOutputStream());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // unsafe way to do this
            MainActivity.socketOut = socketOut;
            System.out.println("Connected to server");

            return null;
        }
    }

    private void sendMessage(String m){
        try {
            if(connected()) {
                new SendTask().execute(m);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class SendTask extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... strings) {
            MainActivity.socketOut.write(strings[0]);
            MainActivity.socketOut.flush();

            return null;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
