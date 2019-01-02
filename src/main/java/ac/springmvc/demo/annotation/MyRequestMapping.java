package ac.springmvc.demo.annotation;

import java.lang.annotation.*;

/**
 * @author acemma
 * @date 2018/12/25 17:42
 * @Description
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value();
}
