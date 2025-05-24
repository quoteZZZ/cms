package com.cms;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author quoteZZZ
 */
//@EnableFileStorage
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class CmsApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(CmsApplication.class, args);
        System.out.println("    (竞赛管理系统启动成功)     \n" +
                "   .---. .-.   .-. .----.          \n" +
                "  /  ___}|  `.'  |{ {__            \n" +
                "  \\     }| |\\ /| |.-._} }          \n" +
                "   `---' `-' ` `-'`----'           ");


    }
}
