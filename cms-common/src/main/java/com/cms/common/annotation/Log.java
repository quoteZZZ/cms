package com.cms.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.cms.common.enums.BusinessType;
import com.cms.common.enums.OperatorType;

/**
 * 自定义操作日志记录注解类：
 * (被com.cms.common.aspectj.LogAspect切面类使用)
 * 用于记录操作日志，利用切面在每个方法前以@Log记录操作日志
 * @author quoteZZZ
 *
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })//该注解定义了（注解的使用位置）可以应用于方法和方法参数的修饰范围。
@Retention(RetentionPolicy.RUNTIME)//（注解的作用范围）表示该注解会在运行时保留，可以通过反射机制读取。
@Documented//表示该注解会被JavaDoc工具记录，在生成文档时会被保存。
public @interface Log
{
    /**
     * 操作模块
     */
    public String title() default "";//默认为空

    /**
     * 操作功能（OTHER：其他，INSERT：查询，QUERY 新增，UPDATE：修改，DELETE：删除，EXPORT：导出，IMPORT：导入，GRANT：授权，FORCE：强退，GENCODE：生成代码，CLEAN：清空数据）
     */
    public BusinessType businessType() default BusinessType.OTHER;//默认为其他

    /**
     * 操作人类别（OTHER：其他，MANAGE：后台用户，MOBILE：手机端用户）
     */
    public OperatorType operatorType() default OperatorType.MANAGE;//默认为后台用户

    /**
     * 是否保存请求的参数
     */
    public boolean isSaveRequestData() default true;//默认为true

    /**
     * 是否保存响应的参数
     */
    public boolean isSaveResponseData() default true;//默认为true

    /**
     * 排除指定的请求参数
     */
    public String[] excludeParamNames() default {};//默认为空
}
