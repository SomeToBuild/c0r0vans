package com.coe.c0r0vans.UIElements;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.coe.c0r0vans.GameObjects.Ambush;
import com.coe.c0r0vans.GameObjects.City;
import com.coe.c0r0vans.GameObjects.GameObject;
import com.coe.c0r0vans.GameObjects.GameObjectView;
import com.coe.c0r0vans.GameObjects.Player;
import com.coe.c0r0vans.GameObjects.SelectedObject;
import com.coe.c0r0vans.OnGameObjectChange;

import org.json.JSONObject;

import java.util.Arrays;

import utility.GPSInfo;
import utility.internet.ServerListener;
import utility.internet.serverConnect;

/**
 * @author Shadilan
 * Компонент для отображения действий
 */
public class ActionView extends LinearLayout {

    public ActionView(Context context) {
        super(context);
    }

    public ActionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(){
        //inflate(getContext(), R.layout.actions_layout, this);
        serverConnect.getInstance().addListener(new ServerListener() {
            @Override
            public void onLogin(JSONObject response) {

            }

            @Override
            public void onRefresh(JSONObject response) {

            }

            @Override
            public void onAction(JSONObject response) {
                if (getVisibility() == VISIBLE) {
                    reloadActions();
                }
            }

            @Override
            public void onPlayerInfo(JSONObject response) {

            }

            @Override
            public void onError(JSONObject response) {

            }

            @Override
            public void onMessage(JSONObject response) {

            }

            @Override
            public void onRating(JSONObject response) {

            }
        });
        Log.d("Loader","ErrorPlace");
        Player.getPlayer().addOnChange(new OnGameObjectChange() {
            @Override
            public void change(int ChangeType) {
                reloadActions();
            }
        });

        if (locationListener==null) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    reloadActions();

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
            };
            GPSInfo.getInstance(getContext()).AddLocationListener(locationListener);
        }
    }

    public void HideView(){
        SelectedObject.getInstance().hidePoint();
        SelectedObject.getInstance().setTarget(null);
        this.removeAllViews();
        Player.getPlayer().higlight(null);
    }
    LocationListener locationListener;
    RelativeLayout currentView=null;

    public void ShowView(){
        //Очистить вью
        //Загрузить вью
        //Обновить видимость экшенов
        Log.d("tttt","test_2");
        this.removeAllViews();
        GameObject target=SelectedObject.getInstance().getTarget();
        if (target instanceof Player){
            setCurrentView(target.getObjectView(getContext()));

        } else
        if (target instanceof City)
        {
            setCurrentView(target.getObjectView(getContext()));
            Player.getPlayer().higlight(target.getGUID());
        } else if (target instanceof Ambush)
        {
            setCurrentView(target.getObjectView(getContext()));

        }



        Log.d("tttt", "test_2");
    }
    public void setCurrentView(RelativeLayout view){
        currentView= view;
        ((GameObjectView) view).setContainer(this);
        this.addView(currentView);
        this.requestLayout();
        reloadActions();
    }

    private void reloadActions(){
        try {
            if (currentView != null && currentView instanceof GameObjectView) {
                if (SelectedObject.getInstance().getTarget()==null || SelectedObject.getInstance().getTarget().getMarker()==null) return;
                float[] distances = new float[1];
                Location.distanceBetween(SelectedObject.getInstance().getTarget().getMarker().getPosition().latitude,
                        SelectedObject.getInstance().getTarget().getMarker().getPosition().longitude,
                        Player.getPlayer().getMarker().getPosition().latitude,
                        Player.getPlayer().getMarker().getPosition().longitude, distances);

                boolean inZone = (distances.length > 0 && distances[0] <= (Player.getPlayer().getActionDistance()));
                ((GameObjectView) currentView).updateInZone(inZone);
                ((GameObjectView) currentView).setDistance((int) distances[0]);
            }
        } catch (Exception e)
        {
            serverConnect.getInstance().sendDebug(2, "UER:" + e.toString()+ Arrays.toString(e.getStackTrace()));
        }
    }

}