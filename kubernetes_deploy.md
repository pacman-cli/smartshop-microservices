# Kubernetes Deployment Guide

This guide provides step-by-step instructions for deploying the SmartShop Microservices Platform to a Kubernetes cluster (e.g., Minikube, Docker Desktop, or a cloud-based K8s).

## Prerequisites

- `kubectl` installed and configured to point to your cluster.
- `docker` installed and running.
- Maven 3.8+ and Java 17+ installed.
- (Recommended) Minikube or Docker Desktop with Kubernetes enabled.

## Step 1: Build Application Artifacts

First, compile and package all microservices into JAR files.

```bash
mvn clean package -DskipTests
```

## Step 2: Build Docker Images

If you are using **Minikube**, point your shell to Minikube's Docker daemon so the images are built directly inside the cluster:

```bash
eval $(minikube docker-env)
```

Now build the images using Docker Compose (this reads the `Dockerfile`s in each subdirectory):

```bash
docker-compose build
```

## Step 3: Deploy Configuration & Secrets

Apply the ConfigMap and Secrets first, as they are required by all other components.

```bash
kubectl apply -f k8s/config.yaml
```

*Note: The `JWT_SECRET` and database passwords in `config.yaml` are Base64 encoded. Update them for production use.*

## Step 4: Deploy Infrastructure

Deploy the backing services: PostgreSQL, Kafka, Zookeeper, Redis, and MailHog.

```bash
kubectl apply -f k8s/infrastructure.yaml
```

**Verify Infrastructure:**
Wait until all pods are `Running` and `Ready`.
```bash
kubectl get pods -w
```

## Step 5: Deploy Platform Services

Deploy the core Spring Cloud infrastructure: Discovery Server (Eureka) and Config Server.

```bash
kubectl apply -f k8s/platform.yaml
```

**Wait for Platform:**
Ensure `discovery-server` and `config-server` are healthy before proceeding.
```bash
kubectl logs -f deployment/discovery-server
```

## Step 6: Deploy Business Services

Finally, deploy the business microservices.

```bash
kubectl apply -f k8s/business-services.yaml
```

## Step 7: Verify and Access

Check the status of all deployments:

```bash
kubectl get all
```

### Accessing the API Gateway
The API Gateway is exposed via a **NodePort (30080)**.

- **Minikube**:
  ```bash
  minikube service api-gateway --url
  ```
- **Docker Desktop / Local K8s**:
  Access via `http://localhost:30080`.

### Accessing Eureka Dashboard
- Access via `http://localhost:8761` (requires port-forwarding):
  ```bash
  kubectl port-forward svc/discovery-server 8761:8761
  ```

### Accessing MailHog (Emails)
- Access via `http://localhost:8025` (requires port-forwarding):
  ```bash
  kubectl port-forward svc/mailhog 8025:8025
  ```

## Cleanup

To remove the entire deployment:

```bash
kubectl delete -f k8s/
```
