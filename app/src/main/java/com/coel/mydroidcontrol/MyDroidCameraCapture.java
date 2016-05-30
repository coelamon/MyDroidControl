package com.coel.mydroidcontrol;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by coel on 29.05.2016.
 */
public class MyDroidCameraCapture {
    private Format m_format;
    private int m_width;
    private int m_height;
    private byte[] m_pixelData;

    public enum Format {
        GRAY_4BIT(1);

        final int nativeInt;

        Format(int ni) {
            this.nativeInt = ni;
        }
    }

    public MyDroidCameraCapture(Format fmt, int width, int height, byte[] data) {
        m_format = fmt;
        m_width = width;
        m_height = height;
        m_pixelData = data;
    }

    public int getWidth() {
        return m_width;
    }

    public int getHeight() {
        return m_height;
    }

    public Bitmap createBitmap() {
        if (m_format == null) {
            return null;
        }
        int[] colors = new int[m_width*m_height];
        for (int j = 0; j < m_height; j++) {
            for (int i = 0; i < m_width; i++) {
                int dst = j * m_width + i;
                int src = j * (m_width / 2) + (int)(i / 2);
                byte v = m_pixelData[src];
                if ((i % 2) == 0) {
                    v = (byte)((v & 0x0F) << 4);
                } else {
                    v = (byte)(v & 0xF0);
                }
                colors[dst] = Color.argb(255, v, v, v);
            }
        }
        return Bitmap.createBitmap(colors, m_width, m_height, Bitmap.Config.ARGB_8888);
    }
}
