/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.test.dunit.standalone;

import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.VM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * VersionManager loads the class-paths for all of the releases of Geode configured for
 * backward-compatibility testing in the geode-core build.gradle file.
 * <p>
 * Tests may use these versions in launching VMs to run older clients or servers.
 * 
 * @see Host#getVM(String, int)
 */
public class VersionManager {
  public static final String CURRENT_VERSION = "000";

  private static VersionManager instance;

  protected static void init() {
    instance = new VersionManager();
    instance.findVersions();
  }

  public static VersionManager getInstance() {
    if (instance == null) {
      init();
    }
    return instance;
  }

  /**
   * classPaths for old versions loaded from a file generated by Gradle
   */
  private Map<String, String> classPaths = new HashMap<>();

  private List<String> testVersions = new ArrayList<String>(10);

  private String oldestVersion;

  /**
   * Test to see if a version string is known to VersionManager. Versions are either CURRENT_VERSION
   * or one of the versions returned by VersionManager#getVersions()
   */
  public boolean isValidVersion(String version) {
    return version.equals(CURRENT_VERSION) || classPaths.containsKey(version);
  }

  /**
   * Returns true if the version is equal to the CURRENT_VERSION constant
   */
  public static boolean isCurrentVersion(String version) {
    return version.equals(CURRENT_VERSION);
  }

  /**
   * Returns the classPath for the given version, or null if the version is not valid. Use
   * CURRENT_VERSION for the version in development.
   */
  public String getClasspath(String version) {
    return classPaths.get(version);
  }

  /**
   * Returns a list of older versions available for testing
   */
  public List<String> getVersions() {
    return Collections.unmodifiableList(testVersions);
  }

  public List<String> getVersionsWithoutCurrent() {
    List<String> result = new ArrayList<>(testVersions);
    result.remove(CURRENT_VERSION);
    return result;
  }

  /**
   * returns the oldest version defined in the geodeOldVersionClasspaths.txt file
   */
  public String getOldestVersion() {
    return oldestVersion;
  }

  private void findVersions() {
    // this file is created by the gradle task createClasspathsPropertiesFile
    File propFile = new File(
        "../../../geode-old-versions/build/generated-resources/main/geodeOldVersionClasspaths.txt");
    if (!propFile.exists()) {
      // running under an IDE
      propFile = new File(
          "../geode-old-versions/build/generated-resources/main/geodeOldVersionClasspaths.txt");
    }
    String oldver = "ZZZ";
    if (propFile.exists()) {
      System.out.println("found geodeOldVersionClasspaths.txt - loading properties");
      Properties dunitProperties = loadProperties(propFile);
      for (Map.Entry<Object, Object> entry : dunitProperties.entrySet()) {
        String version = (String) entry.getKey();
        if (version.startsWith("test") && version.length() >= "test".length()) {
          if (version.equals("test")) {
            version = CURRENT_VERSION;
          } else {
            version = version.substring("test".length());
            if (version.compareTo(oldver) < 0) {
              oldver = version;
            }
          }
          classPaths.put(version, (String) entry.getValue());
          testVersions.add(version);
        }
      }
      if (oldver.equals("ZZZ")) {
        oldestVersion = CURRENT_VERSION;
      } else {
        oldestVersion = oldver;
      }
    } else {
      System.out.println(
          "WARNING: could not find geodeTestClasspaths.txt - tests will use current version");
    }
  }

  /**
   * Loads properties from a file, returning a Map object. Note: this method cannot use
   * Properties.load() because that method interprets back-slashes as escape characters, causing
   * class-paths on Windows machines to be garbled.
   */
  private Properties loadProperties(File propFile) {
    Properties props = new Properties();
    try (FileReader reader = new FileReader(propFile)) {
      props.load(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return props;
  }



}
