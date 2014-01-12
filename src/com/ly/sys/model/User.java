package com.ly.sys.model;

import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.CacheKit;
import com.ly.tool.Cnd;


public class User extends Model<User> {

    public static final User userDao = new User();

    public User getUser(Integer id)
    {
        return userDao.findById(id);
    }

    public Page<User> getListUser(int pageNum,int pageSize,User user)
    {
    	StringBuffer sb = new StringBuffer();
        if(Cnd.getSql(user, sb))
        {
            return userDao.paginate(pageNum, 20, "select *", sb.toString());
        }else{
        	return userDao.paginateByCache("user", "page_user_" + pageNum, pageNum, pageSize, "select *", "from user order by id desc");
        }
    }

    public boolean saveOrUpdate(User user)
    {
        boolean ok = false;
        Integer id = user.getInt("id");
        if (id == null || id <= 0){
            ok =  user.save();
        }else{
            ok = user.update();
        }
        CacheKit.removeAll("user");
        return ok;
    }
}