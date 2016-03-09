#!/bin/bash

DOCKER_NETWORK=java_code_examples_it_network_$$
AMQP_HOST=ecag-fixml-dev1
AMQP_CONTAINER_NAME=ecag-fixml-dev1-$$
JAVA_CONTAINER_NAME=java-code-examples-host-$$
JAVA_HOST=java-code-examples-host
TMP_DIR=
SUDO=
SKIP_MVN_SETTINGS=false
REPORTS_DIR=
QPIDD_IMAGE_VERSION="fixml:sim"
MVN_IMAGE_VERSION="3-jdk-7"
RESULTS_MSG="RESULTS:\n"

trap "stop_and_remove_left_containers && exit 1" SIGINT SIGTERM

function stop_and_remove_left_containers() {
    for container in $(${SUDO} docker ps -a --filter "status=running" --format "{{.Names}}") ; do
        if [ ${container} == ${AMQP_CONTAINER_NAME} ] || [ ${container} == ${JAVA_CONTAINER_NAME} ] ; then
            echo "Stopping container: ${container}"
            ${SUDO} docker stop ${container}
        fi
    done
    for container in $(${SUDO} docker ps -a --filter "status=exited" --format "{{.Names}}") ; do
        if [ ${container} == ${AMQP_CONTAINER_NAME} ] || [ ${container} == ${JAVA_CONTAINER_NAME} ] ; then
            echo "Removing container: ${container}"
            ${SUDO} docker rm ${container}
        fi
    done
    ${SUDO} docker network rm ${DOCKER_NETWORK}
}

function print_help() {
    local MY_NAME="$(basename $0 .sh)"
    echo "Usage: ${MY_NAME}.sh [OPTION]..."
    echo ""
    echo " optional"
    echo ""
    echo "  --qpidd-version=VERSION  Use specific Qpidd image version (default: ${QPIDD_IMAGE_VERSION})"
    echo "  --mvn-version=VERSION    Use specific Maven Docker image version (e.g. 3-jdk-8)"
    echo "  --use-sudo               Execute every docker command under sudo"
    echo "  --skip-mvn-settings      Maven settings will be skipped (default: ${SKIP_MVN_SETTINGS})"
    echo "  --copy-reports-to=DIR    Copy failsafe-reports directory into the DIR"
    echo "  --help, -h, -?           Print this help and exit"
}

function extract_parameter_value_from_string {
    echo "${1#*=}"
    return 0
}

function parse_cmdline_parameters() {
    for i in "$@" ; do
        case $i in
        --mrg-version=*)
            QPIDD_IMAGE_VERSION=$(extract_parameter_value_from_string $1);;
        --mvn-version=*)
            MVN_IMAGE_VERSION=$(extract_parameter_value_from_string $1);;
        --use-sudo)
            SUDO="sudo";;
        --skip-mvn-settings)
            SKIP_MVN_SETTINGS="true";;
        --copy-reports-to=*)
            REPORTS_DIR=$(extract_parameter_value_from_string $1);;
        --help | -h | -?)
            print_help; exit 0;;
        "");;
        *)
            echo "Unknown parameter '$i'";
            exit 2;;
        esac
        shift
    done
}

function startup() {
    TMP_DIR=$(mktemp -d)
}

function create_network() {
    ${SUDO} docker network create --subnet=192.168.0.0/16 --driver bridge ${DOCKER_NETWORK}
}

# param: $1 - image version
function start_qpidd_container() {
    ${SUDO} docker run -d --net=${DOCKER_NETWORK} --name=${AMQP_CONTAINER_NAME} --hostname=${AMQP_HOST} scholzj/$1
    RESULTS_MSG+="MRG: $1, "
}

# param: $1 - image version
function start_tests_container() {
    local AMQP_CONTAINER_NAME_IP_ADDRESS=$(${SUDO} docker inspect --format "{{ .NetworkSettings.Networks.${DOCKER_NETWORK}.IPAddress }}" ${AMQP_CONTAINER_NAME})
    ${SUDO} docker run -d -it --net=${DOCKER_NETWORK} --add-host ${AMQP_HOST}:${AMQP_CONTAINER_NAME_IP_ADDRESS} --name=${JAVA_CONTAINER_NAME} --hostname=${JAVA_HOST} maven:$1 bash
    RESULTS_MSG+="MAVEN IMAGE: $1, "
}

# create and copy maven's settings.xml file with proxy settings into the docker image
function prepare_maven_on_container() {
    [ "${SKIP_MVN_SETTINGS}" == "true" ] && return 0
    cat > ${TMP_DIR}/settings.xml <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                  http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <proxies>
    <proxy>
      <active>true</active>
      <protocol>http</protocol>
      <host>webproxy.deutsche-boerse.de</host>
      <port>8080</port>
      <nonProxyHosts>cmqaart.deutsche-boerse.de</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
EOF
    ${SUDO} docker cp ${TMP_DIR}/settings.xml ${JAVA_CONTAINER_NAME}:/root/.m2/
}

# get source code into the docker container
function prepare_sources_on_container() {
    ${SUDO} docker exec ${JAVA_CONTAINER_NAME} bash -c "cd && git clone https://github.com/Eurex-Clearing-Messaging-Interfaces/Java-Code-Examples.git java-code-examples"
    #${SUDO} docker cp /home/zeromic/src/java-code-examples ${JAVA_CONTAINER_NAME}:/root
}

function execute_tests() {
    ${SUDO} docker exec ${JAVA_CONTAINER_NAME} bash -c "cd && cd java-code-examples && mvn -P integration-test verify"
    #${SUDO} docker attach ${JAVA_CONTAINER_NAME}
    local RETURN_CODE=$?
    if [ ${RETURN_CODE} -eq 0 ] ; then
        RESULTS_MSG+=" RESULT: SUCCESS\n"
    else
        RESULTS_MSG+=" RESULT: FAILURE\n"
    fi
}

function cleanup() {
    rm -rf ${TMP_DIR}
    ${SUDO} docker stop ${AMQP_CONTAINER_NAME}
    ${SUDO} docker rm ${AMQP_CONTAINER_NAME}
    ${SUDO} docker stop ${JAVA_CONTAINER_NAME}
    ${SUDO} docker rm ${JAVA_CONTAINER_NAME}
    ${SUDO} docker network rm ${DOCKER_NETWORK}
}

# params: $1 - QPID container id, $2 - Maven container id
function execute_single_run() {
    local QPIDD_CONTAINER_VERSION=$1
    local MAVEN_CONTAINER_VERSION=$2
    startup
    create_network && \
    start_qpidd_container ${QPIDD_CONTAINER_VERSION} && \
    start_tests_container ${MAVEN_CONTAINER_VERSION} && \
    prepare_maven_on_container && \
    prepare_sources_on_container && \
    execute_tests
    copy_results
    cleanup
}

function execute_all_runs() {
    execute_single_run ${QPIDD_IMAGE_VERSION} ${MVN_IMAGE_VERSION}
}

function print_results() {
    echo -e ${RESULTS_MSG}
}

function copy_results() {
    if [ ! -z "${REPORTS_DIR}" ] ; then
        ${SUDO} docker cp ${JAVA_CONTAINER_NAME}:/root/wmq-bridge/target/failsafe-reports/ ${REPORTS_DIR}
    fi
}

parse_cmdline_parameters "$@"
execute_all_runs
print_results
