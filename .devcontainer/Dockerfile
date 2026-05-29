FROM mcr.microsoft.com/devcontainers/java:17

# Maven 3.9.x
ARG MAVEN_VERSION=3.9.9
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  | tar -xz -C /opt && \
  ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# Docker CLI (for Docker-in-Docker dev workflows)
RUN apt-get update && apt-get install -y --no-install-recommends \
  docker.io \
  && rm -rf /var/lib/apt/lists/*

# Useful tools
RUN apt-get update && apt-get install -y --no-install-recommends \
  jq \
  curl \
  netcat-openbsd \
  && rm -rf /var/lib/apt/lists/*

# Maven settings for GitHub Packages publishing
RUN mkdir -p /root/.m2 && \
  echo '<settings>' > /root/.m2/settings.xml && \
  echo '  <servers>' >> /root/.m2/settings.xml && \
  echo '    <server>' >> /root/.m2/settings.xml && \
  echo '      <id>github</id>' >> /root/.m2/settings.xml && \
  echo '      <username>${env.GITHUB_ACTOR}</username>' >> /root/.m2/settings.xml && \
  echo '      <password>${env.GITHUB_TOKEN}</password>' >> /root/.m2/settings.xml && \
  echo '    </server>' >> /root/.m2/settings.xml && \
  echo '  </servers>' >> /root/.m2/settings.xml && \
  echo '</settings>' >> /root/.m2/settings.xml

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
