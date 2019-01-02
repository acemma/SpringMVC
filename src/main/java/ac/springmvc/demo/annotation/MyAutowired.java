package ac.springmvc.demo.annotation;

import java.lang.annotation.*;

/**
 * @author acemma
 * @date 2018/12/25 17:41
 * @Description 实现自动注入
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {

    String value() default "";

}
