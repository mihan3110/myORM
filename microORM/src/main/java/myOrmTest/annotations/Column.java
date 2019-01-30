package myOrmTest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    /** Имя столбца (атрибута) **/
    String name() default "";
    /** Будет ли значение уникальным **/
    boolean unique() default false;
    /** Будут ли существовать значения NULL **/
    boolean nullable() default true;
    /** Длина 1 записи   **/
    int length() default 255;
    /** Точность вещественных чисел **/
    int precision() default 0;
    /** Размер для чисел **/
    int scale() default 0;
}




/*

СПИСОК ФУНКЦИЙ НАПИСАТЬ
СИНТАКСИС РАЗНЫЫХ БД
 */