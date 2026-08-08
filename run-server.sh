#!/bin/bash
# Find the generated jar and run it
JAR_PATH=$(ls /home/haison/Stirling-PDF/app/core/build/libs/stirling-pdf-*.jar | head -n 1)
# Find Java downloaded by Gradle toolchains, fallback to system Java
JAVA_CMD=$(find /home/haison/.gradle/jdks -name java -type f -executable | head -n 1)
if [ -z "$JAVA_CMD" ]; then
    JAVA_CMD=/usr/bin/java
fi
exec "$JAVA_CMD" -jar "$JAR_PATH" --spring.profiles.active=dev,security
