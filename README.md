A simple and flexible web service framework based on Mina
============================
1. 基于Apache Mina框架下的简单和灵活的Web服务框架
2. 类似于SpringBoot的接口编码风格
3. 支持Mysql数据库,仿照ThinkPHP的数据库访问风格
4. 支持定时任务,参考SpringBoot
5. 支持Websocket服务

快速集成：
============================
```java
    <!-- https://mvnrepository.com/artifact/com.fenglinga/tinyspring -->
    <dependency>
        <groupId>com.fenglinga</groupId>
        <artifactId>tinyspring</artifactId>
        <version>1.0.4</version>
    </dependency>
```

如何使用：
============================

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
	public String hello_world(VelocityContext model) throws Exception {
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

Websocket支持：
```java
package endpoint;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fenglinga.tinyspring.framework.annotation.Component;
import com.fenglinga.tinyspring.framework.annotation.websocket.OnClose;
import com.fenglinga.tinyspring.framework.annotation.websocket.OnError;
import com.fenglinga.tinyspring.framework.annotation.websocket.OnMessage;
import com.fenglinga.tinyspring.framework.annotation.websocket.OnOpen;
import com.fenglinga.tinyspring.framework.annotation.websocket.ServerEndpoint;
import com.fenglinga.tinyspring.framework.websocket.WebSocketCodecPacket;

@ServerEndpoint(value = "/websocket")
@Component
public class WebSocketEndpoint {
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketEndpoint.class);
    
    @OnOpen
    public void onOpen(IoSession session) {
        LOG.info("OPENED:" + session);
    }

    @OnClose
    public void onClose(IoSession session) {
    	LOG.info("CLOSED:" + session);
    }
    
    @OnMessage
    public void onMessage(String message, IoSession session) {
    	LOG.info("onMessage:" + message);
    	WebSocketCodecPacket result = WebSocketCodecPacket.buildPacket("Hello from server:" + message);
    	session.write(result);
    }
    
    @OnMessage
    public void onMessage(byte[] message, IoSession session) {
    }

    @OnError
    public void onError(IoSession session, Throwable error) {
        error.printStackTrace();
    }
}
```

数据库访问：
```java
Db.name("users")
	.alias("u")
	.join("tp_user_account ua", "u.id = ua.member_id")
	.field("id,recommend_id,mobile")
	.where("mobile", "12345678901")
	.find();
```

帮助反馈
============================
有任何技术问题，请加QQ群：836405402

