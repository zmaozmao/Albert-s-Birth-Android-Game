package com.example.bizzi.GameSystem.GameObSubSystem;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.example.bizzi.GameSystem.AudioSubSystem.GameAudio;
import com.example.bizzi.GameSystem.GameWorld;
import com.example.bizzi.GameSystem.GraphicsSubSystem.GameGraphics;
import com.example.bizzi.GameSystem.GraphicsSubSystem.Spritesheet;
import com.example.bizzi.GameSystem.JLiquidFunUtility.WallJoint;
import com.example.bizzi.GameSystem.Utility.Builder;
import com.example.bizzi.GameSystem.Utility.JsonUtility;
import com.google.fpl.liquidfun.Body;
import com.google.fpl.liquidfun.BodyDef;
import com.google.fpl.liquidfun.BodyType;
import com.google.fpl.liquidfun.CircleShape;
import com.google.fpl.liquidfun.FixtureDef;
import com.google.fpl.liquidfun.PolygonShape;
import com.google.fpl.liquidfun.World;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import static com.example.bizzi.GameSystem.GameObSubSystem.GameObject.getGameOB;

public final class GameObBuilder implements Builder {

    private static final int MAXPOOLSIZE = 300;
    private static final String LEVELS = "levels/";
    private final Context context;
    private World world;
    private static final SparseArray<Long> TimeStamps = new SparseArray<>();
    private final static float THICKNESS = 1;
    private Body einstein;
    private JSONArray enemies;

    private String level;


    public GameObBuilder(Context context) {
        this.context = context;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public void build() {
    }

    public SparseArray<GameObject> buildMenu() {
        SparseArray<GameObject> array = new SparseArray<>();
        GameObject gameOB;
        Bitmap bitmap;
        DrawableComponent drawable;
        ControllableComponent controllable;

        //Create background_menu
        gameOB = getGameOB();
        gameOB.type = GameObject.GameObjectType.MENU;
        bitmap = GameGraphics.STATICSPRITE.get(gameOB.type);
        drawable = DrawableComponent.getDrawableComponent(gameOB, bitmap);
        drawable.x = GameWorld.BUFFERWIDTH / 2;
        drawable.y = GameWorld.BUFFERHEIGHT / 2;
        gameOB.components.put(drawable.getType(), drawable);

        array.append(array.size(), gameOB);

        //Create menuTitle
        gameOB = getGameOB();
        gameOB.type = GameObject.GameObjectType.MENUTITLE;
        bitmap = GameGraphics.STATICSPRITE.get(gameOB.type);
        drawable = DrawableComponent.getDrawableComponent(gameOB, bitmap);
        drawable.x = GameWorld.BUFFERWIDTH / 2;
        drawable.y = GameWorld.BUFFERHEIGHT / 2 + 40;
        ;
        gameOB.components.put(drawable.getType(), drawable);
        array.append(array.size(), gameOB);
        float previousY = drawable.y + bitmap.getHeight() / 2;

        //Create startButton
        gameOB = getGameOB();
        gameOB.type = GameObject.GameObjectType.STARTBUTTON;
        bitmap = GameGraphics.STATICSPRITE.get(gameOB.type);
        drawable = DrawableComponent.getDrawableComponent(gameOB, bitmap);
        drawable.x = GameWorld.BUFFERWIDTH / 2;
        drawable.y = (int) previousY + 100;
        gameOB.components.put(drawable.getType(), drawable);
        controllable = ControllableComponent.ControllableWidgetComponent.getControllableWidgetComponent(gameOB);
        gameOB.components.put(controllable.getType(), controllable);
        array.append(array.size(), gameOB);
        previousY = drawable.y;


        //Create quitButton
        gameOB = getGameOB();
        gameOB.type = GameObject.GameObjectType.QUITBUTTON;
        bitmap = GameGraphics.STATICSPRITE.get(gameOB.type);
        drawable = DrawableComponent.getDrawableComponent(gameOB, bitmap);
        drawable.x = GameWorld.BUFFERWIDTH / 2;
        drawable.y = (int) previousY + bitmap.getHeight() + 50;
        gameOB.components.put(drawable.getType(), drawable);
        controllable = ControllableComponent.ControllableWidgetComponent.getControllableWidgetComponent(gameOB);
        gameOB.components.put(controllable.getType(), controllable);
        array.append(array.size(), gameOB);

        //Create soundButton
        gameOB = getGameOB();
        gameOB.type = GameObject.GameObjectType.SOUNDBUTTON;
        Spritesheet spritesheet = GameGraphics.ANIMATEDSPRITE.get(gameOB.type);
        AnimatedComponent animated = AnimatedComponent.getAnimatedComponent(gameOB, spritesheet);
        animated.x = (int) 7.5f * GameWorld.BUFFERWIDTH / 8;
        animated.y = GameWorld.BUFFERHEIGHT / 10;
        if (GameAudio.SILENCE)
            animated.animation=2;
        gameOB.components.put(animated.getType(), animated);
        controllable = ControllableComponent.ControllableWidgetComponent.getControllableWidgetComponent(gameOB);
        gameOB.components.put(controllable.getType(), controllable);
        array.append(array.size(), gameOB);

        return array;
    }

    public SparseArray<GameObject> buildLevel(String level) {
        SparseArray<GameObject> array = new SparseArray<>();
        this.level=level;
        try {
            //Obtain level description
            JSONObject description = new JSONObject(JsonUtility.readJsonFromFile(context.getAssets(), LEVELS + level));
            //Background
            array.append(array.size(), buildBackgroundLevel());

            //Enclosure
            buildEnclosureLevel(array);

            //Building walls
            JSONObject slidingWalls = description.getJSONArray("walls").getJSONObject(0);
            int tot = slidingWalls.getInt("number");
            for (int i = 0; i < tot; i++)
                buildSlidingWall(slidingWalls, i + 1, tot, array);


            //Building Egg cell
            JSONObject cell = description.getJSONArray("eggcell").getJSONObject(0);
            array.append(array.size(), buildEggCell(cell));

            //Building enemies
            enemies = description.getJSONArray("enemies");
            for (int i = 0; i < enemies.length(); i++) {
                switch (enemies.getJSONObject(i).getString("type")) {
                    case "spermatozoon":
                        for (int z = 0; z < enemies.getJSONObject(i).getInt("number"); z++)
                            array.append(array.size(), buildEnemySpermatozoon(enemies.getJSONObject(i)));

                        TimeStamps.append(i, System.currentTimeMillis() / 1000);
                        break;
                    case "pill":
                        for (int z = 0; z < enemies.getJSONObject(i).getInt("number"); z++)
                            array.append(array.size(), buildEnemyPill(enemies.getJSONObject(i)));
                        TimeStamps.append(i, System.currentTimeMillis() / 1000);
                        break;
                }
            }

            //Building Einstein's spermatozoon

            JSONObject spermatozoon = description.getJSONArray("spermatozoon").getJSONObject(0);
            array.append(array.size(), buildSpermatozoon(spermatozoon));


        } catch (JSONException e) {
            Log.d("Debug", "Unable to create JsonOB for level: " + level);
            return null;
        }

        return array;
    }

    private void buildSlidingWall(JSONObject wall, int i, int tot, SparseArray<GameObject> array) {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.DOOR;

        //Set-Up Door DrawableComponent
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);

        // Set-Up Door Physic Component + wallBuild call
        Random random = new Random();
        int j = 2;
        int n = 5;
        try {
            j = wall.getInt("rmin");
            n = wall.getInt("rmax") - j;
        } catch (JSONException e) {
            Log.d("Debug", "Unable to get height SlidingWall");
        }
        float wallHeight = random.nextInt(n) + j;
        BodyDef bdef = new BodyDef();
        float cy = (PhysicComponent.YMIN + PhysicComponent.YMAX) / 2;
        float cx = PhysicComponent.XMIN + (PhysicComponent.PHYSICALWIDTH * i / (tot + 1));
        bdef.setPosition(cx, cy);
        //TODO Modificare in KinematicBody  e  usare o applyForce o setVelocity
        bdef.setType(BodyType.dynamicBody);
        Body body = world.createBody(bdef);
        body.setSleepingAllowed(false);
        body.setUserData(go);

        PolygonShape box = new PolygonShape();
        box.setAsBox(THICKNESS / 2, wallHeight / 2);
        FixtureDef fixturedef = new FixtureDef();
        fixturedef.setShape(box);
        fixturedef.setFriction(0.2f);       // default 0.2
        fixturedef.setRestitution(0.4f);    // default 0
        fixturedef.setDensity(0);     // default 0
        body.createFixture(fixturedef);

        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);
        PhysicComponent physicComponent = PhysicComponent.getPhysicComponent(go, body, THICKNESS, wallHeight);
        go.setComponent(physicComponent);

        // clean up native objects
        fixturedef.delete();
        bdef.delete();
        box.delete();
        GameObject sw = buildSudWall(cx, cy, wallHeight);
        GameObject nw = buildNordWall(cx, cy, wallHeight);
        WallJoint.buildPrismaticDoor(((PhysicComponent) go.getComponent(Component.ComponentType.PHYSIC)).getBody(),
                ((PhysicComponent) sw.getComponent(Component.ComponentType.PHYSIC)).getBody(),
                world, cx, cy, PhysicComponent.YMAX - THICKNESS - wallHeight / 2, wallHeight);
        array.append(array.size(), go);
        array.append(array.size(), nw);
        array.append(array.size(), sw);
    }


    private GameObject buildSudWall(float cx, float cy, float height) {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.WALL;

        float myHeight = PhysicComponent.YMAX - THICKNESS - height / 2;
        //Drawable Component
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);
        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);
        //Physic Component
        BodyDef bdef = new BodyDef();
        bdef.setPosition(cx, cy + height / 2 + myHeight / 2);
        Body body = world.createBody(bdef);
        body.setUserData(go);
        PolygonShape box = new PolygonShape();
        //box.setCentroid(x, y);
        box.setAsBox(THICKNESS / 2, myHeight / 2);
        body.createFixture(box, 0); // no density needed
        body.setSleepingAllowed(true);
        bdef.delete();
        box.delete();
        PhysicComponent physicComponent = PhysicComponent.getPhysicComponent(go, body, THICKNESS, myHeight);
        go.setComponent(physicComponent);
        return go;

    }

    private GameObject buildNordWall(float cx, float cy, float height) {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.WALL;

        float myHeight = -PhysicComponent.YMIN - THICKNESS - height / 2;
        //Drawable Component
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);
        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);
        //Physic Component
        BodyDef bdef = new BodyDef();
        bdef.setPosition(cx, cy - height / 2 - myHeight / 2);
        Body body = world.createBody(bdef);
        body.setUserData(go);
        PolygonShape box = new PolygonShape();
        //box.setCentroid(x, y);
        box.setAsBox(THICKNESS / 2, myHeight / 2);
        body.createFixture(box, 0); // no density needed
        body.setSleepingAllowed(true);
        bdef.delete();
        box.delete();
        PhysicComponent physicComponent = PhysicComponent.getPhysicComponent(go, body, THICKNESS, myHeight);
        go.setComponent(physicComponent);
        return go;

    }

    private GameObject buildEnemySpermatozoon(JSONObject spermatozoon) {

        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.SPERMATOZOON;
        //PhysicComponent
        float width = 1.5f, heigth = 0.5f, tFriction = 0.3f, tRestitution = 0.4f, tDensity = 0.05f, cFriction = 0.3f, cRestitution = 0.3f, cDensity = 0.4f, radius = width / 8;
        float cWidht, cHeight;
        PhysicComponent physicComponent;
        FixtureDef fixturedef = new FixtureDef(), fixtesta = new FixtureDef(), fixtesta2 = new FixtureDef();
        BodyDef bdef = new BodyDef();
        CircleShape testa = new CircleShape();
        CircleShape testa2 = new CircleShape();
        PolygonShape coda = new PolygonShape();

        try {
            width = (float) spermatozoon.getDouble("width");
            heigth = (float) spermatozoon.getDouble("height");
            tFriction = (float) spermatozoon.getDouble("tFriction");
            tRestitution = (float) spermatozoon.getDouble("tRestitution");
            tDensity = (float) spermatozoon.getDouble("tDensity");
            cFriction = (float) spermatozoon.getDouble("cFriction");
            cRestitution = (float) spermatozoon.getDouble("cRestitution");
            cDensity = (float) spermatozoon.getDouble("cDensity");
        } catch (JSONException e) {
            Log.d("Debug", "Unable to get width,heigth enemey spermatozoon");
        }



        bdef.setType(BodyType.dynamicBody);
        Random rand = new Random();
        //if Einstein == NULL=> Build level call  buildSpermatozoon
        // else  buildSpawner call it

        if(einstein!=null) {
            bdef.setPosition(einstein.getPositionX(),rand.nextInt(PhysicComponent.PHYSICALHEIGHT-2*(int)THICKNESS)+PhysicComponent.YMIN+THICKNESS);
            //bdef.setPosition(einstein.getPositionX(), 3/5*(PhysicComponent.PHYSICALHEIGHT));
        }
        else {
            bdef.setPosition(rand.nextInt(PhysicComponent.PHYSICALWIDTH/2-(int)THICKNESS) +PhysicComponent.XMIN+THICKNESS,rand.nextInt(PhysicComponent.PHYSICALHEIGHT-2*(int)THICKNESS) +PhysicComponent.YMIN+THICKNESS);
        }

        Body body = world.createBody(bdef);
        body.setSleepingAllowed(false);
        body.setUserData(go);
        radius = width / 12;
        testa.setRadius(radius);
        testa2.setRadius(radius);
        cWidht = 2 * width / 4;
        cHeight = radius / 50;

        coda.setAsBox(cWidht, cHeight);
        testa.setPosition(radius, 0f);
        testa2.setPosition(2 * radius, 0);
        coda.setCentroid(-cWidht, 0f);

        //Setup Teste
        //1
        fixtesta.setShape(testa);
        fixtesta.setFriction(tFriction);
        fixtesta.setRestitution(tRestitution);
        fixtesta.setDensity(tDensity);
        //2
        fixtesta2.setShape(testa2);
        fixtesta2.setFriction(tFriction);
        fixtesta2.setRestitution(tRestitution);
        fixtesta2.setDensity(tDensity);


        //Setup Coda
        fixturedef.setShape(coda);
        fixturedef.setFriction(cFriction);       // default 0.1
        fixturedef.setRestitution(cRestitution);    // default 0
        fixturedef.setDensity(cDensity);
        body.createFixture(fixtesta);
        body.createFixture(fixturedef);
        body.createFixture(fixtesta2);

        // This velocity let Einstein to move faster
        body.setLinearDamping(0.7f);

        //Clean up
        fixtesta.delete();
        fixturedef.delete();
        fixtesta2.delete();
        bdef.delete();
        coda.delete();
        testa.delete();
        testa2.delete();


        //DrawableComponent
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);
        drawableComponent = DrawableComponent.getDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);
        physicComponent = PhysicComponent.getPhysicComponent(go, body, width, heigth);
        go.setComponent(physicComponent);


        return go;
    }

    private GameObject buildEggCell(JSONObject EggCell) {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.EGGCELL;

        //DrawableComponent
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);
        drawableComponent = DrawableComponent.getDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);

        //Physic Component
        BodyDef bdef = new BodyDef();
        FixtureDef ovulDef = new FixtureDef();
        CircleShape ovulo = new CircleShape(), sx = new CircleShape();

        float width = 0.8f, height = 0.6f, friction = 0.1f, density = 0.4f, restitution = 0.3f;


        try {
            width = (float) EggCell.getDouble("width");

        } catch (JSONException e) {
            Log.d("Debug", "Unable to get Egg Cell Radius");
        }
        bdef.setPosition(PhysicComponent.XMIN + 7 * PhysicComponent.PHYSICALWIDTH / 8, (PhysicComponent.YMAX + PhysicComponent.YMIN) / 2);
        bdef.setType(BodyType.staticBody);
        Body body = world.createBody(bdef);
        body.setSleepingAllowed(false);
        body.setUserData(go);
        ovulo.setRadius(width / 2);
        ovulDef.setShape(ovulo);
        body.createFixture(ovulDef);

        // clean up native objects
        ovulDef.delete();
        ovulo.delete();
        bdef.delete();
        PhysicComponent physicComponent = PhysicComponent.getPhysicComponent(go, body, width, width);
        go.setComponent(physicComponent);

        return go;
    }

    private GameObject buildSpermatozoon(JSONObject spermatozoon) {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.EINSTEIN;
        //PhysicComponent
        float width = 1.5f, heigth = 0.5f, tFriction = 0.3f, tRestitution = 0.4f, tDensity = 0.05f, cFriction = 0.3f, cRestitution = 0.3f, cDensity = 0.4f, radius = width / 8;
        float cWidht, cHeight;
        PhysicComponent physicComponent;
        FixtureDef fixturedef = new FixtureDef(), fixtesta = new FixtureDef(), fixtesta2 = new FixtureDef();
        BodyDef bdef = new BodyDef();
        CircleShape testa = new CircleShape();
        CircleShape testa2 = new CircleShape();
        PolygonShape coda = new PolygonShape();


        try {
            width = (float) spermatozoon.getDouble("width");
            heigth = (float) spermatozoon.getDouble("height");
            tFriction = (float) spermatozoon.getDouble("tFriction");
            tRestitution = (float) spermatozoon.getDouble("tRestitution");
            tDensity = (float) spermatozoon.getDouble("tDensity");
            cFriction = (float) spermatozoon.getDouble("cFriction");
            cRestitution = (float) spermatozoon.getDouble("cRestitution");
            cDensity = (float) spermatozoon.getDouble("cDensity");
        } catch (JSONException e) {
            Log.d("Debug", "Unable to get width,heigth enemey spermatozoon");
        }

        if(einstein!=null)
            bdef.setPosition(einstein.getPositionX(), 4*PhysicComponent.PHYSICALHEIGHT/5);
        bdef.setType(BodyType.dynamicBody);
        bdef.setPosition(PhysicComponent.XMIN+THICKNESS*4,PhysicComponent.YMAX-THICKNESS*2);
        Body body = world.createBody(bdef);
        body.setSleepingAllowed(false);
        body.setUserData(go);
        radius = width / 6;
        testa.setRadius(radius);
        testa2.setRadius(radius);
        cWidht = 2 * width / 4;
        cHeight = radius / 50;

        coda.setAsBox(cWidht, cHeight);
        testa.setPosition(radius, 0f);
        testa2.setPosition(2 * radius, 0);
        coda.setCentroid(-cWidht, 0f);

        //Setup Teste
        //1
        fixtesta.setShape(testa);
        fixtesta.setFriction(tFriction);
        fixtesta.setRestitution(tRestitution);
        fixtesta.setDensity(tDensity);
        //2
        fixtesta2.setShape(testa2);
        fixtesta2.setFriction(tFriction);
        fixtesta2.setRestitution(tRestitution);
        fixtesta2.setDensity(tDensity);


        //Setup Coda
        fixturedef.setShape(coda);
        fixturedef.setFriction(cFriction);       // default 0.1
        fixturedef.setRestitution(cRestitution);    // default 0
        fixturedef.setDensity(cDensity);
        body.createFixture(fixtesta);
        body.createFixture(fixturedef);
        body.createFixture(fixtesta2);
        einstein=body;
        //Clean up
        fixtesta.delete();
        fixturedef.delete();
        fixtesta2.delete();
        bdef.delete();
        coda.delete();
        testa.delete();
        testa2.delete();


        //DrawableComponent
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);
        drawableComponent = DrawableComponent.getDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);
        physicComponent = PhysicComponent.getPhysicComponent(go, body, width, heigth);
        go.setComponent(physicComponent);
        return go;

    }

    private GameObject buildEnemyPill(JSONObject pill) {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.PILL;

        //DrawableComponent
        DrawableComponent drawableComponent;
        Bitmap bitmap;
        bitmap = GameGraphics.STATICSPRITE.get(go.type);
        drawableComponent = DrawableComponent.getDrawableComponent(go, bitmap);
        go.setComponent(drawableComponent);

        //Physic Component
        BodyDef bdef = new BodyDef();
        PolygonShape pillola = new PolygonShape();
        FixtureDef fixturedef = new FixtureDef(), fixdx = new FixtureDef(), fixsx = new FixtureDef();
        CircleShape dx = new CircleShape(), sx = new CircleShape();

        float width = 0.8f, height = 0.6f, friction = 0.1f, density = 0.4f, restitution = 0.3f;


        try {
            width = (float) pill.getDouble("width");
            height = (float) pill.getDouble("height");
            friction = (float) pill.getDouble("friction");
            density = (float) pill.getDouble("density");
            restitution = (float) pill.getDouble("restitution");
        } catch (JSONException e) {
            Log.d("Debug", "Unable to get width,heigth ecc.. enemey pill");
        }

        Random rand = new Random();
        if(einstein!=null) {
            bdef.setPosition(einstein.getPositionX(),rand.nextInt(PhysicComponent.PHYSICALHEIGHT-2*(int)THICKNESS) +PhysicComponent.YMIN+THICKNESS);            //bdef.setPosition(einstein.getPositionX(), 3/5*(PhysicComponent.PHYSICALHEIGHT));
        }
        else {
            bdef.setPosition(rand.nextInt(PhysicComponent.PHYSICALWIDTH/2-(int)THICKNESS) +PhysicComponent.XMIN+THICKNESS,rand.nextInt(PhysicComponent.PHYSICALHEIGHT-2*(int)THICKNESS) +PhysicComponent.YMIN+THICKNESS);

        }
        bdef.setType(BodyType.dynamicBody);
        Body body = world.createBody(bdef);
        body.setSleepingAllowed(false);
        body.setUserData(go);
        //Rettangolo rappresentante corpo della pillola
        pillola.setAsBox(width / 3, height / 6);

        // Due circonferenze rappresentanti i  poli della pillola
        dx.setRadius(height / 7);
        sx.setRadius(height / 7);
        dx.setPosition(width / 3, 0);
        sx.setPosition(-width / 3, 0);

        fixdx.setShape(dx);
        fixsx.setShape(sx);

        //Settaggi pillola
        fixturedef.setShape(pillola);
        fixturedef.setFriction(friction);
        fixturedef.setRestitution(restitution);
        fixturedef.setDensity(density);


        body.createFixture(fixturedef);
        body.createFixture(fixdx);
        body.createFixture(fixsx);

        // This velocity let Einstein to move faster
        body.setLinearDamping(0.7f);


        // clean up native objects
        fixturedef.delete();
        fixdx.delete();
        fixsx.delete();
        bdef.delete();
        pillola.delete();
        dx.delete();
        sx.delete();

        PhysicComponent physicComponent = PhysicComponent.getPhysicComponent(go, body, width, height);
        go.setComponent(physicComponent);
        return go;
    }

    private GameObject buildBackgroundLevel() {
        GameObject go = getGameOB();
        go.type = GameObject.GameObjectType.BACKGROUND;
        DrawableComponent drawableComponent;
        drawableComponent = DrawableComponent.getDrawableComponent(go, GameGraphics.STATICSPRITE.get(go.type));
        drawableComponent.x = GameWorld.BUFFERWIDTH / 2;
        drawableComponent.y = GameWorld.BUFFERHEIGHT / 2;
        go.setComponent(drawableComponent);
        return go;
    }

    private void buildEnclosureLevel(SparseArray<GameObject> array) {

        //Bottom enclosure
        GameObject go;
        DrawableComponent drawableComponent;
        PhysicComponent physicComponent;
        BodyDef bdef;
        PolygonShape box;

        go = GameObject.getGameOB();
        bdef = new BodyDef();
        bdef.setType(BodyType.staticBody);
        bdef.setPosition((PhysicComponent.XMIN + PhysicComponent.XMAX) / 2, PhysicComponent.YMAX - THICKNESS / 2);
        Body body = world.createBody(bdef);
        body.setUserData(go);
        box = new PolygonShape();
        box.setAsBox(PhysicComponent.PHYSICALWIDTH / 2, THICKNESS / 2);
        body.createFixture(box, 0);
        bdef.delete();
        box.delete();

        go.type = GameObject.GameObjectType.WALL;
        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, GameGraphics.STATICSPRITE.get(go.type));
        go.setComponent(drawableComponent);

        physicComponent = PhysicComponent.getPhysicComponent(go, body, PhysicComponent.PHYSICALWIDTH, THICKNESS);
        go.setComponent(physicComponent);
        array.append(array.size(), go);

        //Top Enclosure
        go = GameObject.getGameOB();
        bdef = new BodyDef();
        bdef.setType(BodyType.staticBody);
        bdef.setPosition((PhysicComponent.XMIN + PhysicComponent.XMAX) / 2, PhysicComponent.YMIN + THICKNESS / 2);
        body = world.createBody(bdef);
        body.setUserData(go);
        box = new PolygonShape();
        box.setAsBox(PhysicComponent.PHYSICALWIDTH / 2, THICKNESS / 2);
        body.createFixture(box, 0);
        bdef.delete();
        box.delete();

        go.type = GameObject.GameObjectType.WALL;
        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, GameGraphics.STATICSPRITE.get(go.type));
        go.setComponent(drawableComponent);

        physicComponent = PhysicComponent.getPhysicComponent(go, body, PhysicComponent.PHYSICALWIDTH, THICKNESS);
        go.setComponent(physicComponent);
        array.append(array.size(), go);

        //Left Enclosure
        go = GameObject.getGameOB();
        bdef = new BodyDef();
        bdef.setType(BodyType.staticBody);
        bdef.setPosition(PhysicComponent.XMIN + THICKNESS / 2, (PhysicComponent.YMIN + PhysicComponent.YMAX) / 2);
        body = world.createBody(bdef);
        body.setUserData(go);
        box = new PolygonShape();
        box.setAsBox(THICKNESS / 2, PhysicComponent.PHYSICALHEIGHT / 2);
        body.createFixture(box, 0);
        bdef.delete();
        box.delete();

        go.type = GameObject.GameObjectType.WALL;
        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, GameGraphics.STATICSPRITE.get(go.type));
        go.setComponent(drawableComponent);

        physicComponent = PhysicComponent.getPhysicComponent(go, body, THICKNESS, PhysicComponent.PHYSICALHEIGHT);
        go.setComponent(physicComponent);
        array.append(array.size(), go);


        //Right Enclosure
        go = GameObject.getGameOB();
        bdef = new BodyDef();
        bdef.setType(BodyType.staticBody);
        bdef.setPosition(PhysicComponent.XMAX - THICKNESS / 2, (PhysicComponent.YMIN + PhysicComponent.YMAX) / 2);
        body = world.createBody(bdef);
        body.setUserData(go);
        box = new PolygonShape();
        box.setAsBox(THICKNESS / 2, PhysicComponent.PHYSICALHEIGHT / 2);
        body.createFixture(box, 0);
        body.setSleepingAllowed(true);
        bdef.delete();
        box.delete();

        go.type = GameObject.GameObjectType.WALL;
        drawableComponent = DrawableComponent.PaintDrawableComponent.getPaintDrawableComponent(go, GameGraphics.STATICSPRITE.get(go.type));
        go.setComponent(drawableComponent);

        physicComponent = PhysicComponent.getPhysicComponent(go, body, THICKNESS, PhysicComponent.PHYSICALHEIGHT);
        go.setComponent(physicComponent);
        array.append(array.size(), go);
    }

    public void buildSpawner(SparseArray<GameObject> array) {
        if (array.size() < 35) {
            try {
                for (int i = 0; i < enemies.length(); i++) {
                    Long last = this.TimeStamps.get(i);
                    Long now = System.currentTimeMillis() / 1000;
                    switch (enemies.getJSONObject(i).getString("type")) {
                        case "spermatozoon":
                            if (now - last >= enemies.getJSONObject(i).getLong("rate")) {
                                for (int z = 0; z < enemies.getJSONObject(i).getInt("numberS"); z++)
                                    array.append(array.size(), buildEnemySpermatozoon(enemies.getJSONObject(i)));
                                TimeStamps.append(i, now);
                            }
                            break;
                        case "pill":
                            if (now - last >= enemies.getJSONObject(i).getLong("rate")) {
                                for (int z = 0; z < enemies.getJSONObject(i).getInt("numberS"); z++)
                                    array.append(array.size(), buildEnemyPill(enemies.getJSONObject(i)));
                                TimeStamps.append(i, now);
                            }
                            break;
                    }
                }
            } catch (JSONException e) {
                Log.d("Debug", "Unable to create JsonOB for level: " + level);
            }
        }
    }
    public SparseArray<GameObject> buildFinish(GameObject.GameObjectType type){

        SparseArray<GameObject> array=new SparseArray<>(2);

        //Create lastBackgorund
        GameObject gameOB = getGameOB();
        gameOB.type = type;
        DrawableComponent drawableComponent = DrawableComponent.getDrawableComponent(gameOB, GameGraphics.STATICSPRITE.get(gameOB.type));
        drawableComponent.x = GameWorld.BUFFERWIDTH / 2;
        drawableComponent.y = GameWorld.BUFFERHEIGHT / 2;
        gameOB.setComponent(drawableComponent);
        array.append(array.size(),gameOB);

        //Create backHomeButton
        gameOB = getGameOB();
        gameOB.type = GameObject.GameObjectType.HOMEBUTTON;
        drawableComponent = DrawableComponent.getDrawableComponent(gameOB, GameGraphics.STATICSPRITE.get(gameOB.type));
        drawableComponent.x = GameWorld.BUFFERWIDTH / 2;
        drawableComponent.y = GameWorld.BUFFERHEIGHT/2+400;
        gameOB.components.put(drawableComponent.getType(), drawableComponent);
        ControllableComponent controllable = ControllableComponent.ControllableWidgetComponent.getControllableWidgetComponent(gameOB);
        gameOB.components.put(controllable.getType(), controllable);
        array.append(array.size(),gameOB);
        return array;
    }

}