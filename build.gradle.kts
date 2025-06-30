// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  id("java")
  id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "org.intellij.sdk"
version = "2.0.3"

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2025.1.1")
  }
  implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

  // Lombok at compile time only
  compileOnly("org.projectlombok:lombok:1.18.30") // or latest version
  annotationProcessor("org.projectlombok:lombok:1.18.30")
}

intellijPlatform {
  buildSearchableOptions = false

  pluginConfiguration {
    ideaVersion {
      sinceBuild = "242"
    }
  }
  pluginVerification  {
    ides {
      recommended()
    }
  }
}
