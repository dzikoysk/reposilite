---
id: kubernetes
title: Kubernetes
---

You may use the official Helm chart to install Reposilite on Kubernetes.

Requirements: `Kubernetes: 1.14+`, [`Helm: v3.x+`](https://helm.sh/docs/intro/install/)

### Adding the Reposilite chart repository
Before you can use the Reposilite chart, you need to add the chart repository to Helm:
```bash
# Add the repository
$ helm repo add reposilite https://helm.reposilite.com/

# Update local repository information
$ helm repo update
```

### Basic installation
Basic installation without changing any of the default chart values.  
**By default, the Helm chart will use 4 GB of disk and 32 MB of RAM, to increase or decrease these settings, see [Installing with custom values](#installing-with-custom-values).**

```bash
# Create the 'reposilite' namespace
$ kubectl create namespace reposilite

# Install the Helm chart into the namespace 'reposilite'
$ helm install reposilite reposilite/reposilite -n reposilite
```

### Installing with custom values
Advanced installation with custom chart values (recommended).
```bash
# Save the default chart values to 'values.yaml'
$ helm show values reposilite/reposilite > values.yaml

# Modify the chart values ('values.yaml') using VI (or your preferred text editor)
$ vi values.yaml

# Create the 'reposilite' namespace
$ kubectl create namespace reposilite

# Install the Helm chart into the namespace 'reposilite', using the custom values
$ helm install reposilite reposilite/reposilite -n reposilite -f values.yaml
```

### Accessing Reposilite
After installing the Reposilite Helm chart, depending on whether you configured the `ingress` using
custom chart values, you may be unable to access the Reposilite frontend.

As Kubernetes ingress is generally different depending on the Ingress controller you are using, we are
unable to document every way to create an ingress, but here are basic examples for two popular ingress controllers.

#### Nginx
Using the [Nginx Ingress Controller](https://github.com/kubernetes/ingress-nginx).
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: reposilite-ingress
  namespace: reposilite
spec:
  rules:
  - host: repo.example.com # Replace the domain you wish to use.
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: reposilite
            port:
              name: http
```

#### Traefik
Using [Traefik](https://traefik.io/traefik/).
```yaml
apiVersion: traefik.containo.us/v1alpha1
kind: IngressRoute
metadata:
  name: reposilite-ingress
  namespace: reposilite
spec:
  entryPoints:
    - web # Replace this with the entrypoint you wish to expose Reposilite on.
  routes:
    - match: Host(`repo.example.com`) # Replace the domain you wish to use.
      kind: Rule
      services:
        - name: reposilite
          port: http
```