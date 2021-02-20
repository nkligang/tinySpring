A simple and flexible web service framework based on Mina
============================
1. 基于Apache Mina框架下的简单和灵活的Web服务框架
2. 类似于SpringBoot的接口编码风格
3. 支持Mysql数据库,仿照ThinkPHP的数据库访问风格
4. 支持定时任务,参考SpringBoot
5. 支持Websocket服务

============================

如何使用：
```java
    public static void main( String[] args ) throws Exception
    {
		new SpringAppBuilder().run(args);
    }
```

配置文件assets/application.properties：
```java
server.port=8080
server.env=dev
server.login_url=login.html
database.type=mysql
database.hostname=localhost
database.database=
database.username=
database.password=
database.hostport=3306
database.dsn=
database.params=
database.charset=utf8
database.prefix=
database.debug=true
database.deploy=0
database.rw_separate=false
database.master_num=1
database.slave_no=
database.fields_strict=true
database.resultset_type=array
database.auto_timestamp=false
database.sql_explain=false
database.show-sql=true
```

定义Controller：
```java
package controller;

import org.apache.mina.http.api.HttpMethod;
import org.apache.velocity.VelocityContext;

import com.fenglinga.tinyspring.framework.Controller;
import com.fenglinga.tinyspring.framework.annotation.RequestMapping;

@com.fenglinga.tinyspring.framework.annotation.Controller
public class HelloWordController extends Controller {	
	@RequestMapping(value = "/hello_world.html", method = HttpMethod.GET)
	public String user_group_list(VelocityContext model) throws Exception {
		return "hello_world.html";
	}
}
```

定义RestController：
```java
package controller;

import org.apache.mina.http.api.HttpMethod;
import org.apache.mina.http.api.HttpRequest;

import com.alibaba.fastjson.JSONObject;
import com.fenglinga.tinyspring.framework.Controller;
import com.fenglinga.tinyspring.framework.annotation.Comment;
import com.fenglinga.tinyspring.framework.annotation.RequestMapping;
import com.fenglinga.tinyspring.framework.annotation.RestController;

@RestController
public class HelloWordRestController extends Controller {
	@Comment(content="Hello World")
	@RequestMapping(value = "/hello_world", method = {HttpMethod.GET, HttpMethod.POST})
	public JSONObject hello_world(
			HttpRequest request
	) throws Exception {
		return new JSONObject();
	}
}
```
