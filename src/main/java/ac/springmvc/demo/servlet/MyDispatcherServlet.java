package ac.springmvc.demo.servlet;

import ac.springmvc.demo.annotation.MyAutowired;
import ac.springmvc.demo.annotation.MyController;
import ac.springmvc.demo.annotation.MyRequestMapping;
import ac.springmvc.demo.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author acemma
 * @date 2018/12/25 18:05
 * @Description MVC框架的请求分发中转
 * 继承HttpServlet，重写init方法、doGet、doPost方法
 */
public class MyDispatcherServlet extends HttpServlet {

    private Logger log = Logger.getLogger("init");

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    //handlerMapping的类型可以自定义为Handler
    private Map<String, Method> handlerMapping = new  HashMap<>();

    private Map<String, Object> controllerMap  = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        log.info("初始化MyDispatcherServlet");
        //1.加载配置文件，填充properties字段；
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.根据properties，初始化所有相关联的类,扫描用户设定的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));

        //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
        doInstance();

        // 4.自动化注入依赖
        doAutowired();

        //5.初始化HandlerMapping(将url和method对应上)
        initHandlerMapping();

        doAutowired2();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("执行MyDispatcherServlet的doGet()");
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("执行MyDispatcherServlet的doPost()");
        try {
            //处理请求
            doDispatch(req,resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }

    private void doDispatch(HttpServletRequest req,HttpServletResponse resp) throws Exception{
        if (handlerMapping.isEmpty()){
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url=url.replace(contextPath, "");
        if (!this.handlerMapping.containsKey(url)){
            resp.setStatus(404);
            resp.getWriter().write("404 NOT FOUND!");
            log.info("404 NOT FOUND!");
            return;
        }
        Method method = this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];
        //方法的参数列表
        for (int i=0; i<parameterTypes.length; i++){
            //根据参数名称处理
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")){
                paramValues[i] = req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")){
                paramValues[i]=resp;
                continue;
            }
            if(requestParam.equals("String")){
                for (Map.Entry<String,String[]> param:parameterMap.entrySet()){
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i]=value;
                }
            }
        }
        try {
            method.invoke(this.controllerMap.get(url),paramValues);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 加载配置文件，读取配置信息，将其填充到properties字段
     * @param location 配置文件的位置
     */
    private void doLoadConfig(String location){
        //把web.xml中的contextConfigLocation对应的value值的文件加载到输入流里
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {

            log.info("读取" + location + "里面的文件");
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != resourceAsStream){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 将指定包下扫描的类，添加到classNames字段中
     * @param packageName 需要扫描的包名
     */
    private void doScanner(String packageName){
        log.info("doScanner");
        String name = "/"+packageName.replaceAll("\\.","/");
        URL url = this.getClass().getResource(name);
        File dir = new File(url.getFile());
        for (File file:dir.listFiles()){
            if (file.isDirectory()){
                doScanner(packageName+"."+file.getName());
            }else {
                String className = packageName + "." + file.getName().replace(".class","");
                classNames.add(className);
            }
        }
    }

    /**
     * 将classNames中的类实例化，经key-value：类名（小写）-类对象放入ioc字段中
     */
    private void doInstance(){
        log.info("do instance");
        if (classNames.isEmpty()){
            return;
        }
        for (String className:classNames){
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)){
                    ioc.put(clazz.getSimpleName(),clazz.newInstance());
                }else if (clazz.isAnnotationPresent(MyService.class)){
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanName = myService.value();
                    if ("".equals(beanName.trim())){
                        beanName=toLowerFirstWord(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces){
                        ioc.put(i.getName(),instance);
                    }
                }else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * 实现自动注入
     */
    private void doAutowired(){
        log.info("doAutowired");
        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doAutowired2(){
        log.info("doAutowired2");
        if (controllerMap.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry:controllerMap.entrySet()){
            //包括私有的方法，在spring中没有隐私，@MyAutowired可以注入public、private字段
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired autowired= field.getAnnotation(MyAutowired.class);
                String beanName=autowired.value().trim();
                if ("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    /**
     * 初始化HandlerMapping(将url和method对应起来)
     */
    private void initHandlerMapping(){
        log.info("init HandlerMapping");
        if (ioc.isEmpty()){
            return;
        }
        try {
            for (Map.Entry<String, Object> entry: ioc.entrySet()){
                Class<? extends Object> clazz = entry.getValue().getClass();
                if(!clazz.isAnnotationPresent(MyController.class)){
                    continue;
                }
                //拼接URL
                String baseUrl = "";
                if (clazz.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping clazzAnnotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = clazzAnnotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method:methods){
                    if (!method.isAnnotationPresent(MyRequestMapping.class)){
                        continue;
                    }
                    MyRequestMapping methodAnnotation = method.getAnnotation(MyRequestMapping.class);
                    String url = methodAnnotation.value();
                    url = baseUrl+url;
                    handlerMapping.put(url,method);
                    controllerMap.put(url,clazz.newInstance());
                    System.out.println(url+","+method);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }


    /**
     * 将字符串中的首字母小写
     * @param name
     * @return
     */
    private String toLowerFirstWord(String name){

        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

}
