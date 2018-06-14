package com.example.bizzi.GameSystem.GameObSubSystem;

import android.support.v4.util.Pools;

import com.example.bizzi.GameSystem.GraphicsSubSystem.GameGraphics;
import com.example.bizzi.GameSystem.Utility.Recyclable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class GameObject implements Recyclable {

    public enum GameObjectType{MENU, EINSTEIN, STARTBUTTON, QUITBUTTON, MENUTITLE,SOUNDBUTTON, ENCLOSURE, SPERMATOZOON,BACKGROUND, DOOR,PILL,WALL, EGGCELL}

    private static int count=0;

    public int id;

    final Map<Component.ComponentType,Component> components;

    private static final Pools.Pool<GameObject> POOL=new Pools.SimplePool<>(200);

    GameObjectType type;

    static GameObject getGameOB() {
        GameObject object = POOL.acquire();
        if (object == null)
            object = new GameObject();
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameObject that = (GameObject) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Component getComponent (Component.ComponentType type){
       return components.get(type);
    }

    void setComponent (Component component){
        components.put(component.getType(),component);
    }

    private GameObject(){
        id=count++;
        components= new EnumMap<>(Component.ComponentType.class);
    }

    public GameObjectType getType() {
        return type;
    }

    public void recycle(){
        for (Component component : components.values())
            component.recycle();
        components.clear();
        POOL.release(this);
    }

    public static byte[] serializeGameObject(GameObject go, boolean complete){
        byte[] array, rotation=new byte[2];
        int x,y,semiWidth,semiHeight;

        //Distinguish between type

        DrawableComponent drawableComponent=(DrawableComponent) go.getComponent(Component.ComponentType.DRAWABLE);
        AnimatedComponent animatedComponent= (AnimatedComponent)go.getComponent(Component.ComponentType.ANIMATED);

        if (drawableComponent!=null) {
            x = drawableComponent.x;
            y = drawableComponent.y;
            if (drawableComponent.rotation<=90) {
                rotation[0] = 0;
                rotation[1]=(byte) drawableComponent.rotation;
            }
            else if (drawableComponent.rotation>90 && drawableComponent.rotation<=180){
                rotation[0] = 1;
                rotation[1]=(byte) (drawableComponent.rotation - 90);
            }
            else if (drawableComponent.rotation>180 && drawableComponent.rotation<=270){
                rotation[0] = 2;
                rotation[1]=(byte) (drawableComponent.rotation - 180);
            }
            else if (drawableComponent.rotation>270 && drawableComponent.rotation<=360){
                rotation[0] = 3;
                rotation[1]=(byte) (drawableComponent.rotation - 180);
            }
            semiWidth=drawableComponent.semiWidth;
            semiHeight=drawableComponent.semiHeight;
        }
        else {
            x = animatedComponent.x;
            y=animatedComponent.y;
            rotation[0]=5;
            rotation[1]=(byte)animatedComponent.animation;
            semiWidth=animatedComponent.semiWidth;
            semiHeight=animatedComponent.semiHeight;
        }

        //Allocate byte array and fill last fields when complete is true
        if (complete) {
            array = new byte[20];
            ByteBuffer.wrap(array,12,4).order(ByteOrder.LITTLE_ENDIAN).putInt(semiWidth);
            ByteBuffer.wrap(array,16,4).order(ByteOrder.LITTLE_ENDIAN).putInt(semiHeight);
        }
        else
            array=new byte[12];


        array[0]=(byte)go.id;
        array[1]=(byte)go.type.ordinal();
        array[2]=rotation[0];
        array[3]=rotation[1];
        ByteBuffer.wrap(array,4,4).order(ByteOrder.LITTLE_ENDIAN).putInt(x);
        ByteBuffer.wrap(array,8,4).order(ByteOrder.LITTLE_ENDIAN).putInt(y);

        return array;
    }

    public static GameObject deSerializeGameObject(byte[] array, int offset, int length){
        int start=offset*length,
                x,y,rotation,semiWidth=0, semiHeight=0;
        GameObject go=getGameOB();

        //Force to use same id on server ;)
        go.id=array[start];
        go.type=GameObjectType.values()[array[start+1]];

        x= ByteBuffer.wrap(array,start+4,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        y= ByteBuffer.wrap(array,start+8,4).order(ByteOrder.LITTLE_ENDIAN).getInt();

        if (length==20){
            semiWidth=ByteBuffer.wrap(array,start+12,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            semiHeight=ByteBuffer.wrap(array,start+16,4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }

        if (array[start+2]<5){
            DrawableComponent drawableComponent=DrawableComponent.getDrawableComponent(go, GameGraphics.STATICSPRITE.get(go.type));
            drawableComponent.x=x;
            drawableComponent.y=y;
            drawableComponent.rotation=array[start+2]*90 + array[start+3];
            if (length==20){
                drawableComponent.semiWidth=semiWidth;
                drawableComponent.semiHeight=semiHeight;
            }
            go.setComponent(drawableComponent);
        }
        else {
            AnimatedComponent animatedComponent=AnimatedComponent.getAnimatedComponent(go,GameGraphics.ANIMATEDSPRITE.get(go.type));
            animatedComponent.x=x;
            animatedComponent.y=y;
            animatedComponent.animation=array[start+3];
            if (length==20){
                animatedComponent.semiWidth=semiWidth;
                animatedComponent.semiHeight=semiHeight;
            }
            go.setComponent(animatedComponent);
        }
        return go;
    }
}
