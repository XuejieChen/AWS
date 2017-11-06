package com.amazonaws.demo.temperaturecontrol;

/**
 * Created by chenxj on 7/10/17.
 */

public class Grove_Sensor {
    public Grove_Sensor.State state;

    Grove_Sensor() {
        state = new Grove_Sensor.State();
    }

    public class State {
        Grove_Sensor.State.Desired desired;
        Grove_Sensor.State.Delta delta;
        Grove_Sensor.State.Reported reported;

        State() {
            desired = new Grove_Sensor.State.Desired();
            delta = new Grove_Sensor.State.Delta();
            reported = new Grove_Sensor.State.Reported();
        }

        public class Desired {
            Desired() {
            }

            public String relay;
            public String cloud_relay;
            public String cloud_control;
            public Integer threshold;
        }

        public class Reported {
            Reported() {
            }

            public Integer Light;
            public Integer Temperature;
            public String relay;
            public Integer Moisture;
            public String ID;
            public String timestamp;
            public String cloud_relay;
            public String cloud_control;
            public Integer threshold = 100;
        }

        public class Delta {
            Delta() {
            }

            public String relay;
            public String cloud_relay;
            public String cloud_control;
        }
    }

    public Long version;
    public Long timestamp;

}
