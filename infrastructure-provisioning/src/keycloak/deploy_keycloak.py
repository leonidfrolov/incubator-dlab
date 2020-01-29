#!/usr/bin/python

# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

import logging
from fabric.api import *
import argparse
import sys
import os
from dlab.common_lib import ensure_step
from dlab.edge_lib import install_nginx_lua

parser = argparse.ArgumentParser()
parser.add_argument('--user', type=str, default='')
parser.add_argument('--local_ip_address', type=str, default='')
parser.add_argument('--keyfile', type=str, default='')
#parser.add_argument('--gcp_subnet_name', type=str, default='')
#parser.add_argument('--service_base_name', type=str, default='')
#parser.add_argument('--gcp_vpc_name', type=str, default='')
parser.add_argument('--keycloak_realm_name', type=str, default='')
parser.add_argument('--keycloak_user', type=str, default='')
parser.add_argument('--keycloak_user_password', type=str, default='')
parser.add_argument('--step_cert_sans', type=str, default='')
args = parser.parse_args()

keycloak_version = 8.0.1
templates_dir =
if __name__ == "__main__":
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'],
                                               os.environ['project_name'],
                                               os.environ['request_id'])
    local_log_filepath = "/logs/keycloak/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    print("Configure connections")
    try:
        env['connection_attempts'] = 100
        env.key_filename = [args.keyfile]
        env.host_string = '{}@{}'.format(args.user, args.ip_address)
    except Exception as err:
        print("Failed establish connection. Excpeption: " + str(err))
        sys.exit(1)

    print("Install Java")
    ensure_jre_jdk(args.user)


    try:
        install_keycloak()
    except Exception as err:
        print("Failed keycloak install: " + str(err))
        sys.exit(1)



def install_keycloak()
    sudo('wget https://downloads.jboss.org/keycloak/' + keycloak_version + '/keycloak-' + keycloak_version + '.tar.gz')
    sudo('tar -zxvf /tmp/keycloak-' + keycloak_version + '.tar.gz -C /opt/')
    sudo('ln -s /opt/keycloak-' + keycloak_version + ' /opt/keycloak')
    sudo('chown ' + args.user + ':' + args.user + ' -R /opt/keycloak-' + keycloak_version)
    sudo('/opt/keycloak/bin/add-user-keycloak.sh -r master -u ' + args.keycloak_user + ' -p ' + args.keycloak_user_password) #create initial admin user in master realm
    put(templates_dir + 'realm.json', '/tmp/' + keycloak_realm_name + '-realm.json')
    sudo('bin/standalone.sh -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=/tmp/' + keycloak_realm_name + '-realm.json -Dkeycloak.migration.strategy=OVERWRITE_EXISTING')
    sudo('/opt/keycloak/bin/standalone.sh -b ' + args.local_ip_address)