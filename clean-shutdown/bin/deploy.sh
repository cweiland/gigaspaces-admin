#!/bin/bash

# Deploy the example

. `dirname $0`/gs-setup.sh

${JSHOMEDIR}/bin/gs.sh deploy-space \
    -cluster schema=partitioned-sync2backup total_members=2,1 \
    -max-instances-per-vm 1 \
    ${MEMBER_SPACE_NAME}
