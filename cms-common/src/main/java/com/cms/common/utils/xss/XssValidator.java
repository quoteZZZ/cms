package com.cms.common.utils.xss;

import com.cms.common.annotation.Xss;
import com.cms.common.utils.StringUtils;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义xss校验注解实现类：
 * 实现自定义xss校验注解，用于校验用户提交内容是否含有xss字符
 * @author quoteZZZ
 */
public class XssValidator implements ConstraintValidator<Xss, String>
{
    // 匹配HTML标签的正则表达式
    private static final String HTML_PATTERN = "<(\\S*?)[^>]*>.*?|<.*? />";

    // 校验方法
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext)
    {
        if (StringUtils.isBlank(value))
        {
            return true;
        }
        return !containsHtml(value);
    }

    // 判断是否包含HTML标签
    public static boolean containsHtml(String value)
    {
        StringBuilder sHtml = new StringBuilder();
        Pattern pattern = Pattern.compile(HTML_PATTERN);
        Matcher matcher = pattern.matcher(value);
        while (matcher.find())
        {
            sHtml.append(matcher.group());
        }
        return pattern.matcher(sHtml).matches();
    }
}