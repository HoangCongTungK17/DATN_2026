package vn.hoangtung.jobfind.util.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiMessage {// https://naveen-metta.medium.com/understanding-java-annotations-and-creating-custom-annotations-e87cb77072ed
    String value();
}
