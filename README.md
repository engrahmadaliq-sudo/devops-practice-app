# DevOps Practice Project — Java App with Full CI/CD Pipeline

A small Spring Boot REST app packaged so you can practice the entire DevOps
lifecycle end-to-end from a Linux terminal:

**GitHub → Jenkins → Docker → Kubernetes → Prometheus/Grafana**

---

## 0. Prerequisites (install once)

```bash
# Java 17 + Maven
sudo apt update
sudo apt install -y openjdk-17-jdk maven

# Git
sudo apt install -y git

# Docker
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER   # log out/in after this

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# A local Kubernetes cluster (pick ONE)
# Option A: Minikube (easiest for practice)
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
minikube start --driver=docker

# Option B: kind
# curl -Lo ./kind https://kind.sigs.k8s.io/dl/latest/kind-linux-amd64
# chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind
# kind create cluster

# Jenkins (runs in Docker, easiest for practice)
docker run -d --name jenkins \
  -p 8081:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

Get the Jenkins unlock password:
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Then open `http://localhost:8081` in your browser, paste it, install suggested
plugins, and create an admin user. Also install the **Docker Pipeline** and
**Kubernetes CLI** plugins from Manage Jenkins → Plugins.

---

## 1. Build and run the app locally (sanity check)

```bash
cd devops-practice-app
mvn clean package
java -jar target/devops-practice-app.jar
```
Visit `http://localhost:8080/` — you should see a JSON greeting.
Check metrics: `http://localhost:8080/actuator/prometheus`

Stop it with `Ctrl+C`.

---

## 2. Push the project to GitHub

```bash
git init
git add .
git commit -m "Initial commit: Java app with Docker, K8s, Jenkins, monitoring"

# Create a repo on GitHub first (via github.com or `gh repo create`), then:
git remote add origin https://github.com/<your-username>/devops-practice-app.git
git branch -M main
git push -u origin main
```

---

## 3. Build and test the Docker image

```bash
docker build -t devops-practice-app:1.0 .
docker run -d --name devops-practice-app -p 8080:8080 devops-practice-app:1.0
curl http://localhost:8080/
docker logs devops-practice-app
docker stop devops-practice-app && docker rm devops-practice-app
```

Push it to Docker Hub (optional, needed for Jenkins → K8s flow):
```bash
docker login
docker tag devops-practice-app:1.0 <your-dockerhub-user>/devops-practice-app:1.0
docker push <your-dockerhub-user>/devops-practice-app:1.0
```

---

## 4. Set up the Jenkins pipeline

1. In Jenkins, add credentials (Manage Jenkins → Credentials):
   - `dockerhub-creds` — your Docker Hub username/password (Username with password)
   - `kubeconfig-creds` — (optional) your kubeconfig file, if Jenkins needs to deploy to K8s
2. New Item → Pipeline → name it `devops-practice-app`.
3. Under **Pipeline**, choose "Pipeline script from SCM" → Git → paste your
   GitHub repo URL → branch `main` → script path `Jenkinsfile`.
4. Edit `Jenkinsfile` in your repo and change `DOCKERHUB_USER` to your actual
   Docker Hub username, commit, and push.
5. Click **Build Now**. Jenkins will: checkout → build → test → package →
   docker build → docker push → kubectl deploy.

Note: since Jenkins runs in a container, give it access to `kubectl` and your
cluster context, or run the "Deploy to Kubernetes" stage on an agent that has
`kubectl` configured against your Minikube/kind cluster.

---

## 5. Deploy manually to Kubernetes (without Jenkins, for practice)

```bash
# point deployment.yaml at an image you built/pushed
sed -i "s|IMAGE_PLACEHOLDER|<your-dockerhub-user>/devops-practice-app:1.0|" k8s/deployment.yaml

kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

kubectl get pods
kubectl get svc

# Access the app (Minikube)
minikube service devops-practice-app-service --url
```

Useful checks:
```bash
kubectl describe deployment devops-practice-app
kubectl logs -l app=devops-practice-app
kubectl rollout restart deployment/devops-practice-app
kubectl scale deployment/devops-practice-app --replicas=3
```

---

## 6. Monitoring with Prometheus + Grafana

```bash
cd monitoring
docker compose -f docker-compose-monitoring.yml up -d
```

- Prometheus: `http://localhost:9090` — check **Status → Targets** to confirm
  it's scraping `devops-practice-app`.
- Grafana: `http://localhost:3000` (login: `admin` / `admin`) — Prometheus
  datasource is pre-configured. Create a dashboard and add panels for e.g.
  `http_server_requests_seconds_count` or `jvm_memory_used_bytes`.

If your app is running inside Kubernetes instead of directly on the host,
point Prometheus at the cluster service address instead of
`host.docker.internal`, or install `kube-prometheus-stack` via Helm for a
production-style setup:
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install monitoring prometheus-community/kube-prometheus-stack
```

---

## 7. Suggested practice order

1. Run locally with Maven ✅
2. Push to GitHub ✅
3. Build/run with Docker ✅
4. Wire up Jenkins pipeline (manual build first, then trigger via GitHub webhook) ✅
5. Deploy to Kubernetes manually, then let Jenkins do it ✅
6. Add Prometheus + Grafana, watch metrics while you `kubectl scale` up/down ✅
7. Break something on purpose (e.g. bad image tag) and practice rollback:
   ```bash
   kubectl rollout undo deployment/devops-practice-app
   ```

---

## Project structure

```
devops-practice-app/
├── src/main/java/com/example/demo/   # App source code
├── src/test/java/com/example/demo/   # Unit test
├── pom.xml                           # Maven build file
├── Dockerfile                        # Multi-stage Docker build
├── .dockerignore
├── .gitignore
├── Jenkinsfile                       # CI/CD pipeline definition
├── k8s/
│   ├── deployment.yaml
│   └── service.yaml
├── monitoring/
│   ├── prometheus.yml
│   ├── docker-compose-monitoring.yml
│   └── grafana/provisioning/datasources/datasource.yml
└── README.md
```
