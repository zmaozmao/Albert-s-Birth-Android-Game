package com.example.bizzi.GameSystem;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v4.util.ArraySet;
import android.util.SparseArray;

import com.example.bizzi.GameSystem.AudioSubSystem.GameAudio;
import com.example.bizzi.GameSystem.GameObSubSystem.AnimatedComponent;
import com.example.bizzi.GameSystem.GameObSubSystem.Component;
import com.example.bizzi.GameSystem.GameObSubSystem.ControllableComponent;
import com.example.bizzi.GameSystem.GameObSubSystem.DrawableComponent;
import com.example.bizzi.GameSystem.GameObSubSystem.GameObBuilder;
import com.example.bizzi.GameSystem.GameObSubSystem.GameObject;
import com.example.bizzi.GameSystem.GameObSubSystem.PhysicComponent;
import com.example.bizzi.GameSystem.InputSubSystem.GameInput;
import com.example.bizzi.GameSystem.InputSubSystem.InputObject;
import com.example.bizzi.GameSystem.NetworkingSubSystem.GameNetworking;
import com.google.fpl.liquidfun.World;

public final class GameWorld {

    //Rendering variables
    public static final int BUFFERWIDTH = 1920,
            BUFFERHEIGHT = 1080,
            VELOCITYITERATION=8,
            POSITIONITERATION=3,
            PARTICLEITERATION=3;
    private static final float TIMESTEP = 1 / 50f; //60FPS

    public static boolean home;
    public final Bitmap frameBuffer;
    private final Canvas canvas;
    private final World world;
    //GameObjects
    private final ArraySet<GameObject> gameObjects = new ArraySet<>();


    //Audio SubSystem ref
    private final GameAudio gameAudio;

    //Input SubSystem ref
    private final GameInput gameInput;

    //GameObBuilder
    private final GameObBuilder gameObFactory;

    //GameNetwork
    private final GameNetworking gameNetworking;

    private SparseArray<GameObject> mainMenu;
    private SparseArray<GameObject> level;
    private SparseArray<GameObject> toBeRendered;


    GameWorld(World world, GameAudio gameAudio, GameInput gameInput, GameObBuilder gameObFactory, GameNetworking gameNetworking) {
        this.gameObFactory = gameObFactory;
        this.gameAudio = gameAudio;
        this.gameInput = gameInput;
        this.world = world;
        this.gameNetworking=gameNetworking;
        home = true;
        frameBuffer = Bitmap.createBitmap(BUFFERWIDTH, BUFFERHEIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(frameBuffer);
    }

    public void updateWorld() {
        String Level = casualLevel();
        InputObject.AccelerometerObject accelerometer = gameInput.getAccelerometerEvent();
        if (home) {
            if (level != null) {
                for (int i = 0; i < level.size(); i++)
                    level.get(i).recycle();
                level = null;
            }
            if (mainMenu == null) {
                mainMenu = gameObFactory.buildMenu();
                toBeRendered = mainMenu;
            }
        } else {
            if (mainMenu != null) {
                for (int i = 0; i < mainMenu.size(); i++)
                    mainMenu.get(i).recycle();
                mainMenu = null;
            }
            if (level == null) {
                level = gameObFactory.buildLevel(Level);
                toBeRendered=level;
            }
            else {
                gameObFactory.buildSpawner(toBeRendered,casualLevel());
            }



            world.setGravity(accelerometer.y,accelerometer.x);
            world.step(TIMESTEP,VELOCITYITERATION,POSITIONITERATION,PARTICLEITERATION);
        }

        //Parse all touch events
        SparseArray<InputObject.TouchObject> touches = gameInput.getTouchEvents();
        for (int i = 0; i < touches.size(); i++) {
            InputObject.TouchObject touch = touches.get(i);
            if (touch != null) {
                for (int j = 0; j < toBeRendered.size(); j++) {
                    ControllableComponent controllableComponent = (ControllableComponent) toBeRendered.get(j).getComponent(Component.ComponentType.CONTROLLABLE);
                    if (controllableComponent != null)
                        controllableComponent.notifyTouch(touch);
                }
            }
            touch.recycle();
        }

        //Parse last accelerometer event
        if (accelerometer != null) {
            for (int j = 0; j < toBeRendered.size(); j++) {
                ControllableComponent controllableComponent = (ControllableComponent) toBeRendered.get(j).getComponent(Component.ComponentType.CONTROLLABLE);
                if (controllableComponent != null)
                    controllableComponent.notifyAccelerometer(accelerometer);
            }
        }
        for (int j = 0; j < toBeRendered.size(); j++) {
            PhysicComponent physicComponent=(PhysicComponent)toBeRendered.get(j).getComponent(Component.ComponentType.PHYSIC);
            if (physicComponent!=null)
                physicComponent.updatePosition();
        }

        gameAudio.checkAudio();
    }

    public void renderWorld() {
        canvas.drawARGB(255, 0, 0, 0);
        GameObject gameObject;

        for (int i = 0; i < toBeRendered.size(); i++) {
            gameObject = toBeRendered.get(i);
            DrawableComponent drawableComponent = (DrawableComponent) gameObject.getComponent(Component.ComponentType.DRAWABLE);
            if (drawableComponent != null)
                drawableComponent.draw(canvas);

            AnimatedComponent animatedComponent = (AnimatedComponent) gameObject.getComponent(Component.ComponentType.ANIMATED);
            if (animatedComponent != null)
                animatedComponent.draw(canvas);
        }


    }

    public void pause() {
        gameAudio.pauseAudio();
    }

    private String casualLevel() {
        //TODO maybe one day...
        return "lv1.json";
    }
}
