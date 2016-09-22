package com.coe.c0r0vans.Logic;

import android.graphics.Bitmap;
import android.util.Log;

import com.coe.c0r0vans.GameObject.ActiveObject;
import com.coe.c0r0vans.GameObject.GameObject;
import com.coe.c0r0vans.GameObject.OnGameObjectRemove;
import com.coe.c0r0vans.GameObjects.ObjectAction;
import com.coe.c0r0vans.Singles.GameObjects;
import com.coe.c0r0vans.Singles.MyGoogleMap;
import com.coe.c0r0vans.Singles.ToastSend;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import utility.GATracker;
import utility.GPSInfo;
import utility.GameSound;
import utility.ImageLoader;
import utility.internet.serverConnect;
import utility.notification.Essages;
import utility.settings.GameSettings;

/**
 * Класс обеспечивающий работу с сундуками
 */
public class Chest extends GameObject implements ActiveObject {
    private String currentMarkName;
    private LatLng latlng;
    @Override
    public void loadJSON(JSONObject obj) {

        try {
            GUID=obj.getString("GUID");
            if (obj.has("Lat") && obj.has("Lng")) {
                int lat = obj.getInt("Lat");
                int lng = obj.getInt("Lng");
                latlng = new LatLng(lat / 1e6, lng / 1e6);
                if (mark == null) {
                    if (map!=null) setMarker(map.addMarker(new MarkerOptions().position(latlng)));
                } else {
                    mark.setPosition(latlng);
                }

            }
            changeMarkerSize();
        } catch (JSONException e) {
            GATracker.trackException("LoadChest", e);
        }

    }
    public Chest(GoogleMap map, JSONObject jsonObject){
        super();
        loadJSON(jsonObject);
        setMap(map);
    }

    @Override
    public void setMap(GoogleMap map) {
        super.setMap(map);
        if (mark!=null) mark.remove();
        setMarker(map.addMarker(new MarkerOptions().position(getPosition())));
    }

    @Override
    public void setMarker(Marker m) {
        super.setMarker(m);
        changeMarkerSize();
    }

    @Override
    public void changeMarkerSize() {
        if (mark!=null) {
            String markname = "chest";
            markname = markname + GameObject.zoomToPostfix(MyGoogleMap.getClientZoom());
            if (!markname.equals(currentMarkName)) {
                mark.setIcon(ImageLoader.getDescritor(markname));
                if ("Y".equals(GameSettings.getInstance().get("USE_TILT")))
                    mark.setAnchor(0.5f, 1f);
                else mark.setAnchor(0.5f, 0.5f);
                currentMarkName=markname;
            }
            if (MyGoogleMap.getClientZoom()==ICON_SMALL){
                mark.setVisible(false);
            } else {
                mark.setVisible(true);
            }
        }
    }
    public void setVisibility(boolean visibility) {
        if (mark!=null) changeMarkerSize();
    }

    @Override
    public LatLng getPosition() {
        return latlng;
    }
    @Override
    public void RemoveObject() {

        if (removeListeners!=null){
            for (OnGameObjectRemove onGameObjectRemove:removeListeners){
                onGameObjectRemove.onRemove();
            }
        }
        if (mark!=null){
            mark.remove();
            mark=null;
        }
    }

    @Override
    public int getRadius() {
        return 30;
    }

    @Override
    public void useObject() {

        int dist= (int) GPSInfo.getDistance(GameObjects.getPlayer().getPosition(),getPosition());
        Log.d("Chest","Used:"+dist+" "+GameObjects.getPlayer().getActionDistance());
        if (this.getMarker()!=null
                && dist <GameObjects.getPlayer().getActionDistance()) {
            Log.d("Chest","Used2");
            serverConnect.getInstance().callOpenChest(getChestAction(), GPSInfo.getInstance().GetLat(),
                    GPSInfo.getInstance().GetLng(),getGUID());
        }
    }

    ObjectAction chestAction;
    public ObjectAction getChestAction(){
        if (chestAction==null){

                chestAction=new ObjectAction(this){
                    @Override
                    public Bitmap getImage() {
                        return ImageLoader.getImage("open_chest");
                    }
                    @Override
                    public String getCommand() {
                        return "OpenChest";
                    }

                    @Override
                    public void preAction() {
                        setVisibility(false);
                    }

                    @Override
                    public void postAction(JSONObject response) {
                        if (response.has("Gold")) {
                            try {
                                int gold=response.getInt("Gold");
                                GameObjects.getPlayer().setGold(GameObjects.getPlayer().getGold()+gold);
                                ToastSend.send("Получено "+gold+" золота.");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                        GameSound.playSound(GameSound.OPENCHEST);
                        Essages.addEssage("Награда получена");
                        RemoveObject();
                    }

                    @Override
                    public void postError(JSONObject response) {
                        setVisibility(true);
                        try {
                            String err;
                            if (response.has("Error")) err = response.getString("Error");
                            else if (response.has("Result")) err = response.getString("Result");
                            else err = "U0000";
                            switch (err) {
                                case "DB001":
                                    Essages.addEssage("Ошибка сервера.");
                                    break;
                                case "L0001":
                                    Essages.addEssage("Соединение потеряно.");
                                    break;
                                case "O1401":
                                    Essages.addEssage("Сундук пуст.");
                                    RemoveObject();
                                    break;
                                case "O1402":
                                    Essages.addEssage("Сундук слишком далеко.");
                                    break;
                                default:
                                    if (response.has("Message"))
                                        Essages.addEssage(response.getString("Message"));
                                    else Essages.addEssage("Непредвиденная ошибка.");

                            }
                        }catch (JSONException e)
                        {
                            GATracker.trackException("OpenChest",e);
                        }
                    }
                };


        }
        return chestAction;
    }

}
