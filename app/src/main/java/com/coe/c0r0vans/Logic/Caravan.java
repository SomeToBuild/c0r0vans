package com.coe.c0r0vans.Logic;

import android.graphics.Color;
import android.util.Log;

import com.coe.c0r0vans.GameObject.GameObject;
import com.coe.c0r0vans.GameObject.OnGameObjectRemove;
import com.coe.c0r0vans.Singles.GameObjects;
import com.coe.c0r0vans.Singles.MyGoogleMap;
import com.coe.c0r0vans.Singles.SelectedObject;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import utility.ImageLoader;
import utility.settings.GameSettings;

/**
 * Caravan Object
 */
public class Caravan extends GameObject {


    private int faction=0;
    private String startName;
    private String finishName;
    private int distance;
    private int profit;
    private LatLng startPoint;
    private LatLng finishPoint;
    private Polyline line;
    private int time;
    private String startGUID;
    private String finishGUID;
    private int lat;
    private int lng;


    public Caravan(GoogleMap map,JSONObject obj) throws JSONException {
        this.map=map;
        loadJSON(obj);

    }

    @Override
    public void setMap(GoogleMap map) {
        super.setMap(map);

        if (mark!=null) mark.remove();
        LatLng latLng=new LatLng(lat/1e6,lng/1e6);
        setMarker(map.addMarker(new MarkerOptions().position(latLng)));
        if (startPoint!=null && finishPoint!=null) {
            if (line != null)
            {
                line.getPoints().set(0,startPoint);
                line.getPoints().set(1,finishPoint);
            } else {
                PolylineOptions options = new PolylineOptions();
                options.width(3);
                GameObject target = SelectedObject.getInstance().getTarget();
                if (target != null && target instanceof City && !(target.getGUID().equals(startGUID) || target.getGUID().equals(finishGUID)))
                    options.color(Color.LTGRAY);
                else options.color(Color.BLUE);
                options.geodesic(true);
                options.add(startPoint);
                options.add(finishPoint);
                options.zIndex(150);
                line = map.addPolyline(options);
            }
            showRoute();
        }
    }

    @Override
    public void setMarker(Marker m) {
        mark=m;
        changeMarkerSize();
    }
    public JSONObject getJSON() throws JSONException {
        JSONObject result=new JSONObject();

        result.put("GUID",GUID);
        result.put("StartName",startName);
        result.put("FinishName",finishName);
        result.put("StartGUID",startGUID);
        result.put("FinishGUID",finishGUID);
        result.put("profit",profit);
        result.put("Lat",mark.getPosition().latitude*1e6);
        result.put("Lng",mark.getPosition().longitude*1e6);
        result.put("Distance",distance);
        result.put("profit",profit);
        if (startPoint!=null) {
            result.put("StartLat", startPoint.latitude * 1e6);
            result.put("StartLng", startPoint.longitude * 1e6);
        }
        if (finishPoint!=null) {
            result.put("FinishLat", finishPoint.latitude * 1e6);
            result.put("FinishLng", finishPoint.longitude * 1e6);
        }

        return result;
    }
    @Override
    public void loadJSON(JSONObject obj) {
        try {
            GUID=obj.getString("GUID");
            if (obj.has("Owner")) faction=obj.getInt("Owner"); else faction=0;
            owner=faction==0;
            if (obj.has("StartName")) startName = obj.getString("StartName");
            if (obj.has("FinishName")) finishName = obj.getString("FinishName");
            if (obj.has("StartGUID")) startGUID = obj.getString("StartGUID");
            if (obj.has("FinishGUID")) finishGUID = obj.getString("FinishGUID");
            if (obj.has("Distance")) distance = obj.getInt("Distance");
            if (obj.has("profit")) profit=obj.getInt("profit");
            if (obj.has("Lat") && obj.has("Lng")) {

                lat = obj.getInt("Lat");
                lng = obj.getInt("Lng");
                LatLng latlng = new LatLng(lat / 1e6, lng / 1e6);
                if (mark == null) {
                    if (map!=null) setMarker(map.addMarker(new MarkerOptions().position(latlng)));
                } else {
                    mark.setPosition(latlng);
                }
            }
            if (owner) {
                if (obj.has("StartLat") && obj.has("StartLng")) {
                    if (!(obj.getInt("StartLat") == 0 && obj.getInt("StartLng") == 0))
                        startPoint = new LatLng(obj.getInt("StartLat") / 1e6, obj.getInt("StartLng") / 1e6);

                }
                if (obj.has("FinishLat") && obj.has("FinishLng")) {
                    if (!(obj.getInt("FinishLat") == 0 && obj.getInt("FinishLng") == 0))
                        finishPoint = new LatLng(obj.getInt("FinishLat") / 1e6, obj.getInt("FinishLng") / 1e6);

                }
                if (startPoint!=null && finishPoint!=null & map!=null) {
                    if (line != null)
                    {
                        line.getPoints().set(0,startPoint);
                        line.getPoints().set(1,finishPoint);
                    } else {
                        PolylineOptions options = new PolylineOptions();
                        options.width(3);
                        GameObject target = SelectedObject.getInstance().getTarget();
                        if (target != null && target instanceof City && !(target.getGUID().equals(startGUID) || target.getGUID().equals(finishGUID)))
                            options.color(Color.LTGRAY);
                        else options.color(Color.BLUE);
                        options.geodesic(true);
                        options.add(startPoint);
                        options.add(finishPoint);
                        options.zIndex(150);
                        line = map.addPolyline(options);
                    }
                    showRoute();
                }

            }
            //time=S/v + (v*v-(1+a)*(1+a))/2/(a)/v-0.7
            Upgrade up= GameObjects.getPlayer().getUpgrade("speed");
            if (up!=null && up.getEffect2()>0) {
                int v=up.getEffect2();
                int a=up.getEffect1();
                int S=distance;
                int S0= (int) Math.ceil((Math.pow(v,2)-1)/2/a);
                if (S0>S) time= (int) Math.ceil(Math.sqrt(2*S/a));
                else {
                    int t0 = (int) Math.ceil(v / a);
                    time = (S - S0) / v + t0;
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void setPostion(LatLng latLng) {
        if (mark!=null){

            mark.setPosition(latLng);
        } else setMarker(map.addMarker(new MarkerOptions().position(latLng)));
    }
    private String currentMarkName="";
    @Override
    public void changeMarkerSize() {
        if (mark!=null) {
            String markname = "caravan";
            if (faction<0 || faction>4) faction=4;
            if (faction==0) markname=markname+"_"+faction+ GameObjects.getPlayer().getRace();
            else markname=markname+"_"+faction;
            markname = markname + GameObject.zoomToPostfix(MyGoogleMap.getClientZoom());
            if (!markname.equals(currentMarkName)) {
                mark.setIcon(ImageLoader.getDescritor(markname));
                if ("Y".equals(GameSettings.getInstance().get("USE_TILT")))
                    mark.setAnchor(0.5f, 1f);
                else mark.setAnchor(0.5f, 0.5f);
                currentMarkName=markname;
            }
        }
    }


    public int getFaction(){
        return faction;
    }

    public void showRoute(){
        if (line!=null)
        {
            line.setVisible("Y".equals(GameSettings.getInstance().get("SHOW_CARAVAN_ROUTE")));
        }
    }

    public String getStartGUID() {
        return startGUID;
    }
    public String getFinishGUID() {
        return finishGUID;
    }

    public String getFinishName() {
        return finishName;
    }
    public void fadeRoute(){
        if (line!=null)
            line.setColor(Color.LTGRAY);
    }
    public void releaseFade(){
        if (line!=null)
            line.setColor(Color.BLUE);

    }

    public int getProfit() {
        return profit;
    }

    public LatLng getStartPoint() {
        return startPoint;
    }

    public LatLng getFinishPoint() {
        return finishPoint;
    }

    public LatLng getPoint() {
        return mark.getPosition();
    }

    public int getDistance() {
        return distance;
    }

    public String getStartName() {
        return startName;
    }

    public int getTime() {
        return time;
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

        if (line!=null) {
            Log.d("Leak","LineRemoved");
            line.remove();
            line=null;
        }


    }

    public void setFaction(int faction) {
        this.faction = faction;
        owner = faction == 0;
    }
}