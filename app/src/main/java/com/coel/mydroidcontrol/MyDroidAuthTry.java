package com.coel.mydroidcontrol;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;

/**
 * Created by coel on 04.05.2016.
 */
public class MyDroidAuthTry implements Parcelable {
    public String host;
    public int port;
    public String password;

    public MyDroidAuthTry(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public MyDroidAuthTry(Parcel in) {
        host = in.readString();
        port = in.readInt();
        password = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeString(password);
    }

    public static final Parcelable.Creator<MyDroidAuthTry> CREATOR = new Parcelable.Creator<MyDroidAuthTry>() {

        @Override
        public MyDroidAuthTry createFromParcel(Parcel source) {
            return new MyDroidAuthTry(source);
        }

        @Override
        public MyDroidAuthTry[] newArray(int size) {
            return new MyDroidAuthTry[size];
        }
    };
}
