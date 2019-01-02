package ac.springmvc.demo.annotation;

import java.lang.annotation.*;

/**
 * @author acemma
 * @date 2018/12/25 17:42
 * @Description
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {
    String value() default "";
}


