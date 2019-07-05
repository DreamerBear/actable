# <img src="http://img.jasoncui.online/logo.svg" height=120 /> actable(表结构同步工具)

[![version][version-badge]][CHANGELOG] 
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)


  为统一数据平台开发的表结构同步工具<br/>
  是一个基于Spring和Mybatis的Maven项目，支持springboot，增强了Mybatis的功能<br/>
  通过读取jar包中model注解的方式来创建表，修改表结构，目前仅支持Mysql<br/>
  已通过provided阻止maven依赖传递性<br/>
  例子见:[actable-demo]<br/>
  
## Development
### 项目包结构说明
- actable-annotation（提供定义表结构的注解）
  - com.mhc.actable
    - annotation（定义表结构的注解）
    - constants（定义表结构的常量）
    
- actable-core（表结构同步核心代码）
  - com.mhc.actable
    - config（提供开箱即用的springboot接入机制）
    - core（表结构同步核心代码）
    - EntitySyncHandler（表结构同步执行器,使用方式同工具类）

### 接入说明
完整示例参考:[actable-demo]
#### 创建表结构定义jar包
1.创建jar包项目,添加pom依赖
```xml
<dependency>
    <groupId>com.mhc</groupId>
    <artifactId>actable-annotation</artifactId>
    <version>1.0.0</version>
</dependency>
```
2.jar包项目中添加表结构定义model,表结构定义注解在com.mhc.actable.annotation包下,注释自说明
```java
@Table(name = "test_a", comment = "测试表a")
public class TestA {

    @Column(name = "id", type = MySqlTypeConstant.VARCHAR, length = 11, isNull = false, comment = "主键")
    private Integer id;

    @Column(name = "name", type = MySqlTypeConstant.VARCHAR, length = 111, comment = "名字")
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```
3.将jar包项目打好包,保存在本地目录
#### springboot项目读取jar包,并执行表结构同步
1.在启动类上添加@EnableActable注解
```java
@SpringBootApplication
@EnableActable
public class ActableDemoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActableDemoServerApplication.class, args);
    }

}
```
2.调用EntitySyncHandler方法
```java
String jarLocalPath = "/Users/xxx/repo/com/mhc/actable-demo-jar/1.0.0-SNAPSHOT/actable-demo-jar-1.0.0-SNAPSHOT.jar"
EntitySyncHandler.syncJarEntitiesToMainDB(jarLocalPath);
```

## Contribute

Please do contribute! Issues and pull requests are welcome.

Thank you for your help improving our performance at a time!

Any questions contact with xuchao@maihaoche.com please.

[CHANGELOG]: ./CHANGELOG.md
[version-badge]: https://img.shields.io/badge/version-1.0.0-blue.svg
[actable-demo]: https://git.dawanju.net/xuchao/actable-demo
