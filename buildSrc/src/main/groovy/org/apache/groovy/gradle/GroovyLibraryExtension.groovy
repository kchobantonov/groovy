/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.gradle

import groovy.transform.CompileStatic
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.SoftwareComponentContainer
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.ManifestMergeDetails
import org.gradle.api.java.archives.ManifestMergeSpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
/**
 * Provides information about Groovy libraries
 */
@CompileStatic
class GroovyLibraryExtension {
    final SharedConfiguration sharedConfiguration
    final Property<Boolean> includeInGroovyAll
    final ListProperty<String> repackagedDependencies
    final JavaPluginConvention javaPluginConvention
    final JavaPluginExtension javaPluginExtension
    final SoftwareComponentContainer components
    final ConfigurationContainer configurations

    GroovyLibraryExtension(ObjectFactory factory,
                           SharedConfiguration sharedConfiguration,
                           JavaPluginConvention javaPluginConvention,
                           JavaPluginExtension javaPluginExtension,
                           SoftwareComponentContainer components,
                           ConfigurationContainer configurations
    ) {
        this.sharedConfiguration = sharedConfiguration
        this.includeInGroovyAll = factory.property(Boolean).convention(true)
        this.repackagedDependencies = factory.listProperty(String).convention([])
        this.javaPluginConvention = javaPluginConvention
        this.javaPluginExtension = javaPluginExtension
        this.components = components
        this.configurations = configurations
    }

    void registerOptionalFeature(String name) {
        javaPluginExtension.registerFeature(name) {
            it.usingSourceSet(javaPluginConvention.sourceSets.getByName("main"))
        }
        AdhocComponentWithVariants component = (AdhocComponentWithVariants) components.getByName("groovyLibrary")
        def apiElements = configurations.getByName("${name}ApiElements")
        apiElements.artifacts.clear()
        component.addVariantsFromConfiguration(apiElements) {
            if (it.configurationVariant.name != "${name}ApiElements") {
                it.skip()
            }
            it.mapToMavenScope("compile")
            it.mapToOptional()
        }
        def runtimeElements = configurations.getByName("${name}RuntimeElements")
        runtimeElements.artifacts.clear()
        component.addVariantsFromConfiguration(runtimeElements) {
            if (it.configurationVariant.name != "${name}RuntimeElements") {
                it.skip()
            }
            it.mapToMavenScope("runtime")
            it.mapToOptional()
        }
    }

    void configureManifest(Manifest manifest, List<String> exclusions) {
        manifest.from(createBaseManifest()) { ManifestMergeSpec spec ->
            spec.eachEntry { ManifestMergeDetails details ->
                if (exclusions.any { it == details.key }) {
                    details.exclude()
                }
            }
        }
    }

    private Manifest createBaseManifest() {
        def groovyBundleVersion = sharedConfiguration.groovyBundleVersion.get()
        javaPluginConvention.manifest { Manifest mn ->
            mn.attributes(
                    'Extension-Name': 'groovy',
                    'Specification-Title': 'Groovy: a powerful, multi-faceted language for the JVM',
                    'Specification-Version': groovyBundleVersion,
                    'Specification-Vendor': 'The Apache Software Foundation',
                    'Implementation-Title': 'Groovy: a powerful, multi-faceted language for the JVM',
                    'Implementation-Version': groovyBundleVersion,
                    'Implementation-Vendor': 'The Apache Software Foundation',
                    'Bundle-ManifestVersion': '2',
                    'Bundle-Name': 'Groovy Runtime',
                    'Bundle-Description': 'Groovy Runtime',
                    'Bundle-Version': groovyBundleVersion,
                    'Bundle-Vendor': 'The Apache Software Foundation',
                    'Bundle-ClassPath': '.',
                    'Eclipse-BuddyPolicy': 'dependent',
                    'DynamicImport-Package': '*',
                    'Main-Class': 'groovy.ui.GroovyMain')
        }

    }

}