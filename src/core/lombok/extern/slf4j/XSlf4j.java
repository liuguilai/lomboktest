/*
 * Copyright (C) 2012-2017 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.extern.slf4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Causes lombok to generate a logger field.
 * <p>
 * Complete documentation is found at <a href="https://projectlombok.org/features/Log">the project lombok features page for lombok log annotations</a>.
 * <p>
 * Example:
 * <pre>
 * &#64;XSlf4j
 * public class LogExample {
 * }
 * </pre>
 * 
 * will generate:
 * 
 * <pre>
 * public class LogExample {
 *     private static final org.slf4j.ext.XLogger log = org.slf4j.ext.XLoggerFactory.getXLogger(LogExample.class);
 * }
 * </pre>
 * 
 * This annotation is valid for classes and enumerations.<br>
 * @see <a href="https://www.slf4j.org/api/org/slf4j/ext/XLogger.html">org.slf4j.ext.XLogger</a>
 * @see <a href="https://www.slf4j.org/api/org/slf4j/ext/XLoggerFactory.html#getXLogger(java.lang.Class)">org.slf4j.ext.XLoggerFactory#getLogger(java.lang.Class)</a>
 * @see lombok.extern.apachecommons.CommonsLog &#64;CommonsLog
 * @see lombok.extern.java.Log &#64;Log
 * @see lombok.extern.log4j.Log4j &#64;Log4j
 * @see lombok.extern.log4j.Log4j2 &#64;Log4j2
 * @see lombok.extern.slf4j.Slf4j &#64;Slf4j
 * @see lombok.extern.jbosslog.JBossLog &#64;JBossLog
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface XSlf4j {
	/** @return The category of the constructed Logger. By default, it will use the type where the annotation is placed. */
	String topic() default "";
    /** @return if it's true logger will be static, else won't */
    boolean isStatic() default true;
}
