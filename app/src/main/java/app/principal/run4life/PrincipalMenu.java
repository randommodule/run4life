package app.principal.run4life;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import app.principal.run4life.Entities.Alerta;
import app.principal.run4life.Helpers.MqttManager;
import app.principal.run4life.Interfaces.IdCallback;


public class PrincipalMenu extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private String  newspeed;
    private Sensor lightSensor;
    private View mainLayout;

    private Marker startMarker=null;

    private Marker currentLocationMarker;
    private Polyline routePolyline;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Button location;
    private Button sosButton;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView calorias;
    private ImageView logo;
    private GoogleMap googleMap;
    private MediaPlayer correctsound;
    private MediaPlayer wrongsound;
    private LocationManager locationManager;
    private boolean tracking = false;
    private Spinner weightSpinner;
    private double totalDistance = 0;
    private Location lastLocation;
    private boolean paused = false;
    private boolean isFirstLocation = true;
    private MqttManager mqttManager;
    private double latitud, longitud;
    private List<LatLng> locationHistory = new ArrayList<>();

    private LocationHandlerThread locationHandlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal_menu);
        mainLayout = findViewById(R.id.principalMenu);
        correctsound = MediaPlayer.create(this, R.raw.correct);
        wrongsound=MediaPlayer.create(this,R.raw.wrong);
        logo=findViewById(R.id.logoImage);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        resetButton = findViewById(R.id.resetButton);
        sosButton=findViewById(R.id.sosButton);
        location=findViewById(R.id.sendLocationButton);
        distanceTextView = findViewById(R.id.distanceTextView);
        speedTextView = findViewById(R.id.speedTextView);
        calorias = findViewById(R.id.caloriesTextView);
        weightSpinner = findViewById(R.id.weightSpinner);
        String brokerUrl = "tcp://lavendermouse241.cloud.shiftr.io:1883";
        String clientId = "Lavendermouse241";
        mqttManager = new MqttManager(brokerUrl, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("lavendermouse241");
        options.setPassword("ZuWwbzj2wXtoliku".toCharArray());
        mqttManager.connect(options, null);

        String[] weightValues = new String[201];
        for (int i = 0; i <= 200; i++) {
            weightValues[i] = String.valueOf(i);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weightValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weightSpinner.setAdapter(adapter);

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

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer != null) {
          sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
          Toast.makeText(this,"Este dispositivo no tiene acelerometro",Toast.LENGTH_SHORT).show();
        }

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTracking(v);
            }
        });
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                correctsound.start();
                sendLocation(v);
            }
        });
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                correctsound.start();
                registerAlert();
            }
        });

        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        requestLocationPermission();
        locationHandlerThread = new LocationHandlerThread("LocationHandlerThread", locationListener);
        locationHandlerThread.start();
        locationHandlerThread.prepareHandler();
        locationHandlerThread.setLocationListener(locationListener);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }

    public void goToMainActivity(View view) {
        correctsound.start();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            startLocationUpdates();
        }
    }

    public void startTracking(View view) {

        if (weightSpinner.getSelectedItem().equals("0")) {
            wrongsound.start();
            Toast.makeText(this, "No ha ingresado su peso en kilos", Toast.LENGTH_SHORT).show();
        }
        else {
            correctsound.start();
            tracking = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            paused = false;
            if (lastLocation == null) {
                lastLocation = getLastKnownLocation();

            }


            if (lastLocation != null && googleMap != null && startMarker == null) {
                LatLng startLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                startMarker = googleMap.addMarker(new MarkerOptions()
                        .position(startLatLng)
                        .title("Punto de inicio")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f));
            }
            locationHandlerThread.requestLocationUpdates(locationManager);

            Toast.makeText(this, "Recorrido iniciado", Toast.LENGTH_SHORT).show();
        }
    }

    public void resetTracking(View view) {
        if (weightSpinner.getSelectedItem().equals("0")) {
            wrongsound.start();
            Toast.makeText(this, "No ha ingresado su peso en kilos", Toast.LENGTH_SHORT).show();
        } else {
            if (routePolyline != null) {
                routePolyline.remove();
                routePolyline = null;
                locationHistory.clear();
            }
            correctsound.start();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            totalDistance = 0;
            isFirstLocation = true;
            distanceTextView.setText("Distancia recorrida: 0 km");
            speedTextView.setText("Velocidad: 0 m/s");
            calorias.setText("Calorias quemadas: 0 cal.");
            paused = false;
            if (startMarker != null) {
                startMarker.remove();
                startMarker = null;
            }
            if (lastLocation != null && googleMap != null && startMarker == null) {
                LatLng startLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                startMarker = googleMap.addMarker(new MarkerOptions()
                        .position(startLatLng)
                        .title("Punto de inicio")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f));
                locationHistory.add(startLatLng);
            }

            Toast.makeText(this, "Recorrido reiniciado", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTracking(View view) {
        correctsound.start();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        speedTextView.setText("Velocidad: 0 m/s");
        if (tracking) {
            tracking = false;
            paused = true;
            Toast.makeText(this, "Recorrido pausado", Toast.LENGTH_SHORT).show();
            stopLocationUpdates();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            showCurrentLocationOnMap(lastKnownLocation);
        }
    }

    private void showCurrentLocationOnMap(Location location) {
        if (googleMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentLocationMarker == null) {
                        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("Ubicación Actual")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    } else {
                        currentLocationMarker.setPosition(currentLatLng);
                    }
                    locationHistory.add(currentLatLng);

                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));

                    if (startMarker != null && locationHistory.size() > 1) {
                        if (routePolyline != null) {
                            routePolyline.remove();
                        }
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .width(5)
                                .color(Color.BLUE);
                        for (LatLng point : locationHistory) {
                            polylineOptions.add(point);
                        }

                        routePolyline = googleMap.addPolyline(polylineOptions);
                    }
                }
            });
        }
    }


    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return null;
    }

    public void startLocationUpdates() {
        if (locationHandlerThread != null) {
            locationHandlerThread.requestLocationUpdates(locationManager);

        } else {
            Log.e("PrincipalMenu", "locationHandlerThread is null");
        }
    }

    private void stopLocationUpdates() {
        if (!paused && locationManager != null) {
            locationHandlerThread.stopLocationUpdates(locationManager);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speedTextView.setText("Velocidad: 0 m/s");
                }
            });
        }
    }

    private void updateDistanceAndSpeed(Location newLocation) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(newLocation);
            totalDistance += distance;
            long timeElapsed = (newLocation.getTime() - lastLocation.getTime()) / 1000;
            if (timeElapsed > 0) {
                double speed = distance / timeElapsed;
                final String speedText = String.format("Velocidad: %.2f m/s", speed);
                final String distanceText = String.format("Distancia recorrida: %.2f km", totalDistance / 1000);

                if(!paused){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        distanceTextView.setText(distanceText);
                        if (accelerometer == null) {
                            speedTextView.setText(speedText);
                        }
                        calorias.setText("Calorias quemadas: " + ((int) (1.03 * Integer.parseInt(weightSpinner.getSelectedItem().toString()) * (totalDistance / 1000))) + " cal.");
                        showCurrentLocationOnMap(newLocation);
                    }
                });
            }
            }
        }
        lastLocation = newLocation;
        if (isFirstLocation) {
            lastLocation = newLocation;
            isFirstLocation = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Se requiere acceso a la ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        if (locationHandlerThread != null) {
            locationHandlerThread.quit();
            locationHandlerThread = null;
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location newLocation) {
            updateDistanceAndSpeed(newLocation);
        }
    };
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(accelerometer!=null){
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float velocidadX = event.values[0];
            float velocidadY = event.values[1];
            float velocidadZ = event.values[2];
            float velocidadTotal = (float) Math.sqrt(velocidadX * velocidadX + velocidadY * velocidadY + velocidadZ * velocidadZ);
            float umbralMovimiento = 1.0f;
            if (velocidadTotal > umbralMovimiento) {
               newspeed = "Velocidad: " + velocidadTotal + " m/s";
               if(!paused){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speedTextView.setText(newspeed);
                        calorias.setText("Calorias quemadas: " + ((int)(1.03 * Integer.parseInt(weightSpinner.getSelectedItem().toString()) * (totalDistance / 1000))) + " cal.");
                  }
                });
               }
            }
        }
        }
        if(lightSensor!=null){
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                float lightValue = event.values[0];
                if (lightValue > 20) {
                    logo.setImageResource(R.drawable.run4life);
                    mainLayout.setBackgroundResource(R.drawable.principal_menu_background);
                } else {
                    logo.setImageResource(R.drawable.run4life_night);
                    mainLayout.setBackgroundResource(R.drawable.principal_menu_night);
                }
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    public class LocationHandlerThread extends HandlerThread {
        private Handler handler;
        private LocationListener locationListener;
        public void setLocationListener(LocationListener listener) {
            locationListener = listener;
        }
        public LocationHandlerThread(String name, LocationListener listener) {
            super(name);
            locationListener = listener;
        }
        public void postTask(Runnable task) {
            handler.post(task);
        }

        public void prepareHandler() {
            handler = new Handler(getLooper());
        }

        public void requestLocationUpdates(LocationManager locationManager) {
            if (handler == null) {
                throw new IllegalStateException("Handler not prepared. Call prepareHandler() first.");
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationManager != null && locationListener != null) {
                        if (ActivityCompat.checkSelfPermission(PrincipalMenu.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PrincipalMenu.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

                    }
                }
            });
        }

        public void stopLocationUpdates(LocationManager locationManager) {
            if (handler == null) {
                throw new IllegalStateException("Handler not prepared. Call prepareHandler() first.");
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationManager != null && locationListener != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                }
            });
        }
    }
    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return null;
    }
    private String generateGoogleMapsLink(double latitude, double longitude) {
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }
    public void sendLocation(View view) {
        Location currentLocation = getCurrentLocation();

        if (currentLocation != null) {

            Intent sendIntent = new Intent("android.intent.action.SEND");
            Location myposition=getCurrentLocation();
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Mi ubicación: " + generateGoogleMapsLink(myposition.getLatitude(),myposition.getLongitude()));
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.whatsapp");

            try {
                startActivity(sendIntent);
            } catch (Exception e) {
                e.printStackTrace();
                wrongsound.start();
                Toast.makeText(this, "No se pudo enviar la ubicación", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerAlert() {
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                generateAsincronicId(new IdCallback() {
                    @Override
                    public void onIdGenerated(int id) {
                        FirebaseDatabase db = FirebaseDatabase.getInstance();
                        DatabaseReference dbref = db.getReference("Alertas");

                        // Obtener la fecha y hora actuales
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                        String fecha = dateFormat.format(calendar.getTime());
                        String hora = timeFormat.format(calendar.getTime());

                        Alerta nuevaAlerta = new Alerta(id, getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude(), fecha, hora);
                        dbref.child(String.valueOf(id)).setValue(nuevaAlerta);

                        // Notificar al servidor MQTT
                        String mensajeMQTT = "Nueva alerta: ID " + id + ", Fecha: " + fecha + ", Hora: " + hora + ", Latitud: " + latitud + ", Longitud: " + longitud;
                        MqttMessage mqttMessage = new MqttMessage();
                        mqttMessage.setPayload(mensajeMQTT.getBytes());
                        mqttManager.publish("Alerta", mqttMessage);


                        Toast.makeText(PrincipalMenu.this, "La Alerta ha sido publicada con ID: " + id, Toast.LENGTH_SHORT).show();

                    }
                });

            }
        });
    }
    private void generateAsincronicId(IdCallback callback) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                Random random = new Random();
                int id;
                boolean idExistente;

                do {
                    id = random.nextInt(1000000);
                    idExistente = verifyId(id);
                } while (idExistente);

                return id;
            }

            @Override
            protected void onPostExecute(Integer id) {
                super.onPostExecute(id);
                // Llamar al callback con la ID generada
                callback.onIdGenerated(id);
            }
        }.execute();
    }

    private boolean verifyId(int id) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("Alertas");

        final boolean[] existeId = {false};

        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Alerta alerta = dataSnapshot.getValue(Alerta.class);
                    if (alerta != null && alerta.getId() == id) {
                        existeId[0] = true;
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        return existeId[0];
    }
}