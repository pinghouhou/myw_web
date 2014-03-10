package com.ly.controller;

import com.jfinal.core.Controller;
import com.jfinal.plugin.ehcache.CacheKit;
import com.ly.Global;
import com.ly.model.*;
import com.ly.tool.Dwz;

import javax.servlet.http.HttpSession;
import java.util.Date;
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
        Object userid = session.getAttribute(Global.USER_ID);
        Trip trip = getModel(Trip.class);
        trip.set("userid",userid);
        trip.set("adddate",new Date());
        System.out.println(trip.get("name"));
        boolean ok = Trip.tripDao.saveOrUpdate(trip);
        if (ok)
        {
            session.setAttribute("tripid",trip.get("id"));
            redirect("/showPlace");
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
        List<Webmenu> list_menu = Webmenu.webmenuDao.getListMenu();
        setAttr("webmenu_list",list_menu);

        HttpSession session = getSession();
        Integer tripid = Integer.parseInt(getPara(0));

        session.setAttribute(Global.TRIP_ID,tripid);
        setAttr("trip", Trip.tripDao.getTrip(tripid));

        List<Img> list_img = Img.imgDao.getListImgByTripid(tripid);
        setAttr("list_img",list_img);

        List<Flight> flight_list = Flight.flightDao.getListFlightByTrip(tripid);
        setAttr("flight_list",flight_list);

        List<Hotel> hotel_list = Hotel.hotelDao.getListHotelByTrip(tripid);
        setAttr("hotel_list",hotel_list);

        List<Place> place_list = Place.placeDao.getListPlaceByTripid(tripid);
        setAttr("place_list",place_list);

        List<Guide> guide_list = Guide.guideDao.getListGuideByTrip(tripid);
        setAttr("guide_list",guide_list);

        List<Restaurant> res_list = Restaurant.restaurantDao.getListRestaurantByTrip(tripid);
        setAttr("restaurant_list",res_list);

        render("/WEB-INF/index/show.jsp");
    }

    public void follow()
    {
        int id = getParaToInt("id");

        HttpSession session = getSession();
        Object userid = session.getAttribute(Global.USER_ID);
        Follow follow = new Follow();
        follow.set("tripid",id);
        follow.set("userid",userid);
        boolean ok =  Follow.followDao.saveOrUpdate(follow);
        renderJson(ok ? "1" : "0");
    }

}
