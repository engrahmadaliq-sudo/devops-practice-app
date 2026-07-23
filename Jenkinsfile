pipeline {
    agent any

    environment {
        // Change DOCKERHUB_USER to your own Docker Hub username
        DOCKERHUB_USER   = "yourdockerhubuser"
        IMAGE_NAME       = "devops-practice-app"
        IMAGE_TAG        = "${env.BUILD_NUMBER}"
        FULL_IMAGE       = "${DOCKERHUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
        DOCKERHUB_CREDS  = credentials('dockerhub-creds')   // Jenkins credential ID
        KUBECONFIG_CREDS = credentials('kubeconfig-creds')  // Jenkins credential ID (optional)
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Pulling source code from GitHub...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the application with Maven...'
                sh 'mvn -B clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging JAR file...'
                sh 'mvn -B package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                echo "Building Docker image: ${FULL_IMAGE}"
                sh "docker build -t ${FULL_IMAGE} -t ${DOCKERHUB_USER}/${IMAGE_NAME}:latest ."
            }
        }

        stage('Docker Push') {
            steps {
                echo 'Pushing image to Docker Hub...'
                sh '''
                    echo "$DOCKERHUB_CREDS_PSW" | docker login -u "$DOCKERHUB_CREDS_USR" --password-stdin
                    docker push ${FULL_IMAGE}
                    docker push ${DOCKERHUB_USER}/${IMAGE_NAME}:latest
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo 'Deploying to Kubernetes cluster...'
                sh '''
                    sed -i "s|IMAGE_PLACEHOLDER|${FULL_IMAGE}|g" k8s/deployment.yaml
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml
                    kubectl rollout status deployment/devops-practice-app
                '''
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed. Check logs above.'
        }
        always {
            sh 'docker logout || true'
        }
    }
}
