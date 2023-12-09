package app.principal.run4life;



import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.principal.run4life.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private Button start;
    private Button exit;
    private Button sosAlert;
    private ImageView logo;
    private View background;
    private MediaPlayer mainsong;
    private MediaPlayer corretsound;
    private Sensor lightSensor;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start=findViewById(R.id.startButton);
        exit=findViewById(R.id.exitButton);
        sosAlert=findViewById(R.id.alertasSosButton);
        mainsong=MediaPlayer.create(this,R.raw.main_menu_track);
        corretsound=MediaPlayer.create(this,R.raw.correct);
        logo=findViewById(R.id.logoImageMain);
        background =findViewById(R.id.mainBackground);
        mainsong.start();
        mainsong.setLooping(true);
        sensorManager = (SensorManager) getSystemService(this.SENSOR_SERVICE);
        if (sensorManager != null) {
            // Obtén una referencia al sensor de luminosidad
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (lightSensor != null) {
                // Registra el SensorEventListener
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                // El dispositivo no tiene sensor de luminosidad
                Toast.makeText(this, "El dispositivo no tiene un sensor de luminosidad", Toast.LENGTH_SHORT).show();
            }
        } else {
            // El dispositivo no tiene servicio de sensores
            Toast.makeText(this, "El dispositivo no tiene servicio de sensores", Toast.LENGTH_SHORT).show();
        }





    }
    public void closeApplication(View view) {
        corretsound.start();
        mainsong.stop();
        finishAffinity();
    }
    public void openSecondActivity(View view) {
        corretsound.start();
        mainsong.stop();
        Intent intent = new Intent(this, PrincipalMenu.class);
        startActivity(intent);
    }
    public void openAlertActivity(View view) {
        corretsound.start();
        mainsong.stop();
        Intent intent = new Intent(this, AlertActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mainsong != null && mainsong.isPlaying()) {
            mainsong.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainsong != null && !mainsong.isPlaying()) {
            mainsong.start();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if(lightSensor!=null){
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                float lightValue = event.values[0];
                if (lightValue > 20) {
                    logo.setImageResource(R.drawable.run4life);
                    background.setBackgroundResource(R.drawable.main_menu_wallpaper);
                } else {
                    logo.setImageResource(R.drawable.run4life_night);
                    background.setBackgroundResource(R.drawable.main_menu_night);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

