package ac.springmvc.demo.controller;

import ac.springmvc.demo.annotation.MyAutowired;
import ac.springmvc.demo.annotation.MyController;
import ac.springmvc.demo.annotation.MyRequestMapping;
import ac.springmvc.demo.annotation.MyRequestParam;
import ac.springmvc.demo.service.TestService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author acemma
 * @date 2018/12/28 11:38
 * @Description
 */
@MyController
@MyRequestMapping("/api/test")
public class Test1Controller {

    @MyAutowired
    private TestService testService;

    @MyRequestMapping(value = "/test1")
    public void myTest1(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("param")String param){
        try {
            response.getWriter().write( "Test1Controller:the param you send is :"+param);
            testService.printParam(param);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
