package com.ly.controller;

import com.jfinal.core.Controller;
import com.jfinal.plugin.ehcache.CacheKit;
import com.ly.Global;
import com.ly.model.Img;
import com.ly.model.Trip;
import com.ly.model.Webmenu;
import com.ly.tool.Dwz;

import javax.servlet.http.HttpSession;
import java.util.List;


public class TripController extends Controller {

    public void index()
    {

        Trip menu= getModel(Trip.class);
        setAttr("page", Trip.tripDao.getListTrip(getParaToInt("pageNum", 1), getParaToInt("numPerPage",20),menu));
        setAttr("trip", menu);
        render("user_list.jsp");
        render("trip_list.jsp");
    }

    public void edit()
    {
        Integer id = getParaToInt();
        if (id == null || id <= 0){
            setAttr("trip",null);
        }else{
            setAttr("trip", Trip.tripDao.getTrip(id));
        }
        render("trip.jsp");
    }

    public void save()
    {
        HttpSession session = getSession();
        Object o = session.getAttribute(Global.USER_ID);
        Trip trip = getModel(Trip.class);
        trip.set("userid",o);
        boolean ok = Trip.tripDao.saveOrUpdate(trip);
        if (ok)
        {
            session.setAttribute("tripid",trip.get("id"));
            redirect("/upload");
        }
    }

    public void del()
    {
        boolean ok =  Trip.tripDao.deleteById(getParaToInt());
        CacheKit.removeAll("trip");
        renderJson(Dwz.jsonRtn(ok,"trip",""));
    }

    public void show()
    {
        HttpSession session = getSession();
        Object userid = session.getAttribute(Global.USER_ID);

        Integer tripid = Integer.parseInt(getPara(0));
        setAttr("trip", Trip.tripDao.getTrip(tripid));

        List<Img> list_img = Img.imgDao.getListImgByTripid(Integer.parseInt(userid.toString()),tripid);
        setAttr("list_img",list_img);
    }

}