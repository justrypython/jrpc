/*
 * Copyright (C) 2014~2016 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dinstone.jrpc.spring;

import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.dinstone.jrpc.spring.factory.ConfigBean;
import com.dinstone.jrpc.spring.factory.RegistryBean;
import com.dinstone.jrpc.spring.factory.TransportBean;

public class JrpcBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    private AtomicInteger count = new AtomicInteger();

    protected Class<?> factoryBeanClass;

    protected Class<?> beanClass;

    public JrpcBeanDefinitionParser(Class<?> factoryBeanClass, Class<?> beanClass) {
        this.factoryBeanClass = factoryBeanClass;
        this.beanClass = beanClass;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return factoryBeanClass;
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String id = element.getAttribute("id");
        if (!StringUtils.hasText(id)) {
            id = beanClass.getSimpleName() + "-" + count.incrementAndGet();
            builder.addPropertyValue("id", id);
            element.setAttribute("id", id);
        } else {
            builder.addPropertyValue("id", id);
        }

        String name = element.getAttribute("name");
        if (!StringUtils.hasText(name)) {
            String[] pidName = ManagementFactory.getRuntimeMXBean().getName().split("@");
            builder.addPropertyValue("name", pidName[1] + ":" + pidName[0]);
        } else {
            builder.addPropertyValue("name", name);
        }

        builder.addPropertyValue("transportBean", getConfigBeanDefinition(element, parserContext, "transport"));

        builder.addPropertyValue("registryBean", getConfigBeanDefinition(element, parserContext, "registry"));
    }

    private BeanDefinition getConfigBeanDefinition(Element element, ParserContext parserContext, String configName) {
        BeanDefinitionBuilder sbd = BeanDefinitionBuilder.genericBeanDefinition(ConfigBean.class);

        Element configElement = getChildElement(element, configName);
        if (configElement != null) {
            String schema = configElement.getAttribute("schema");
            sbd.addPropertyValue("schema", schema);
            String address = configElement.getAttribute("address");
            sbd.addPropertyValue("address", address);

            NodeList propertyList = configElement.getElementsByTagName("property");
            Properties properties = new Properties();
            for (int i = 0; i < propertyList.getLength(); i++) {
                Node propertyNode = propertyList.item(i);
                Element pe = (Element) propertyNode;
                properties.put(pe.getAttribute("key"), pe.getAttribute("value"));
            }
            sbd.addPropertyValue("properties", properties);
        }

        return sbd.getBeanDefinition();
    }

    private BeanDefinition getRegistryBeanDefinition(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder sbd = BeanDefinitionBuilder.genericBeanDefinition(RegistryBean.class);

        Element registry = getChildElement(element, "registry");
        if (registry != null) {
            String schema = registry.getAttribute("schema");
            sbd.addPropertyValue("schema", schema);

            String address = registry.getAttribute("address");
            sbd.addPropertyValue("address", address);

            String basePath = registry.getAttribute("basePath");
            if (StringUtils.hasText(basePath)) {
                sbd.addPropertyValue("basePath", basePath);
            }
        }

        return sbd.getBeanDefinition();
    }

    protected BeanDefinition getTransportBeanDefinition(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder sbd = BeanDefinitionBuilder.genericBeanDefinition(TransportBean.class);
        String address = element.getAttribute("address");
        if (StringUtils.hasText(address)) {
            sbd.addPropertyValue("address", address);
        }

        String host = element.getAttribute("host");
        if (StringUtils.hasText(host)) {
            sbd.addPropertyValue("host", host);
        }

        String port = element.getAttribute("port");
        if (StringUtils.hasText(port)) {
            sbd.addPropertyValue("port", port);
        }

        String transport = element.getAttribute("transport");
        if (StringUtils.hasText(transport)) {
            sbd.addPropertyValue("type", transport);
        }

        return sbd.getBeanDefinition();
    }

    public static Element getChildElement(Element ele, String childName) {
        NodeList nl = ele.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element && nodeMatch(node, childName)) {
                return (Element) node;
            }
        }
        return null;
    }

    private static boolean nodeMatch(Node node, String desiredName) {
        return (desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName()));
    }

}