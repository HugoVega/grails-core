/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.test.runtime

import grails.core.GrailsApplication
import grails.core.support.proxy.DefaultProxyHandler
import grails.validation.ConstraintsEvaluator
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.spring.beans.GrailsApplicationAwareBeanPostProcessor
import org.grails.spring.context.support.GrailsPlaceholderConfigurer
import org.grails.spring.context.support.MapBasedSmartPropertyOverrideConfigurer
import org.grails.transaction.TransactionManagerPostProcessor
import org.grails.validation.DefaultConstraintEvaluator
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.context.support.StaticMessageSource
import org.springframework.util.ClassUtils

/**
 * a TestPlugin for TestRuntime that adds some generic beans that are
 * required in Grails applications
 *
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
class CoreBeansTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication']
    String[] providedFeatures = ['coreBeans']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplicationParam) {
        defineBeans(runtime) {
            conversionService(ConversionServiceFactoryBean)
        }

        if(ClassUtils.isPresent("org.grails.plugins.databinding.DataBindingGrailsPlugin", CoreBeansTestPlugin.classLoader)) {
            def plugin = ClassUtils.forName("org.grails.plugins.databinding.DataBindingGrailsPlugin").newInstance()
            plugin.grailsApplication = grailsApplicationParam
            plugin.applicationContext = grailsApplicationParam.mainContext
            defineBeans(runtime, plugin.doWithSpring())
        }

        defineBeans(runtime) {
            xmlns context:"http://www.springframework.org/schema/context"
            // adds AutowiredAnnotationBeanPostProcessor, CommonAnnotationBeanPostProcessor and others
            // see org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors method
            context.'annotation-config'()

            proxyHandler(DefaultProxyHandler)
            messageSource(StaticMessageSource)
            gormConstraintEvaluator(org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator, ref("messageSource"))
            "${ConstraintsEvaluator.BEAN_NAME}"(DefaultConstraintEvaluator, ref("gormConstraintEvaluator"))
            grailsApplicationAwarePostProcessor(GrailsApplicationAwareBeanPostProcessor, grailsApplicationParam)
            transactionManagerAwarePostProcessor(TransactionManagerPostProcessor)
            grailsPlaceholderConfigurer(GrailsPlaceholderConfigurer, '${', grailsApplicationParam.config.toProperties())
            mapBasedSmartPropertyOverrideConfigurer(MapBasedSmartPropertyOverrideConfigurer) {
                grailsApplication = grailsApplicationParam
            }
        }
    }

    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }

    void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
        }
    }

    void close(TestRuntime runtime) {

    }
}
