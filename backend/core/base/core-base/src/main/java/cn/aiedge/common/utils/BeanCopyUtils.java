package cn.aiedge.common.utils;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * Bean复制工具类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class BeanCopyUtils {

    /**
     * 复制属性（忽略null值）
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyProperties(Object source, Object target) {
        org.springframework.beans.BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
    }

    /**
     * 复制属性到新对象
     *
     * @param source 源对象
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 新对象
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            org.springframework.beans.BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy properties", e);
        }
    }

    /**
     * 获取null属性名
     *
     * @param source 源对象
     * @return null属性名数组
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        return emptyNames.toArray(new String[0]);
    }

    /**
     * 复制非null属性（包含null值）
     *
     * @param source 源对象
     * @param target 目标对象
     */
    public static void copyPropertiesWithNull(Object source, Object target) {
        org.springframework.beans.BeanUtils.copyProperties(source, target);
    }
}