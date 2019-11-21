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

//data "helm_repository" "smallstep" {
//  name = "smallstep"
//  url  = "https://smallstep.github.io/helm-charts/"
//}

data "template_file" "step_ca_values" {
  template = file("./modules/helm_charts/step-ca-chart/values.yaml")
  vars = {
    step_ca_password             = random_string.step_ca_password.result
    step_ca_provisioner_password = random_string.step_ca_provisioner_password.result
    step_ca_host                 = data.kubernetes_service.nginx_service.load_balancer_ingress.0.ip
  }
}

resource "helm_release" "step_ca" {
  name       = "step-certificates"
  chart      = "./modules/helm_charts/step-ca-chart"
  namespace  = kubernetes_namespace.dlab-namespace.metadata[0].name
  depends_on = [null_resource.cert_manager_delay]
  wait       = false
  timeout    = 600

  values     = [
    data.template_file.step_ca_values.rendered
  ]
}

resource "kubernetes_ingress" "step_ca_ingress" {
  metadata {
    name        = "step-ca"
    namespace   = kubernetes_namespace.dlab-namespace.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class": "nginx"
      "nginx.ingress.kubernetes.io/ssl-redirect": "false"
      "nginx.ingress.kubernetes.io/rewrite-target": "/step"
    }
  }

  spec {
    backend {
      service_name = helm_release.step_ca.name
      service_port = 80
    }

    rule {
      http {
        path {
          backend {
            service_name = helm_release.step_ca.name
            service_port = 80
          }

          path = "/step"
        }
      }
    }
  }
  depends_on = [helm_release.step_ca]
}

resource "null_resource" "step_ca_delay" {
  provisioner "local-exec" {
    command = "sleep 120"
  }
  triggers = {
    "before" = helm_release.step_ca.name
  }
}