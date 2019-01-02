package ac.springmvc.demo.service.impl;

import ac.springmvc.demo.annotation.MyService;
import ac.springmvc.demo.service.TestService;

/**
 * @author acemma
 * @date 2018/12/28 11:37
 * @Description
 */
@MyService
public class TestServiceImpl implements TestService {
    @Override
    public void printParam(String param) {
        System.out.println("接收到的参数为： "+param);
    }
}
