package com.ly.controller;

import com.alibaba.fastjson.JSON;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifReader;
import com.jfinal.core.Controller;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.upload.UploadFile;
import com.ly.Global;
import com.ly.model.Hotel;
import com.ly.model.Img;
import com.ly.model.ImgLove;
import com.ly.model.Trip;
import com.ly.tool.Dwz;
import com.ly.vo.FileUploadInfo;
import net.coobird.thumbnailator.Thumbnails;
import org.nutz.lang.Files;
import org.nutz.lang.random.StringGenerator;

import javax.servlet.http.HttpSession;
import javax.swing.text.html.HTML;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class ImgController extends Controller {

    public void index()
    {

        Img menu= getModel(Img.class);
        setAttr("page", Img.imgDao.getListImg(getParaToInt("pageNum", 1), getParaToInt("numPerPage",20),menu));
        setAttr("img", menu);
        render("user_list.jsp");
        render("img_list.jsp");
    }

    public void edit()
    {
        Integer id = getParaToInt();
        if (id == null || id <= 0){
            setAttr("img",null);
        }else{
            setAttr("img", Img.imgDao.getImg(id));
        }
        render("img.jsp");
    }

    public void save() throws IOException, ImageProcessingException {

        HttpSession session = getSession();
        Object userid = session.getAttribute(Global.USER_ID);
        Object tripid = session.getAttribute(Global.TRIP_ID);
        Object placeid = session.getAttribute(Global.PLACE_ID);

        PathKit pk = new PathKit();
        String contextPath = pk.getWebRootPath();
        String path = "/upload";
        String pathUrl = contextPath + path;
        int maxSize = 20 * 1024 * 1024;              //20M

        List<FileUploadInfo> fileInfos = new LinkedList<FileUploadInfo>();
        List<UploadFile> files = getFiles(pathUrl, maxSize, "utf-8");
        for (UploadFile uploadFile : files)
        {
            FileUploadInfo fileInfo = new FileUploadInfo();

            File f = uploadFile.getFile();

            //exif
            String lat = "";
            String lon = "";
            String createDate = "";
            Metadata metadata = ImageMetadataReader.readMetadata(f);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String tagName = tag.getTagName();
                    String desc = tag.getDescription();

                    int tagType = tag.getTagType();
                    if(tagType == 306 || tagName.equals("Date/Time Original") || tagName.equals("Date/Time")) {
                        createDate = desc;
                    } else if (tagName.equals("GPS Latitude")) {
                        lat = desc;
                    } else if (tagName.equals("GPS Longitude")) {
                        lon = desc;
                    }
                }
            }



            String type = Files.getSuffixName(f);

            StringGenerator sg = new StringGenerator(5);
            String name = sg.next() + System.currentTimeMillis();
            String fileName = name + "." + type;
            String s_fileName = name + "_200_200." + type;

            Thumbnails.of(f).size(200,200).toFile(uploadFile.getSaveDirectory() + "/" + s_fileName);
            Files.rename(f, fileName);

            String url = "/upload/"+fileName;
            String s_url  = "/upload/"+s_fileName;



            Img img = new Img();
            img.set("userid",userid);
            img.set("tripid",tripid);
            img.set("placeid",placeid);
            img.set("adddate",new Date());
            img.set("imgpath",url);
            img.set("smallimgpath",s_url);
            img.set("lat",lat);
            img.set("lon",lon);
            if (createDate.equals(""))
            {
                img.set("createdate",new Date());
            }else{
                img.set("createdate",createDate);
            }


            Img.imgDao.saveOrUpdate(img);

            Trip.tripDao.updateDefalutImg(tripid,s_url);

            DecimalFormat df = new DecimalFormat("#.00");

            fileInfo.setName(fileName);
            fileInfo.setUrl(url);
            fileInfo.setThumbnailUrl(s_url);
            fileInfo.setDeleteType("DELETE");
            fileInfo.setDeleteUrl(img.getInt("id").toString());
            fileInfo.setSize( df.format((double) f.getTotalSpace() / 1024) + "K");
            fileInfos.add(fileInfo);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("{\"files\":");
        sb.append(JSON.toJSONString(fileInfos));
        sb.append("}");

        System.out.println(sb.toString());

        renderJson(sb.toString());

    }

    public void del()
    {
        int id = getParaToInt("id");
        boolean ok =  Img.imgDao.deleteById(id);
        Object oTripid = getSession().getAttribute(Global.TRIP_ID);
        renderJson(ok ? Img.imgDao.count(oTripid) : "-1");
    }

    public void defaultImg()
    {
        int id = getParaToInt("id");
        Img img = Img.imgDao.findById(id);
        Boolean ok =  Trip.tripDao.updateDefalutImg(img.getInt("tripid"),img.getStr("imgpath"));
        renderJson(ok ? "1" : "0");

    }

    public void mark()
    {
        Img img = getModel(Img.class);
        Boolean ok = Img.imgDao.saveOrUpdate(img);
        renderJson(ok ? "1" : "0");
    }

    public void love()
    {
        HttpSession session = getSession();
        Object userid = session.getAttribute(Global.USER_ID);

        if (userid == null)
        {
            renderJson("-1");
            return;
        }

        int imgid = getParaToInt("id");

        Img img = Img.imgDao.findById(imgid);

        ImgLove imgLove = ImgLove.imgLoveDao.getImgLove(userid,imgid);

        int num = 0;

        if (imgLove == null)
        {
            ImgLove iLove = new ImgLove();
            iLove.set("userid",userid);
            iLove.set("imgid",imgid);
            ImgLove.imgLoveDao.saveOrUpdate(iLove);
            num = img.getInt("love") + 1;
        }else{
            imgLove.delete();
            num = img.getInt("love") - 1;
        }
        img.set("love",num);
        Boolean ok = Img.imgDao.saveOrUpdate(img);
        renderJson(ok ? num : "0");
    }

}
