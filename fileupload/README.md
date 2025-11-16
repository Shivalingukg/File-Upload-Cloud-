# File Upload Cloud API

A Spring Boot REST API for secure file uploads and downloads with AWS S3 integration. Uses presigned URLs for secure, temporary access to files stored in S3.

## Overview

This application provides a complete file management system with the following features:

- **Secure File Upload**: Generate presigned PUT URLs for client-side uploads directly to S3
- **Secure File Download**: Generate presigned GET URLs for secure file access
- **File Tracking**: Stores file metadata (filename, size, upload time, user ID) in a local H2 database
- **File Deletion**: Delete files from S3 and remove metadata from database
- **File Verification**: Check if a file exists in S3

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 17
- **Build Tool**: Maven
- **Database**: H2 (in-memory/file-based)
- **ORM**: Spring Data JPA with Hibernate
- **Cloud Storage**: AWS S3 (AWS SDK v2)
- **API Documentation**: REST JSON

## Project Structure

```
src/
├── main/
│   ├── java/com/example/fileupload/
│   │   ├── FileUploadApiApplication.java        # Main Spring Boot application
│   │   ├── config/
│   │   │   └── AwsConfig.java                   # AWS S3 and Presigner configuration
│   │   ├── controller/
│   │   │   └── FileController.java              # REST API endpoints
│   │   ├── service/
│   │   │   ├── S3Service.java                   # AWS S3 operations (presigner, delete, etc)
│   │   │   └── FileService.java                 # Business logic for file management
│   │   ├── entity/
│   │   │   └── FileMeta.java                    # JPA entity for file metadata
│   │   ├── dto/
│   │   │   ├── PresignRequest.java              # Request DTO for presign endpoints
│   │   │   ├── PresignResponse.java             # Response DTO with presigned URL
│   │   │   └── ConfirmRequest.java              # Request DTO for confirming uploads
│   │   └── repository/
│   │       └── FileMetaRepository.java          # Spring Data JPA repository
│   └── resources/
│       ├── application.properties               # Configuration (S3 bucket, regions, etc)
│       └── index.html                           # Simple web UI
└── test/
    └── java/com/example/fileupload/
        └── FileuploadApplicationTests.java      # Basic integration tests
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **AWS Credentials** (Access Key ID and Secret Access Key)
  - Set via environment variables: `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
  - Or configure in `~/.aws/credentials`
- **AWS S3 Bucket** already created in your AWS account
- **Network Access** to Maven Central (for downloading dependencies)

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Shivalingukg/File-Upload-Cloud-.git
cd File-Upload-Cloud-/fileupload
```

### 2. Configure AWS Credentials

Set your AWS credentials as environment variables:

**Windows (PowerShell):**
```powershell
$env:AWS_ACCESS_KEY_ID = "your-access-key-id"
$env:AWS_SECRET_ACCESS_KEY = "your-secret-access-key"
```

**Windows (Command Prompt):**
```cmd
set AWS_ACCESS_KEY_ID=your-access-key-id
set AWS_SECRET_ACCESS_KEY=your-secret-access-key
```

**Linux/Mac:**
```bash
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key
```

### 3. Update Application Configuration

Edit `src/main/resources/application.properties`:

```properties
# AWS S3 Configuration
app.s3.region=us-east-1                    # Your AWS region
app.s3.bucket=my-file-bucket               # Your S3 bucket name
app.s3.presign.expireSeconds=3600          # URL expiration time in seconds

# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Database Configuration (H2)
spring.datasource.url=jdbc:h2:mem:filedb
spring.h2.console.enabled=true
```

**Available AWS Regions:** `us-east-1`, `us-west-2`, `eu-west-1`, `ap-southeast-1`, etc.

### 4. Build the Project

```bash
mvn clean package
```

**Note**: If you encounter AWS SDK dependency resolution issues, clear the Maven cache:

```powershell
# Windows PowerShell
Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\software\amazon\awssdk
mvn -U clean package
```

```bash
# Linux/Mac
rm -rf ~/.m2/repository/software/amazon/awssdk
mvn -U clean package
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or:

```bash
java -jar target/fileupload-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Base URL
```
http://localhost:8080/api
```

### 1. Generate Presigned PUT URL (Upload)

**Endpoint:** `POST /files/presign-put`

**Request:**
```json
{
  "userId": "user123",
  "filename": "document.pdf",
  "contentType": "application/pdf"
}
```

**Response:**
```json
{
  "key": "user123/1700145600000_document.pdf",
  "url": "https://my-file-bucket.s3.us-east-1.amazonaws.com/user123/1700145600000_document.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&...",
  "expiresInSeconds": 3600
}
```

**Usage:**
1. Client receives the presigned URL
2. Client uploads file using HTTP PUT request to the URL with the file as body
3. Set `Content-Type` header to match the `contentType` in the request

### 2. Confirm Upload

**Endpoint:** `POST /files/confirm-upload`

**Request:**
```json
{
  "key": "user123/1700145600000_document.pdf",
  "userId": "user123",
  "filename": "document.pdf"
}
```

**Response:**
```json
{
  "id": 1,
  "key": "user123/1700145600000_document.pdf",
  "userId": "user123",
  "filename": "document.pdf",
  "uploadedAt": "2024-11-16T12:00:00"
}
```

### 3. Generate Presigned GET URL (Download)

**Endpoint:** `POST /files/presign-get`

**Request:**
```json
{
  "key": "user123/1700145600000_document.pdf"
}
```

**Response:**
```json
{
  "key": "user123/1700145600000_document.pdf",
  "url": "https://my-file-bucket.s3.us-east-1.amazonaws.com/user123/1700145600000_document.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&...",
  "expiresInSeconds": 3600
}
```

**Usage:**
- Client receives the presigned URL
- Client can access/download the file using HTTP GET request to the URL within the expiration time

### 4. Check File Existence

**Endpoint:** `GET /files/exists/{key}`

**Example:** `GET /files/exists/user123/1700145600000_document.pdf`

**Response:**
```json
{
  "exists": true,
  "key": "user123/1700145600000_document.pdf"
}
```

### 5. Delete File

**Endpoint:** `DELETE /files/{key}`

**Example:** `DELETE /files/user123/1700145600000_document.pdf`

**Response:**
```json
{
  "message": "File deleted successfully",
  "key": "user123/1700145600000_document.pdf"
}
```

## Example Workflow

### Step 1: Get Presigned PUT URL

```bash
curl -X POST http://localhost:8080/api/files/presign-put \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "filename": "myfile.txt",
    "contentType": "text/plain"
  }'
```

### Step 2: Upload File Using Presigned URL

```bash
# Save the URL from step 1 to a variable
$URL = "https://my-file-bucket.s3.us-east-1.amazonaws.com/..."

curl -X PUT "$URL" \
  -H "Content-Type: text/plain" \
  --data-binary "@myfile.txt"
```

### Step 3: Confirm Upload

```bash
curl -X POST http://localhost:8080/api/files/confirm-upload \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user123/1700145600000_myfile.txt",
    "userId": "user123",
    "filename": "myfile.txt"
  }'
```

### Step 4: Get Presigned GET URL

```bash
curl -X POST http://localhost:8080/api/files/presign-get \
  -H "Content-Type: application/json" \
  -d '{
    "key": "user123/1700145600000_myfile.txt"
  }'
```

### Step 5: Download File Using Presigned URL

```bash
curl -X GET "https://my-file-bucket.s3.us-east-1.amazonaws.com/..." \
  -o myfile.txt
```

## Database Schema

### FileMeta Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-generated) |
| key | VARCHAR | S3 object key (unique identifier) |
| user_id | VARCHAR | User who uploaded the file |
| filename | VARCHAR | Original filename |
| uploaded_at | TIMESTAMP | Upload timestamp |

## Configuration Details

### application.properties

```properties
# AWS Configuration
app.s3.region=us-east-1
app.s3.bucket=my-bucket
app.s3.presign.expireSeconds=3600

# Server
server.port=8080
server.servlet.context-path=/api

# Spring Data JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# H2 Database
spring.datasource.url=jdbc:h2:mem:filedb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Access H2 Console at: `http://localhost:8080/h2-console`

## Dependencies

Main dependencies managed by Spring Boot 3.5.7:

- **spring-boot-starter-web**: REST API support
- **spring-boot-starter-data-jpa**: Database ORM
- **h2**: In-memory/file-based database
- **spring-boot-starter-validation**: Bean validation
- **aws-sdk-java-core**: AWS SDK core
- **s3**: AWS S3 client
- **s3-presigner**: AWS S3 presigner (for pre-signed URLs)
- **lombok**: Reduce boilerplate code
- **spring-boot-starter-test**: Testing framework

See `pom.xml` for full dependency list.

## Error Handling

### Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| AWS credentials not found | Missing AWS credentials | Set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` environment variables |
| S3 bucket not found | Bucket doesn't exist or wrong region | Verify bucket name and region in `application.properties` |
| Presigned URL expired | URL expired before client used it | Increase `app.s3.presign.expireSeconds` or generate new URL |
| File not found in S3 | File deleted or wrong key | Check file exists using `/files/exists/{key}` endpoint |
| Build failure - AWS SDK artifacts not found | Maven Central network issue or version mismatch | Clear Maven cache: `mvn -U clean package` |

## Testing

Run unit tests:

```bash
mvn test
```

Run integration tests:

```bash
mvn verify
```

## Deployment

### Build JAR

```bash
mvn clean package
```

Output: `target/fileupload-0.0.1-SNAPSHOT.jar`

### Docker (Optional)

Create `Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/fileupload-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
docker build -t file-upload-api .
docker run -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID=xxx \
  -e AWS_SECRET_ACCESS_KEY=xxx \
  -e APP_S3_BUCKET=my-bucket \
  -e APP_S3_REGION=us-east-1 \
  file-upload-api
```

### AWS Deployment (Elastic Beanstalk / App Service)

1. Build JAR: `mvn clean package`
2. Deploy JAR to your cloud provider
3. Set environment variables for AWS credentials and S3 configuration
4. Ensure security group allows inbound traffic on port 8080

## Security Considerations

- **Presigned URLs**: URLs are time-limited (default 1 hour). Adjust `app.s3.presign.expireSeconds` based on your needs.
- **AWS Credentials**: Never commit credentials to version control. Use environment variables or AWS credential files.
- **S3 Bucket Policy**: Configure bucket policies to restrict access to authorized users only.
- **HTTPS**: Deploy behind HTTPS in production.
- **Access Control**: Implement authentication/authorization in the API controller if needed.

## Performance Notes

- **Presigned URLs**: Generated server-side; clients upload/download directly to/from S3 (reduces server load)
- **Database**: H2 is suitable for development/testing. Use PostgreSQL/MySQL for production.
- **File Metadata**: Stored in database for tracking; actual file data stored in S3.
- **Large Files**: S3 handles large files efficiently; presigned URLs support multipart uploads.

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit changes: `git commit -am 'Add your feature'`
4. Push to branch: `git push origin feature/your-feature`
5. Submit a pull request

## License

This project is open source and available under the MIT License.

## Support & Issues

For issues, questions, or feature requests, please open an issue on GitHub:
[File-Upload-Cloud- Issues](https://github.com/Shivalingukg/File-Upload-Cloud-/issues)

## Author

Shivalingukg

## Changelog

### Version 0.0.1-SNAPSHOT
- Initial release
- Presigned URL generation for PUT and GET operations
- File metadata tracking with JPA
- AWS S3 integration with presigner
- REST API endpoints for file management
- H2 database for metadata storage
