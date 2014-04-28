#!/bin/bash

# GigaSpaces XAP setup

DEFAULT_JSHOMEDIR=/usr/local/gigaspaces-xap-premium-9.6.2-ga
if [ -z "$JSHOMEDIR" ]; then
  export JSHOMEDIR=$DEFAULT_JSHOMEDIR
fi  
export GS_HOME=${JSHOMEDIR}

export PATH=${JSHOMEDIR}/bin:${PATH}

export NIC_ADDR=127.0.0.1
export LOOKUPLOCATORS=${NIC_ADDR}
export LOOKUPGROUPS="clean-shutdown"

export EXAMPLE_SPACE_NAME="clean-shutdown-space"

${JSHOMEDIR}/bin/setenv.sh

