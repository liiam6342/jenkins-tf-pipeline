# Use latest Jenkins Image
FROM jenkins/jenkins:2.332.3-lts

# Skips the setup wizard when first launching Jenkins 
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

USER root
RUN apt-get update && \
  apt-get install -y \
  zip \
  unzip \
  graphviz \
  python3-pip \
  apt-transport-https \
  software-properties-common \
  jq \
  wget \
  curl

# Install Terraform 
RUN apt-get install unzip
RUN wget https://releases.hashicorp.com/terraform/1.3.0/terraform_1.3.0_linux_amd64.zip
RUN unzip terraform_1.3.0_linux_amd64.zip
RUN mv terraform /usr/local/bin/
RUN terraform --version

# INSTALL AWS CLI
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
  unzip awscliv2.zip && \
  ./aws/install

# INSTALL TFLINT
RUN curl -s https://raw.githubusercontent.com/terraform-linters/tflint/master/install_linux.sh | bash

# INSTALL TFENV
RUN pip3 install "checkov<3.0.0"

RUN mkdir .tfenv && \
  git clone https://github.com/tfutils/tfenv.git ~/.tfenv

ENV PATH /home/jenkins/.tfenv/bin:$PATH
ENV PATH /home/jenkins/.local/bin:$PATH

# Copies list of plugins to container and runs install script
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt