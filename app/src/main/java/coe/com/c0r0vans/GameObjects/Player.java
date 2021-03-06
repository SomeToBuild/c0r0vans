package coe.com.c0r0vans.GameObjects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import coe.com.c0r0vans.ActionView;
import coe.com.c0r0vans.GameSound;
import coe.com.c0r0vans.MyGoogleMap;
import coe.com.c0r0vans.OnGameObjectChange;
import coe.com.c0r0vans.R;
import utility.Essages;
import utility.GPSInfo;
import utility.GameSettings;
import utility.ImageLoader;
import utility.ResourceString;
import utility.serverConnect;

/**
 * @author Shadilan
 */
public class Player extends GameObject {
    private static Player player;
    private int race=0;

    public static void instance(){
        player=new Player();
    }
    public static Player getPlayer(){
        return player;
    }

    //Fields
    private int Caravans=0;
    private int AmbushesMax=100;
    private int AmbushesLeft=100;
    private int Level=0;
    private int TNL=0;
    private int Exp=0;
    private int MostIn=0;
    private boolean routeStart=false;
    private int Gold=0;

    private int AmbushRadius=30;
    private int ActionDistance=50;

    //Arrays
    private ArrayList<Upgrade> Upgrades;
    private HashMap<String,Upgrade> NextUpgrades;
    private ArrayList<Route> Routes;
    private ArrayList<AmbushItem> Ambushes;

    private String currentRoute="";

    public Player() {
        init();
    }

    public void init(){
        image= ImageLoader.getImage("hero");
        Upgrades=new ArrayList<>();
        NextUpgrades=new HashMap<>();
        Routes=new ArrayList<>();
        Ambushes=new ArrayList<>();
        if (dropRoute==null) dropRoute = new ObjectAction(this) {
            @Override
            public Bitmap getImage() {
                return ImageLoader.getImage("drop_route");
            }

            @Override
            public String getInfo() {
                return "Сбросить маршрут.";
            }

            @Override
            public String getCommand() {
                return "DropUnfinishedRoute";
            }

            @Override
            public void preAction() {
                GameSound.playSound(GameSound.START_ROUTE_SOUND);
            }

            @Override
            public void postAction() {
                serverConnect.getInstance().getPlayerInfo();
                Essages.addEssage("Незаконченый маршрут отменен.");
            }
            @Override
            public void postError() {

            }
        };
        race=GameSettings.getFaction();

    }

    public int getAmbushRad(){return player.AmbushRadius;}


    @Override
    public void changeMarkerSize(float Type) {
        player.mark.setIcon(BitmapDescriptorFactory.fromResource(R.mipmap.marker));
    }

    @Override
    public int getProgress() {
        return (player.Exp*100/player.TNL);
    }

    @Override
    public void setVisibility(boolean visibility) {
        mark.setVisible(false);
        circle.setVisible(visibility);
        circle2.setVisible(visibility);
    }






    @Override
    public Marker getMarker() {
        return mark;
    }
    private Circle circle2;
    /*public Circle getCircle(){
        return circle;
    }*/
    public void setPosition(LatLng target){
        if (mark!=null) mark.setPosition(target);
        if (circle!=null) circle.setCenter(target);
        if (circle2!=null) circle2.setCenter(target);
    }
    @Override
    public void setMarker(Marker m) {
        mark=m;
        if (circle!=null) circle.setCenter(m.getPosition());
        else
        {
            CircleOptions circleOptions=new CircleOptions();
            circleOptions.center(m.getPosition());
            circleOptions.radius(ActionDistance);
            circleOptions.strokeColor(Color.parseColor("#D08D2E"));
            circleOptions.strokeWidth(5);
            circle=map.addCircle(circleOptions);

        }
        if (circle2!=null) circle2.setCenter(m.getPosition());
        else
        {
            CircleOptions circleOptions=new CircleOptions();
            circleOptions.center(m.getPosition());
            circleOptions.radius(5);
            circleOptions.strokeColor(Color.parseColor("#FF0000"));
            circleOptions.strokeWidth(5);
            circle2=map.addCircle(circleOptions);
        }
    }


    @Override
    public void loadJSON(JSONObject obj) {
        try {
            if (obj.has("GUID")) GUID=obj.getString("GUID");
            if (obj.has("PlayerName")) Name=obj.getString("PlayerName");
            if (obj.has("Level")) Level=obj.getInt("Level");
            if (obj.has("TNL")) TNL=obj.getInt("TNL");
            if (obj.has("Exp")) Exp=obj.getInt("Exp");
            if (obj.has("Gold")) Gold=obj.getInt("Gold");
            if (obj.has("Caravans")) Caravans=obj.getInt("Caravans");
            if (obj.has("AmbushesMax")) AmbushesMax=obj.getInt("AmbushesMax");
            if (obj.has("AmbushesLeft")) AmbushesLeft=obj.getInt("AmbushesLeft");
            if (obj.has("MostIn")) MostIn=obj.getInt("MostIn");
            if (obj.has("AmbushRadius")) AmbushRadius=obj.getInt("AmbushRadius");
            if (obj.has("ActionDistance")) ActionDistance=obj.getInt("ActionDistance");
            if (obj.has("Race")) race=obj.getInt("Race");
            if (race!=0) GameSettings.setFaction(race);
            circle.setRadius(ActionDistance);
            if (obj.has("Upgrades")){
                JSONArray upg=obj.getJSONArray("Upgrades");
                Upgrades.clear();
                for (int i=0;i<upg.length();i++) {
                    Upgrades.add(new Upgrade(upg.getJSONObject(i)));

                }
                Log.d("DebugInfo", "ULength" + Upgrades.size());

            }
            if (obj.has("Routes")){
                currentRoute="";
                JSONArray route=obj.getJSONArray("Routes");
                for (Route routel:Routes){
                    routel.RemoveObject();
                }
                Routes.clear();
                for (int i=0;i<route.length();i++) {
                    Route routeObj=new Route(route.getJSONObject(i),map);
                    if (routeObj.getFinishName().equals("null")) currentRoute=routeObj.getStartName();
                    else Routes.add(routeObj);

                }
            }
            if (obj.has("Ambushes")){
                JSONArray ambush=obj.getJSONArray("Ambushes");
                Ambushes.clear();
                for (int i=0;i<ambush.length();i++) Ambushes.add(new AmbushItem(ambush.getJSONObject(i)));
            }
            if (obj.has("NextUpgrades")){
                JSONArray nextUpgrade=obj.getJSONArray("NextUpgrades");
                NextUpgrades.clear();

                for (int i=0;i<nextUpgrade.length();i++) {
                    Upgrade nUp=new Upgrade(nextUpgrade.getJSONObject(i));
                    NextUpgrades.put(nUp.getType(), nUp);

                }
            }
            routeStart = currentRoute.equals("");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        change(OnGameObjectChange.EXTERNAL);
    }

    @Override
    public void RemoveObject() {

    }

    @Override
    public String getInfo() {
        return ResourceString.getInstance().getString("name")+this.Name+"\n"+
                ResourceString.getInstance().getString("gold")+this.Gold+"\n"+
                ResourceString.getInstance().getString("caravans")+this.Caravans+"\n";
    }

    //private  ObjectAction createAmbush;
    private ObjectAction dropRoute;
    public int getLevel() {
        return Level;
    }

    public int getExp() {
        return Exp;
    }

    public int getTNL() {
        return TNL;
    }

    public int getGold() {
        return Gold;
    }


    public int getCaravans() {
        return Caravans;
    }

    public int getAmbushLeft() {
        return AmbushesLeft;
    }

    public int getAmbushMax() {
        return AmbushesMax;
    }

    public int getMostReachIn() {
        return MostIn;
    }

    public ArrayList<Upgrade> getUpgrades() {
        return Upgrades;
    }

    public ArrayList<Route> getRoutes() {
        return Routes;
    }
    public ArrayList<AmbushItem> getAmbushes() {return Ambushes;}
    public int getActionDistance(){return ActionDistance;}
    public String getCurrentRoute(){return currentRoute;}
    public ObjectAction getDropRoute(){return dropRoute;}

    /*public void setCurrentRoute(String currentRoute) {
        this.currentRoute = currentRoute;
        change(OnGameObjectChange.PLAYER);
    }*/

    public void setRouteStart(boolean routeStart) {
        this.routeStart = routeStart;
        change(OnGameObjectChange.PLAYER);
    }

    public boolean getRouteStart() {
        return routeStart;
    }
    public void showRoute(){
        if (Routes!=null){
            for (Route route:Routes){
                route.showRoute();
            }
        }
    }

    private ArrayList<OnGameObjectChange> onChangeList;
    private ArrayList<OnGameObjectChange> removeOnChangeList;
    public void addOnChange(OnGameObjectChange onGameObjectChange){
        if (onChangeList==null) onChangeList=new ArrayList<>();
        onChangeList.add(onGameObjectChange);
    }
    /*public void removeOnChange(OnGameObjectChange onGameObjectChange){
        if (removeOnChangeList==null) removeOnChangeList=new ArrayList<>();
        removeOnChangeList.add(onGameObjectChange);
    }*/
    public void change(int type){
        if (onChangeList==null) return;
        if (removeOnChangeList!=null && removeOnChangeList.size()>0){
            onChangeList.removeAll(removeOnChangeList);
            removeOnChangeList.clear();
        }
        for (OnGameObjectChange ev:onChangeList){
            ev.change(type);
        }
    }
    public Upgrade getNextUpgrade(String type){
        Log.d("NextUp",type+" "+NextUpgrades.size());

        return NextUpgrades.get(type);
    }

    public void setMap(GoogleMap map) {
        this.map = map;

        mark=map.addMarker(new MarkerOptions().position(new LatLng(GPSInfo.getInstance().GetLat() / 1E6, GPSInfo.getInstance().GetLng() / 1E6)));
        CircleOptions circleOptions=new CircleOptions();
        circleOptions.center(new LatLng(GPSInfo.getInstance().GetLat() / 1E6, GPSInfo.getInstance().GetLng() / 1E6));
        circleOptions.radius(ActionDistance);
        circleOptions.strokeColor(Color.parseColor("#D08D2E"));
        circleOptions.strokeWidth(5);
        circle=map.addCircle(circleOptions);
        circleOptions=new CircleOptions();
        circleOptions.center(new LatLng(GPSInfo.getInstance().GetLat() / 1E6, GPSInfo.getInstance().GetLng() / 1E6));
        circleOptions.radius(5);
        circleOptions.strokeColor(Color.parseColor("#FF0000"));
        circleOptions.strokeWidth(5);
        circle2=map.addCircle(circleOptions);
        changeMarkerSize(MyGoogleMap.getClientZoom());
        mark.setAnchor(0.5f, 0.5f);
        mark.setVisible(false);
    }

    public int getRace() {
        Log.d("tttt","Race:"+race);
        return race;
    }

    @Override
    public RelativeLayout getObjectView(Context context) {
        return new ambushCreate(context);

    }
    class ambushCreate extends RelativeLayout implements GameObjectView{

        public ambushCreate(Context context) {
            super(context);
            init();
        }

        public ambushCreate(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public ambushCreate(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }
        private void init(){
            inflate(this.getContext(),R.layout.longtap_layout,this);
            findViewById(R.id.longtapClose).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    close();
                }
            });
            createAmbush=new ObjectAction(Player.getPlayer()) {
                @Override
                public Bitmap getImage() {
                    return ImageLoader.getImage("create_ambush");
                }

                @Override
                public String getInfo() {
                    return "Создание засады";
                }

                @Override
                public String getCommand() {
                    return "SetAmbush";
                }

                @Override
                public void preAction() {

                }

                @Override
                public void postAction() {
                    serverConnect.getInstance().RefreshCurrent();
                    GameSound.playSound(GameSound.SET_AMBUSH);
                    Essages.addEssage("Засада создана.");

                }

                @Override
                public void postError() {

                }
            };
            findViewById(R.id.createAmbushAction).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    serverConnect.getInstance().ExecCommand(createAmbush, Player.getPlayer().getGUID(),
                            GPSInfo.getInstance().GetLat(), GPSInfo.getInstance().GetLng(),
                            (int) (SelectedObject.getInstance().getPoint().latitude * 1e6),
                            (int) (SelectedObject.getInstance().getPoint().longitude * 1e6)
                    );
                    close();
                }
            });
        }
        ObjectAction createAmbush;


        @Override
        public void updateInZone(boolean inZone) {
            if (inZone) findViewById(R.id.createAmbushAction).setVisibility(VISIBLE);
            else findViewById(R.id.createAmbushAction).setVisibility(INVISIBLE);
        }

        @Override
        public void close() {
            this.setVisibility(GONE);
            actionView.HideView();

        }
        ActionView actionView;
        @Override
        public void setContainer(ActionView av) {
            actionView=av;
        }
    }
}
