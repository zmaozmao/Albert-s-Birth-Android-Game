package com.example.bizzi.GameSystem.Utility;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class JsonUtility {

    public static String readJsonFromFile(AssetManager assets, String filename) {
        String result=null;
        try {
            InputStream inputStream = assets.open("json/"+filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            result = sb.toString();
        } catch (IOException e) {
            //Log.d("Debug", "Unable open description file");
        }
        return result;
    }
}
