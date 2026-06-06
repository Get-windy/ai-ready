package cn.aiedge.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;

/**
 * 使用全限定类名作为Bean名称的生成器，避免同名类在不同包中的冲突
 */
public class FullyQualifiedBeanNameGenerator implements BeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String beanClassName = definition.getBeanClassName();
        if (beanClassName == null) {
            // Fallback for factory-method-based beans
            return definition.getFactoryBeanName() + "." + definition.getFactoryMethodName();
        }
        // Use fully qualified class name, replace dots with underscores for valid bean names
        return beanClassName.replace('.', '_');
    }
}
