# Bucket Adapter

## Description

Bucket Adapter API is a Spring Boot application designed to provide **a unified interface for cloud storage buckets** (AWS S3, and in the future GCP, Azure, and other providers).

The application exposes a REST API that allows clients to:
- upload files
- download files
- update existing files
- delete files (single or recursive)
- list bucket contents
- check if an object exists
- generate temporary shareable URLs

The architecture is based on the **Adapter + Factory pattern**, enabling easy integration of new cloud providers without impacting the business logic.

## Getting Started

### Prerequisites

The following tools and dependencies are required:

* **Language / Runtime**
  * Java JDK 21 `openjdk 21.0.9 2025-10-21`
  * OpenJDK Runtime Environment `(Red_Hat-21.0.9.0.10-1) (build 21.0.9+10)`
  * JVM compatible with Java 21

* **Frameworks & Libraries**
  * Spring Boot 4.0.0
  * Spring Framework 7.0.1
  * AWS SDK v2 (S3, Presigner)
  * JUnit 5
  * Mockito

* **Build & Dependency Management**
  * Maven Wrapper (`./mvnw` or `mvn`)

* **IDE used**
  * Visual Studio Code with [Java extension](https://marketplace.visualstudio.com/items?itemName=redhat.java)

* **Supported OS (tested)**
  * Linux (`Fedora Linux 42 (Workstation Edition)`)

* **Cloud Providers**
  * AWS S3 (currently implemented)
  * Google Cloud Storage (planned)
  * Azure Blob Storage (planned)

* **Virtualization**
  * Docker version 29.1.3, build f52814d

---

### Configuration

#### Environment variables / system properties

The application relies on external configuration to select the storage provider and access the bucket.

1. Copy the `.env.example` file to a `.env` file using this command : `cp .env.example .env`.
2. Configure **Spring Configuration** variables in `.env` file :

```bash
# Spring Configuration
SPRING_APPLICATION_NAME=yourappname
SERVER_PORT=8080
```

#### AWS configuration

Required variables:

```bash
AWS_BUCKET_NAME=your-bucket-name
AWS_REGION=your-region
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
```

Provider selection:

```bash
PROVIDER_IMPL=AWS   # or GCP / AZURE
```

#### GCP configuration

...

#### Azure configuration

...

## Deployment

### On dev environment

#### Build the project

```bash
./mvnw clean install
```

#### Run tests

**Generate "data" folder for testing**

1. Make the script executable :

```bash
chmod +x setup-test-data.sh
```

2. Execute the script to create the **data** folder with sample files for testing :

```
./setup-test-data.sh
```

3. Run tests

```bash
./mvnw test
```

#### Run the application
```bash
./mvnw spring-boot:run
```

### On integration environment

#### Maven build

```bash
# Make sure Maven wrapper is executable
chmod +x mvnw

# Clean and compile, skip tests
./mvnw clean package -DskipTests

# (Optional) Run tests
./mvnw test
```

#### Docker build & run

```bash
# Build Docker image (multi-stage)
docker build -t bucketadapter:latest .

# Run the container exposing port 8080
docker run -d -p 8080:8080 --name bucketadapter bucketadapter:latest
```

## Directory structure

```bash
.
├── docker-compose.yml
├── Dockerfile
├── Doxyfile
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── setup-test-data.sh
├── docs                                                    # Documentatin folder
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── bucketadapter
    │   │               ├── adapter
    │   │               │   ├── BucketAdapter.java
    │   │               │   └── impl                        # Adapter implementation
    │   │               ├── BucketAdapterApplication.java
    │   │               ├── config
    │   │               ├── controller
    │   │               ├── exception
    │   │               ├── factory
    │   │               ├── model
    │   │               └── service
    │   └── resources
    │       ├── application.properties
    │       ├── static
    │       └── templates
    └── test
        └── java
            └── com
                └── example
                    └── bucketadapter
                        ├── adapter
                        │   └── impl
                        └── BucketAdapterApplicationTests.java
```

## Collaborate

### Proposing a new feature

- Create an **issue** describing the feature or bug
- Submit a **Pull Request** linked to the issue

### Commit convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

Examples :

```bash
feat: add GCP bucket adapter
fix: handle S3 presigner exception
test: add unit tests for recursive delete
```

### Git branch workflow

This projects use the [Gitflow workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)

Examples :

```bash
feature/implement-aws-s3
release/1.0.0
hotfix/fix-servor-error-on-s3-upload
```

## License

* [Choose the license adapted to your project](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository).

## Contact

For questions or contributions:

- GitHub Issues
- Pull Request discussions

For personal interactions:

- Dieperink David
- contact@daviddieperink.ch