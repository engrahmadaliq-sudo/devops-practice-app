pipeline {
    agent none

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        DOCKERHUB_USER = "admin"
        IMAGE_NAME     = "devops-practice-app"
        IMAGE_TAG      = "${env.BUILD_NUMBER}"
        FULL_IMAGE     = "${DOCKERHUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
        DOCKERHUB_CREDS = credentials('dockerhub-creds')
    }

    stages {

        stage('Checkout') {
            agent any
            steps {
                echo 'Pulling source code from GitHub...'
                checkout scm
                stash name: 'source', includes: '**'
            }
        }

        stage('Build') {
            agent { docker { image 'maven:3.9-eclipse-temurin-17' } }
            steps {
                unstash 'source'
                echo 'Building the application with Maven...'
                sh 'mvn -B clean compile'
                stash name: 'build-output', includes: 'target/**'
            }
        }

        stage('Test') {
            agent { docker { image 'maven:3.9-eclipse-temurin-17' } }
            steps {
                unstash 'source'
                echo 'Running unit tests...'
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            agent { docker { image 'maven:3.9-eclipse-temurin-17' } }
            steps {
                unstash 'source'
                echo 'Packaging JAR file...'
                sh 'mvn -B package -DskipTests'
                stash name: 'jar', includes: 'target/*.jar'
            }
        }

        stage('Docker Build') {
            agent any
            steps {
                unstash 'source'
                unstash 'jar'
                echo "Building Docker image: ${FULL_IMAGE}"
                sh """
                    docker build -t ${FULL_IMAGE} -t ${DOCKERHUB_USER}/${IMAGE_NAME}:latest .
                """
            }
        }

        stage('Docker Push') {
            agent any
            steps {
                echo 'Pushing image to Docker Hub...'
                sh '''
                    echo "$DOCKERHUB_CREDS_PSW" | docker login -u "$DOCKERHUB_CREDS_USR" --password-stdin
                    docker push ${FULL_IMAGE}
                    docker push ${DOCKERHUB_USER}/${IMAGE_NAME}:latest
                    docker logout || true
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            agent any
            steps {
                unstash 'source'
                echo 'Deploying to Kubernetes cluster...'
                sh '''
                    sed -i "s|IMAGE_PLACEHOLDER|${FULL_IMAGE}|g" k8s/deployment.yaml
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml
                    kubectl rollout status deployment/devops-practice-app --timeout=120s
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
            echo 'Pipeline finished.'
        }
    }
}
