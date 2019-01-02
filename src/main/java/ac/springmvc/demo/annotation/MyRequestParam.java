package ac.springmvc.demo.annotation;

import java.lang.annotation.*;

/**
 * @author acemma
 * @date 2018/12/25 17:43
 * @Description
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {

    String value();
}
