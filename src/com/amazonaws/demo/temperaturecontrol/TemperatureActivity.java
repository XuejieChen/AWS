/**
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.temperaturecontrol;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.Switch;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.PublishRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.google.gson.Gson;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.util.TimerTask;
import java.util.UUID;
import java.nio.ByteBuffer;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import java.io.UnsupportedEncodingException;
import java.util.Timer;

public class TemperatureActivity extends Activity {

    private static final String LOG_TAG = TemperatureActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a2uhaedzvhylm2.iot.us-west-2.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "cn-north-1:6056bcea-c945-452c-830c-ab9311880262";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.CN_NORTH_1;

    private static final String ThingName = "Grove_Sensor";
    //

    CognitoCachingCredentialsProvider credentialsProvider;
    //StaticCredentialsProvider credentialsProvider1;
    AWSCredentials awsCredentials;
    AWSIotDataClient iotDataClient;
    //AWSIotMqttManager mqttManager;
    String clientId;
    private final Timer  timer = new Timer();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            getShadows();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        iotDataClient = new AWSIotDataClient(credentialsProvider);
        //iotDataClient.set
        String iotDataEndpoint = CUSTOMER_SPECIFIC_ENDPOINT;
        iotDataClient.setEndpoint(iotDataEndpoint);

        EditText et = (EditText) findViewById(R.id.editText);
        et.setText("100");

        getShadows();
        timer.schedule(task, 2000, 2000);
    }
    public void Grove_SensorUpdated(String Grove_SensorState) {
        Gson gson = new Gson();
        Grove_Sensor tc = gson.fromJson(Grove_SensorState, Grove_Sensor.class);

        Log.i(LOG_TAG, String.format("Temperature: %d", tc.state.reported.Temperature));
        Log.i(LOG_TAG, String.format("Moisture: %d", tc.state.reported.Moisture));

        TextView Temperature = (TextView) findViewById(R.id.Temp);
        Temperature.setText(tc.state.reported.Temperature.toString());
        TextView Moisture = (TextView) findViewById(R.id.Moisture);
        Moisture.setText(tc.state.reported.Moisture.toString());
        TextView Light = (TextView) findViewById(R.id.Light);
        Light.setText(tc.state.reported.Light.toString());
        //EditText et = (EditText) findViewById(R.id.editText);
        //et.setText(tc.state.reported.threshold.toString());
        TextView relay = (TextView) findViewById(R.id.textView9);
        relay.setText(tc.state.reported.relay);
        //if(tc.state.reported.relay == "True"){
        //    relay.setText("On");
        //} else {
        //    relay.setText("Off");
        //}
        TextView threshold = (TextView) findViewById(R.id.textView11);
        threshold.setText(tc.state.reported.threshold.toString());
        ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);
        Switch aSwitch = (Switch) findViewById(R.id.RelaySwitch);
        Log.i(LOG_TAG, String.format("cloud_control: %s", tc.state.desired.cloud_control));
        if(tc.state.desired.cloud_control.equalsIgnoreCase("True")){
            Log.i(LOG_TAG, String.format("cloud_control: set true"));
            tb.setChecked(true);
            aSwitch.setEnabled(true);
        } else {
            Log.i(LOG_TAG, String.format("cloud_control: set false"));
            tb.setChecked(false);
            aSwitch.setEnabled(false);
        }
        if(tc.state.desired.cloud_relay.equalsIgnoreCase("True")){
            aSwitch.setChecked(true);
            aSwitch.setText("Open");
        } else {
            aSwitch.setChecked(false);
            aSwitch.setText("Close");
        }
        //tb.setFocusable(true);
     //   NumberPicker np = (NumberPicker) findViewById(R.id.setpoint);
     //   np.setValue(tc.state.desired.setPoint);

     //   ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);
     //   tb.setChecked(tc.state.desired.enabled);
    }

    public void getShadow(View view) {
        getShadows();
    }

    public void getShadows() {
        GetShadowTask getStatusShadowTask = new GetShadowTask(ThingName);
        getStatusShadowTask.execute();

        //GetShadowTask getControlShadowTask = new GetShadowTask("TemperatureControl");
        //getControlShadowTask.execute();
    }
    public void enableDisableClicked(View view) {
        ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);
        Switch aSwitch = (Switch) findViewById(R.id.RelaySwitch);
        Log.i(LOG_TAG, String.format("System %s", tb.isChecked() ? "enabled" : "disabled"));
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName(ThingName);
        //PublishTopicTask publishTopicTask = new PublishTopicTask();
        //publishTopicTask.setTopic("myTopic/GroveSenor/control");
        //String topic = "$aws/things/Grove_Sensor/shadow/update";
        String newState = String.format("{\"state\":{\"desired\":{\"cloud_control\":\"%s\"}}}",
                (tb.isChecked() ? "True" : "False"));
        updateShadowTask.setState(newState);
        updateShadowTask.execute();
        //try {
            //mqttManager.conn
        //    mqttManager.publishString(topic, newState, AWSIotMqttQos.QOS0);
        //} catch(Exception e){
        //    Log.i(LOG_TAG, "Publish error.", e);
        //    TextView tvStatus = (TextView) findViewById(R.id.textView6);
        //    tvStatus.setText("send error");
            //throw e;
        //}
        //publishTopicTask.setMessage(newState);
        Log.i(LOG_TAG, newState);
        //publishTopicTask.execute();
        if (tb.isChecked()){
            aSwitch.setEnabled(true);

        } else {
            aSwitch.setEnabled(false);

        }
    }

    public void updateSetpoint(View view) {
        EditText et = (EditText) findViewById(R.id.editText);
        String newSetpoint = et.getText().toString();
        Log.i(LOG_TAG, "New setpoint:" + newSetpoint);
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName(ThingName);
        String newState = String.format("{\"state\":{\"desired\":{\"threshold\":%s}}}", newSetpoint);
        Log.i(LOG_TAG, newState);
        updateShadowTask.setState(newState);
        updateShadowTask.execute();
    }
    /*
    public void updateSetpoint(View view) {
        NumberPicker np = (NumberPicker) findViewById(R.id.setpoint);
        Integer newSetpoint = np.getValue();
        Log.i(LOG_TAG, "New setpoint:" + newSetpoint);
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName("TemperatureControl");
        String newState = String.format("{\"state\":{\"desired\":{\"setPoint\":%d}}}", newSetpoint);
        Log.i(LOG_TAG, newState);
        //updateShadowTask.setState(newState);
        //updateShadowTask.execute();
    }*/
    public void relaySwitchClicked(View view) {
        Switch aSwitch = (Switch) findViewById(R.id.RelaySwitch);
        //ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);
        //Integer newSetpoint = np.;
        //Log.i(LOG_TAG, "New setpoint:" + newSetpoint);
        if (aSwitch.isChecked()){
            aSwitch.setText("Open");
        } else {
            aSwitch.setText("Close");
        }
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName(ThingName);
        //updateShadowTask.pub
        String newState = String.format("{\"state\":{\"desired\":{\"cloud_relay\":\"%s\"}}}", aSwitch.isChecked()?"True":"False");
        Log.i(LOG_TAG, newState);
        updateShadowTask.setState(newState);
        updateShadowTask.execute();

    }

    private class GetShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private final String thingName;

        public GetShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                GetThingShadowRequest getThingShadowRequest = new GetThingShadowRequest()
                        .withThingName(thingName);
                GetThingShadowResult result = iotDataClient.getThingShadow(getThingShadowRequest);
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                Log.e("E", "getShadowTask", e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(GetShadowTask.class.getCanonicalName(), result.getResult());
                if (ThingName.equals(thingName)) {
                    Grove_SensorUpdated(result.getResult());
                } //else if ("TemperatureStatus".equals(thingName)) {
                  //  temperatureStatusUpdated(result.getResult());
                //}
            } else {
                Log.e(GetShadowTask.class.getCanonicalName(), "getShadowTask", result.getError());
            }
        }
    }
    private class PublishTopicTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {
        private String topic;
        private String message;

        public void setTopic(String name) {
            topic = name;
        }

        public void setMessage(String state) {
            message = state;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                PublishRequest request = new PublishRequest();
                request.setTopic(topic);
                ByteBuffer payloadBuffer = ByteBuffer.wrap(message.getBytes());
                request.setPayload(payloadBuffer);
                iotDataClient.publish(request);
                String resultString = new String("success");
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "publishTopicTask", e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(UpdateShadowTask.class.getCanonicalName(), result.getResult());
            } else {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "Error in publish Topic",
                        result.getError());
            }
        }
    }
    private class UpdateShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private String thingName;
        private String updateState;

        public void setThingName(String name) {
            thingName = name;
        }

        public void setState(String state) {
            updateState = state;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                UpdateThingShadowRequest request = new UpdateThingShadowRequest();
                request.setThingName(thingName);

                ByteBuffer payloadBuffer = ByteBuffer.wrap(updateState.getBytes());
                request.setPayload(payloadBuffer);

                UpdateThingShadowResult result = iotDataClient.updateThingShadow(request);
                //iotDataClient.
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "updateShadowTask", e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(UpdateShadowTask.class.getCanonicalName(), result.getResult());
            } else {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "Error in Update Shadow",
                        result.getError());
            }
        }
    }
}
