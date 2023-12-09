package app.principal.run4life;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
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
import java.util.Locale;
import java.util.Random;

import app.principal.run4life.Entities.Alerta;
import app.principal.run4life.Helpers.MqttManager;
import app.principal.run4life.Interfaces.IdCallback;

//permite ver la lista de alertas SOS generadas y generar una
//Sube un reporte al servidor FireBase y envia una publicacion al servidor mqtt

public class AlertActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private ListView lvDatos;
    private Button  btnreg,btnret;
    private MediaPlayer correctsound;
    private MqttManager mqttManager;
    private double latitud, longitud;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        lvDatos = findViewById(R.id.lvDatos);
        btnreg = findViewById(R.id.publishButton);
        btnret=findViewById(R.id.returnButton);
        checkLocationPermission();
        correctsound = MediaPlayer.create(this, R.raw.correct);
        String brokerUrl = "tcp://lavendermouse241.cloud.shiftr.io:1883";
        String clientId = "Lavendermouse241";
        mqttManager = new MqttManager(brokerUrl, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("lavendermouse241");
        options.setPassword("ZuWwbzj2wXtoliku".toCharArray());
        mqttManager.connect(options, null);
        alertList();
        btnret.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttManager.disconnect();
                backToMainActivity(v);
            }
        });
        btnreg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerAlert();
            }
        });

    }
    public void backToMainActivity(View view) {
        correctsound.start();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


    private void alertList() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference dbref = db.getReference("Alertas");

        ArrayList<Alerta> listaAlertas = new ArrayList<>();
        ArrayAdapter<Alerta> adapter = new ArrayAdapter<>(AlertActivity.this, android.R.layout.simple_list_item_1, listaAlertas);
        lvDatos.setAdapter(adapter);

        dbref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Alerta alerta = snapshot.getValue(Alerta.class);
                listaAlertas.add(alerta);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        lvDatos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Alerta alerta = listaAlertas.get(i);
                AlertDialog.Builder builder = new AlertDialog.Builder(AlertActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Alerta Seleccionada");

                String msg = "ID: " + alerta.getId() + "\n\n";
                msg += "LATITUD: " + alerta.getLatitud() + "\n\n";
                msg += "LONGITUD: " + alerta.getLongitud()+ "\n\n";
                msg+="FECHA : "+ alerta.getFecha()+ "\n\n";
                msg+="HORA :"+alerta.getHora();

                builder.setMessage(msg);
                builder.show();
            }
        });
    }

    private void registerAlert() {
        correctsound.start();
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

                        Alerta nuevaAlerta = new Alerta(id, latitud, longitud, fecha, hora);
                        dbref.child(String.valueOf(id)).setValue(nuevaAlerta);

                        // Notificar al servidor MQTT
                        String mensajeMQTT = "Nueva alerta: ID " + id + ", Fecha: " + fecha + ", Hora: " + hora + ", Latitud: " + latitud + ", Longitud: " + longitud;
                        MqttMessage mqttMessage = new MqttMessage();
                        mqttMessage.setPayload(mensajeMQTT.getBytes());
                        mqttManager.publish("Alerta", mqttMessage);


                        Toast.makeText(AlertActivity.this, "La Alerta ha sido publicada con ID: " + id, Toast.LENGTH_SHORT).show();

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
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            obtenerUbicacionActual();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void obtenerUbicacionActual() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double latitud = location.getLatitude();
                                double longitud = location.getLongitude();
                                setLatitud(latitud);
                                setLongitud(longitud);

                            } else {
                                Toast.makeText(AlertActivity.this, "Ubicación no disponible", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }



}