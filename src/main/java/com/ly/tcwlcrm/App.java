package com.ly.tcwlcrm;

import com.ly.tcwlcrm.plug.IPage;
import com.ly.tcwlcrm.plug.Page;
import com.ly.tcwlcrm.pojo.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession session = sqlSessionFactory.openSession();
        User user = new User();
        user.setAge(18);
        user.setMobile("15779658255");
        user.setName("gpd");
        user.setSex(1);
        System.out.println(session.insert("insert", user));
        session.commit();
        IPage<User> page = new Page<>();
        page.setCurrent(1);
        page.setSize(3);
        List<Object> select = session.selectList("select", page);
        List<User> records = page.getRecords();
        for (Object o : records) {
            System.out.println(o);
        }

        System.out.println("总数：" + page.getTotal() + ";第" + page.getCurrent() + "页;本页条数：" + page.getSize());
        System.exit(0);
    }

}
