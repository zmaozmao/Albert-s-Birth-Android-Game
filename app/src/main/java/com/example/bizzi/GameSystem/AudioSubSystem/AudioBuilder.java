package com.example.bizzi.GameSystem.AudioSubSystem;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.bizzi.GameSystem.GameObSubSystem.GameObject;
import com.example.bizzi.GameSystem.Utility.Builder;
import com.example.bizzi.GameSystem.Utility.JsonUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AudioBuilder implements Builder {

    public final GameAudio gameAudio;
    private final AssetManager assets;

    public AudioBuilder(Context context){
        assets=context.getAssets();
        gameAudio=new GameAudio(context);
    }

    @Override
    public void build(){
        AudioObject.setGameAudio(gameAudio);
        try {
            JSONObject jsonObject=new JSONObject(JsonUtility.readJsonFromFile(assets,"audio.json"));
            String gameObject, type, sound;
            boolean loop;
            JSONArray jsonArray=jsonObject.getJSONArray("audio");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject=jsonArray.getJSONObject(i);
                gameObject=jsonObject.getString("gameobject");
                type=jsonObject.getString("type");
                sound=jsonObject.getString("file");
                if (type.equalsIgnoreCase("sound"))
                    GameAudio.AUDIOLIBRARY.put(GameObject.GameObjectType.valueOf(gameObject),gameAudio.addSound(sound));
                else {
                    loop=jsonObject.getBoolean("loop");
                    GameAudio.AUDIOLIBRARY.put(GameObject.GameObjectType.valueOf(gameObject), gameAudio.addMusic(sound,loop));
                }
            }
            GameAudio.AUDIOLIBRARY.get(GameObject.GameObjectType.MENU).play();
        } catch (JSONException e) {
            //Log.d("Debug", "Unable to create JsonOB for audio");
        }
    }
}
