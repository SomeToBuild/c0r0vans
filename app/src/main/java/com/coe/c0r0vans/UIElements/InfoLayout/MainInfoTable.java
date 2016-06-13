package com.coe.c0r0vans.UIElements.InfoLayout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coe.c0r0vans.GameObjects.Player;
import com.coe.c0r0vans.R;
import com.coe.c0r0vans.ShowHideForm;

import utility.StringUtils;

/**
 * Информация об игроке
 */
public class MainInfoTable extends LinearLayout implements PlayerInfoLayout {
    private TextView level;
    private TextView exp;
    private TextView gold;
    private TextView caravans;
    private TextView ambushes;
    private ImageView faction;
    private TextView tnl;
    private TextView profit;
    private TextView hirelings;
    private TextView leftToHire;
    private TextView foundedCities;

    public MainInfoTable(Context context) {
        super(context);
        init();
    }

    public MainInfoTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MainInfoTable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init(){
        inflate(getContext(), R.layout.info_main,this);
        level= (TextView) findViewById(R.id.levelInfo);
        exp= (TextView) findViewById(R.id.expInfo);
        tnl= (TextView) findViewById(R.id.tnlInfo);
        gold= (TextView) findViewById(R.id.goldInfo);
        caravans= (TextView) findViewById(R.id.caravanInfo);
        ambushes= (TextView) findViewById(R.id.ambushSetInfo);
        faction= (ImageView) findViewById(R.id.factionSymbol);
        profit = (TextView) findViewById(R.id.profit);
        hirelings= (TextView) findViewById(R.id.hirelings);
        leftToHire= (TextView) findViewById(R.id.leftHirelings);
        foundedCities= (TextView) findViewById(R.id.foundedCities);
    }
    public void update(){
        if (Player.getPlayer()!=null) {
            level.setText(StringUtils.intToStr(Player.getPlayer().getLevel()));
            exp.setText(StringUtils.intToStr(Player.getPlayer().getExp()));
            gold.setText(StringUtils.intToStr(Player.getPlayer().getGold()));
            tnl.setText(StringUtils.intToStr(Player.getPlayer().getTNL()));
            caravans.setText(StringUtils.intToStr(Player.getPlayer().getCaravans()));
            ambushes.setText(StringUtils.intToStr(Player.getPlayer().getAmbushMax() - Player.getPlayer().getAmbushLeft())+"/"+StringUtils.intToStr(Player.getPlayer().getAmbushMax()));
            profit.setText(StringUtils.intToStr(Player.getPlayer().getProfit()));
            hirelings.setText(StringUtils.intToStr(Player.getPlayer().getHirelings()));
            leftToHire.setText(StringUtils.intToStr(Player.getPlayer().getLeftToHire()));
            foundedCities.setText(StringUtils.intToStr(Player.getPlayer().getFoundedCities())+"/"+StringUtils.intToStr(Player.getPlayer().getCityMax()));

            if (Player.getPlayer().getRace() == 1) {
                faction.setImageResource(R.mipmap.guild);
            } else if (Player.getPlayer().getRace() == 2) {
                faction.setImageResource(R.mipmap.alliance);
            } else if (Player.getPlayer().getRace() == 3) {
                faction.setImageResource(R.mipmap.legue);
            }
        }
    }

    @Override
    public void setParent(ShowHideForm parent) {

    }

}
