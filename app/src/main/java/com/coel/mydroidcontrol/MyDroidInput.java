package com.coel.mydroidcontrol;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by coel on 04.05.2016.
 */
public class MyDroidInput implements Parcelable {
    public boolean moveForward = false;
    public boolean moveBackward = false;
    public boolean turnLeft = false;
    public boolean turnRight = false;
    public int speed= 128;

    public MyDroidInput() {}

    public MyDroidInput(Parcel in) {
        int[] data = new int[5];
        for (int i = 0; i < 5; i++) {
            data[i] = in.readInt();
        }
        moveForward = data[0] != 0;
        moveBackward = data[1] != 0;
        turnLeft = data[2] != 0;
        turnRight = data[3] != 0;
        speed = data[4];
    }

    public MyDroidInput(MyDroidInput other) {
        this.moveForward = other.moveForward;
        this.moveBackward = other.moveBackward;
        this.turnLeft = other.turnLeft;
        this.turnRight = other.turnRight;
        this.speed = other.speed;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MyDroidInput other = (MyDroidInput) obj;
        if (turnLeft != other.turnLeft)
            return false;
        if (turnRight != other.turnRight)
            return false;
        if (moveForward != other.moveForward)
            return false;
        if (moveBackward != other.moveBackward)
            return false;
        if (speed != other.speed)
            return false;
        return true;
    }

    @Override
    public String toString() {
        String ret = getClass().getName();
        ret += ":";
        if (moveForward) {
            ret += "F1,";
        }
        else {
            ret += "F0,";
        }
        if (moveBackward) {
            ret += "B1,";
        }
        else {
            ret += "B0,";
        }
        if (turnLeft) {
            ret += "L1,";
        }
        else {
            ret += "L0,";
        }
        if (turnRight) {
            ret += "R1,";
        }
        else {
            ret += "R0,";
        }
        ret += "S" + Integer.toString(speed);
        return ret;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(moveForward ? 1 : 0);
        dest.writeInt(moveBackward ? 1 : 0);
        dest.writeInt(turnLeft ? 1 : 0);
        dest.writeInt(turnRight ? 1 : 0);
        dest.writeInt(speed);
    }

    public static final Parcelable.Creator<MyDroidInput> CREATOR = new Parcelable.Creator<MyDroidInput>() {

        @Override
        public MyDroidInput createFromParcel(Parcel source) {
            return new MyDroidInput(source);
        }

        @Override
        public MyDroidInput[] newArray(int size) {
            return new MyDroidInput[size];
        }
    };
}
