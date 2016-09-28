package com.coe.c0r0vans.Singles;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.coe.c0r0vans.GameObject.ActiveObject;
import com.coe.c0r0vans.GameObject.GameObject;
import com.coe.c0r0vans.Logic.Ambush;
import com.coe.c0r0vans.Logic.Caravan;
import com.coe.c0r0vans.Logic.Chest;
import com.coe.c0r0vans.Logic.City;
import com.coe.c0r0vans.Logic.Player;
import com.coe.c0r0vans.UIElements.ChooseFaction;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import utility.GATracker;
import utility.GPSInfo;
import utility.internet.ServerListener;
import utility.internet.serverConnect;
import utility.notification.Essages;
import utility.settings.GameSettings;
import utility.settings.SettingsListener;


/**
 * Перечень игровых объектов на карте
 */
public class GameObjects{
    private static HashMap<String,GameObject> objects;
    private static Player player;
    private static GoogleMap map;

    public static ActiveObject getClosestObject(LatLng latLng){
        float closest=1000;
        ActiveObject closestObject=null;

        for (GameObject o:activeObjects.values()){
            if (o instanceof ActiveObject && o.getMarker()!=null && o.getMarker().isVisible()) {

                if (o.getPosition()!= null) {

                    float dist = GPSInfo.getDistance(latLng, o.getPosition());
                    if (o instanceof Chest && dist < ((ActiveObject) o).getRadius()) {
                        closestObject = (ActiveObject) o;
                        break;
                    } else if (dist < closest && dist < ((ActiveObject) o).getRadius()) {
                        closest = dist;
                        closestObject = (ActiveObject) o;
                        if (o instanceof Chest) break;
                    }
                }
            }
        }

        return closestObject;
    }

    public static void init(final Context context){
        player=new Player();
        objects=new HashMap<>();
        activeObjects=new HashMap<>();
        SharedPreferences sp = context.getSharedPreferences("player", Context.MODE_PRIVATE);
        String pls = sp.getString("player", "");
        if (!"".equals(pls)) {
            try {
                GameObjects.getPlayer().loadJSON(new JSONObject(pls));
            } catch (JSONException e) {
                GATracker.trackException("LoadPlayer",e);
            }
        }

        serverConnect.getInstance().addListener(new ServerListener() {
            @Override
            public void onResponse(int TYPE, JSONObject response) {
                if (TYPE==REFRESH){
                    try {
                        //Проверить наличие массива JSON. Objects
                        if (response.has("Objects")) {
                            //Скопировать данные в массив для удаления
                            GATracker.trackTimeStart("DonwloadData","ReadyMassive");
                            ArrayList<GameObject> remObjects = new ArrayList<>(objects.values());
                            JSONArray JObj = response.getJSONArray("Objects");
                            int leng = JObj.length();

                            GATracker.trackTimeEnd("DonwloadData","ReadyMassive");
                            for (int i = 0; i < leng; i++) {

                                GameObject robj;
                                String GUID=JObj.getJSONObject(i).getString("GUID");
                                robj=objects.get(GUID);
                                if (robj==null) robj=player.getAmbushes().get(GUID);
                                if (robj==null) robj=player.getRoutes().get(GUID);
                                if (robj != null) {

                                    remObjects.remove(robj);
                                    robj.loadJSON(JObj.getJSONObject(i));

                                } else {
                                    if (JObj.getJSONObject(i).getString("Type").equalsIgnoreCase("Player")) {

                                        GameObjects.getPlayer().loadJSON(JObj.getJSONObject(i));


                                    } else if (JObj.getJSONObject(i).getString("Type").equalsIgnoreCase("City")) {
                                        put(new City(MyGoogleMap.getMap(), JObj.getJSONObject(i)));


                                    } else if (JObj.getJSONObject(i).getString("Type").equalsIgnoreCase("Ambush")) {

                                        Ambush ambush = new Ambush(MyGoogleMap.getMap(), JObj.getJSONObject(i));
                                        /*if (ambush.isOwner()) player.getAmbushes().put(ambush.getGUID(),ambush);
                                        else objects.put(ambush.getGUID(), ambush);*/
                                        put(ambush);

                                    } else if (JObj.getJSONObject(i).getString("Type").equalsIgnoreCase("Caravan")) {

                                        Caravan caravan = new Caravan(MyGoogleMap.getMap(), JObj.getJSONObject(i));
                                        /*if (caravan.isOwner()) player.getRoutes().put(caravan.getGUID(), caravan);
                                        else objects.put(caravan.getGUID(), caravan);*/
                                        put(caravan);

                                    }
                                }

                            }
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTime(new Date());
                            calendar.add(Calendar.MINUTE,-20);
                            for (GameObject obj : remObjects) {
                                //Если город то не удаляем в течении 20 минут.
                                if (!(obj instanceof City &&  ((City) obj).getUpdated().after(calendar.getTime())) && !(obj instanceof Chest)) {
                                    obj.RemoveObject();
                                    removeActive(obj);
                                    objects.remove(obj.getGUID());
                                }
                            }
                        }

                    } catch (JSONException e) {
                       GATracker.trackException("ScanRange",e);
                    }
                } else
                if (TYPE==FASTSCAN){
                    try {
                        //Проверить наличие массива JSON. Objects

                        if (response.has("FastScan")) {
                            //Для каждого объекта в GameObjects
                            JSONArray lst=response.getJSONArray("FastScan");
                            for (GameObject o:objects.values()) {
                               //Если это Засада или Караван
                                if (o instanceof Ambush || o instanceof Caravan || o instanceof Chest) {

                                    //Если он есть в JSON обновить данные
                                    final int lst_length = lst.length();// Moved  lst.length() call out of the loop to local variable lst_length
                                    boolean isChanged=false;
                                    for (int i = 0; i< lst_length; i++){
                                        JSONObject obj=lst.getJSONObject(i);
                                        String guid="";
                                        int lat=0;
                                        int lng=0;
                                        String type="";
                                        if (obj.has("GUID"))guid=obj.getString("GUID");
                                        if (obj.has("Lat"))lat=obj.getInt("Lat");
                                        if (obj.has("Lng"))lng=obj.getInt("Lng");
                                        //if (obj.has("Type")) type=obj.getString("Type");
                                        if (o.getGUID().equals(guid)){
                                            o.setPostion(new LatLng(lat/1e6,lng/1e6));
                                            o.setVisibility(true);
                                            isChanged=true;
                                        }

                                    }
                                    //Пока не очищать типа запомнил ?
                                    try {
                                        if (!isChanged && ((o instanceof Ambush && ((Ambush) o).getFaction() != 0) || (o instanceof Caravan && ((Caravan) o).getFaction() != 0))) {
                                            //TODO Если нет очистить данные.
                                            o.setVisibility(false);
                                            //o.RemoveObject();
                                            //objects.remove(o.getGUID());
                                            //activeObjects.remove(o.getGUID());
                                        } else if (!isChanged && o instanceof Chest) {
                                            o.RemoveObject();
                                            objects.remove(o.getGUID());
                                            activeObjects.remove(o.getGUID());
                                        }
                                    } catch (Exception e){
                                        GATracker.trackException("FastScan",e);
                                    }
                                }
                            }
                            final int lst_length = lst.length();// Moved  lst.length() call out of the loop to local variable lst_length
                            for (int i = 0; i< lst_length; i++){
                                JSONObject obj=lst.getJSONObject(i);
                                String guid="";
                                int lat=0;
                                int lng=0;
                                String type="";
                                if (obj.has("GUID"))guid=obj.getString("GUID");
                                if (obj.has("Lat"))lat=obj.getInt("Lat");
                                if (obj.has("Lng"))lng=obj.getInt("Lng");
                                if (obj.has("Type")) type=obj.getString("Type");
                                if ("Caravan".equals(type)) {
                                    Caravan c = player.getRoutes().get(guid);
                                    if (c != null) c.setPostion(new LatLng(lat / 1e6, lng / 1e6));
                                } else if ("Chest".equalsIgnoreCase(type)){
                                    if (objects.get(guid)==null){
                                        put(new Chest(MyGoogleMap.getMap(),obj));
                                    }
                                }

                            }


                        }

                    } catch (JSONException e) {
                        GATracker.trackException("FastScan",e);
                    }
                } else if (TYPE==PLAYER){
                    GameObjects.getPlayer().loadJSON(response);
                    if (GameObjects.getPlayer().getRace() < 1 || GameObjects.getPlayer().getRace() > 3) {
                        MessageMap.clearAll();
                        Essages.clear();
                        new ChooseFaction(context).show();
                    }
                    SharedPreferences sp = context.getSharedPreferences("player", Context.MODE_PRIVATE);
                    SharedPreferences.Editor ed = sp.edit();
                    try {
                        ed.putString("player", GameObjects.getPlayer().getJSON().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ed.apply();
                }
            }

            @Override
            public void onError(int TYPE, JSONObject response) {

            }
        });

        GameSettings.addSettingsListener(new SettingsListener() {
            @Override
            public void onSettingsSave() {

            }

            @Override
            public void onSettingsLoad() {

            }

            @Override
            public void onSettingChange(String setting) {
                if (objects == null) return;
                switch (setting) {
                    case "SHOW_AMBUSH_RADIUS":
                        for (GameObject o : objects.values()) {
                            if (o instanceof Ambush) {
                                ((Ambush) o).showRadius();
                                o.changeMarkerSize();
                            }
                        }
                        break;
                    case "SHOW_CITY_RADIUS":
                        for (GameObject o : objects.values()) {
                            if (o instanceof City) {
                                ((City) o).showRadius();
                                o.changeMarkerSize();
                            }

                        }
                        break;
                    case "SHOW_BUILD_AREA":
                        for (GameObject o:objects.values()){
                            if (o instanceof  City) ((City) o).showBuildZone();
                        }
                    case "SHOW_CARAVAN_ROUTE":
                        GameObjects.getPlayer().showRoute();
                        break;
                }
            }
        });
        GPSInfo.getInstance().AddLocationListener(new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng target = new LatLng(location.getLatitude(), location.getLongitude());
                GameObjects.getPlayer().setPosition(target);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

    public static HashMap put(GameObject object){
        if (objects.get(object.getGUID())!=null) return objects;
        if (!(object instanceof City) && object.isOwner()){
            player.putObject(object);
            return objects;
        }
        objects.put(object.getGUID(),object);
        if (object instanceof  ActiveObject) {
            //Добавить в перечень активных объектов.
            putActive(object);
        }
        return objects;
    }
    private static HashMap<String,GameObject> activeObjects;
    public static HashMap<String,GameObject> putActive(GameObject object){
        if (!(object instanceof ActiveObject)) return null;
        if (activeObjects.get(object.getGUID())!=null) return activeObjects;
        activeObjects.put(object.getGUID(),object);
        return activeObjects;
    }
    public static HashMap<String,GameObject> removeActive(GameObject object){
        if (!(object instanceof ActiveObject)) return null;
        activeObjects.remove(object.getGUID());
        return activeObjects;
    }
    public static HashMap<String, GameObject> getInstance(){
        return objects;
    }
    public static Player getPlayer(){
        return player;
    }

    public static void setMap(GoogleMap map) {
        if (player!=null) player.setMap(map);
        if (objects!=null) for (GameObject object:objects.values()){
            object.setMap(map);
        }
    }
}
