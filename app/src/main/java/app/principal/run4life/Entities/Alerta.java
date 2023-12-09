package app.principal.run4life.Entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Alerta implements Parcelable  {

    private int id;

    private double latitud;
    private double longitud;
    private String hora;
    private String fecha;

    public Alerta(int id, double latituda, double longituda, String fecha, String hora) {
        this.id = id;

        this.latitud = latituda;
        this.longitud = longituda;
        this.fecha=fecha;
        this.hora=hora;
    }




    public Alerta() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }



    // Resto del código ...

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    // Resto del código ...

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);

        dest.writeDouble(this.latitud);
        dest.writeDouble(this.longitud);
        dest.writeString(this.fecha);
        dest.writeString(this.hora);
    }

    public void readFromParcel(Parcel source) {
        this.id = source.readInt();
        this.latitud = source.readDouble();
        this.longitud = source.readDouble();
        this.fecha=source.readString();
        this.hora=source.readString();
    }

    protected Alerta(Parcel in) {
        this.id = in.readInt();
        this.latitud = in.readDouble();
        this.longitud = in.readDouble();
        this.fecha=in.readString();
        this.hora=in.readString();
    }

    public static final Creator<Alerta> CREATOR = new Creator<Alerta>() {
        @Override
        public Alerta createFromParcel(Parcel source) {
            return new Alerta(source);
        }

        @Override
        public Alerta[] newArray(int size) {
            return new Alerta[size];
        }
    };

}

