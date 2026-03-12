package com.sdet.performance;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AWSS3Uploader - Archives performance test reports to AWS S3
 *
 * OCC Relevance:
 * - JD explicitly requires AWS S3 knowledge
 * - OCC needs permanent, retrievable audit evidence
 * - Organized by environment/date for easy retrieval
 * - Supports compliance and regulatory review requirements
 *
 * S3 Folder Structure:
 * performance-reports/
 *   └── {environment}/
 *       └── {YYYY-MM-DD}/
 *           ├── Load_Test_20240315_143022.jtl
 *           ├── Load_Test_20240315_143022_summary.txt
 *           └── Stress_Test_20240315_150000.jtl
 */
public class AWSS3Uploader {

    private static final Logger log = LogManager.getLogger(AWSS3Uploader.class);

    // S3 Configuration — read from environment variables (CI/CD friendly)
    private static final String BUCKET_NAME = System.getProperty(
            "s3.bucket",
            System.getenv().getOrDefault("S3_BUCKET_NAME", "sdet-performance-reports")
    );

    private static final String AWS_REGION = System.getProperty(
            "aws.region",
            System.getenv().getOrDefault("AWS_REGION", "us-east-1")
    );

    private static final String BASE_PREFIX = "performance-reports";

    private final S3Client s3Client;
    private final String environment;
    private final List<String> uploadedKeys = new ArrayList<>();

    /**
     * Constructor using AWS Default Credentials Provider Chain
     * Automatically picks up credentials from:
     * 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * 2. ~/.aws/credentials file
     * 3. EC2 instance profile (when running on EC2)
     * 4. ECS task role
     *
     * This is the RECOMMENDED approach for production/CI-CD
     */
    public AWSS3Uploader(String environment) {
        this.environment = environment;
        this.s3Client = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        log.info("AWSS3Uploader initialized | Bucket: {} | Region: {} | Env: {}",
                BUCKET_NAME, AWS_REGION, environment);
    }

    /**
     * Constructor with explicit credentials
     * Use for local testing only — NEVER hardcode in production
     * In production always use DefaultCredentialsProvider above
     */
    public AWSS3Uploader(String environment,
                         String accessKeyId,
                         String secretAccessKey) {
        this.environment = environment;
        this.s3Client = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
        log.warn("Using explicit credentials — use DefaultCredentialsProvider in production!");
    }

    /**
     * Upload a single file to S3
     * Organizes under: performance-reports/{env}/{date}/{filename}
     *
     * @param filePath  Local file path to upload
     * @param testName  Name of the test (used in S3 key)
     * @return S3 key (path) where file was uploaded
     */
    @Step("Upload to S3: {filePath}")
    public String uploadFile(String filePath, String testName) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("File not found for S3 upload: {}", filePath);
            return null;
        }

        String s3Key = buildS3Key(testName, file.getName());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(s3Key)
                    .contentType(getContentType(file.getName()))
                    .metadata(java.util.Map.of(
                            "test-name", testName,
                            "environment", environment,
                            "upload-timestamp", LocalDateTime.now().toString(),
                            "framework", "sdet-automation-framework"
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(file));
            uploadedKeys.add(s3Key);

            log.info("Uploaded to S3: s3://{}/{}", BUCKET_NAME, s3Key);

            // Attach S3 link to Allure report (audit evidence)
            Allure.addAttachment(
                    "S3 Upload - " + file.getName(),
                    "text/plain",
                    "File archived to S3:\ns3://" + BUCKET_NAME + "/" + s3Key
            );

            return s3Key;

        } catch (S3Exception e) {
            log.error("S3 upload failed for {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * Upload complete PerformanceTestResult summary to S3
     * Creates a human-readable .txt report in S3
     *
     * @param result  The performance test result to archive
     * @return S3 key where summary was uploaded
     */
    @Step("Archive Performance Result to S3: {result.testName}")
    public String uploadPerformanceSummary(PerformanceTestResult result) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = result.getTestName() + "_" + timestamp + "_summary.txt";
        String s3Key = buildS3Key(result.getTestName(), fileName);

        try {
            String content = result.getSummary();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(s3Key)
                    .contentType("text/plain")
                    .metadata(java.util.Map.of(
                            "test-name", result.getTestName(),
                            "test-type", result.getTestType(),
                            "environment", result.getEnvironment(),
                            "sla-status", result.isSlaBreached() ? "BREACHED" : "PASSED",
                            "timestamp", result.getTimestamp()
                    ))
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromString(content));
            uploadedKeys.add(s3Key);

            log.info("Performance summary archived: s3://{}/{}", BUCKET_NAME, s3Key);

            // Attach S3 reference to Allure
            Allure.addAttachment(
                    "S3 Archive - " + result.getTestName(),
                    "text/plain",
                    "Performance report archived:\ns3://" + BUCKET_NAME + "/" + s3Key +
                            "\n\nSLA Status: " + (result.isSlaBreached() ? "BREACHED" : "PASSED")
            );

            return s3Key;

        } catch (S3Exception e) {
            log.error("Failed to archive performance summary: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Upload entire directory of results to S3
     * Used to bulk-upload all JTL + report files after test suite
     *
     * @param directoryPath  Local directory to upload
     * @param testSuiteName  Name of the test suite
     * @return List of S3 keys uploaded
     */
    @Step("Upload directory to S3: {directoryPath}")
    public List<String> uploadDirectory(String directoryPath,
                                        String testSuiteName) {
        List<String> keys = new ArrayList<>();
        Path dir = Paths.get(directoryPath);

        if (!Files.exists(dir)) {
            log.warn("Directory does not exist: {}", directoryPath);
            return keys;
        }

        try {
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        String key = uploadFile(file.toString(), testSuiteName);
                        if (key != null) keys.add(key);
                    });

            log.info("Directory upload complete: {} files → S3", keys.size());

            // Summary attachment for Allure
            Allure.addAttachment(
                    "S3 Directory Upload - " + testSuiteName,
                    "text/plain",
                    "Uploaded " + keys.size() + " files to s3://" + BUCKET_NAME +
                            "\nKeys:\n" + String.join("\n", keys)
            );

        } catch (IOException e) {
            log.error("Directory upload failed: {}", e.getMessage());
        }

        return keys;
    }

    /**
     * Generate a pre-signed URL for sharing a report
     * Allows stakeholders to view reports without AWS access
     * Valid for 24 hours by default
     *
     * OCC Use Case: Share report link with dev team or auditors
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            software.amazon.awssdk.services.s3.presigner.S3Presigner presigner =
                    software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                            .region(Region.of(AWS_REGION))
                            .build();

            software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest =
                    software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                            .signatureDuration(java.time.Duration.ofHours(24))
                            .getObjectRequest(GetObjectRequest.builder()
                                    .bucket(BUCKET_NAME)
                                    .key(s3Key)
                                    .build())
                            .build();

            String url = presigner.presignGetObject(presignRequest)
                    .url().toString();

            log.info("Pre-signed URL generated for: {}", s3Key);
            presigner.close();
            return url;

        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verify S3 bucket exists and is accessible
     * Call this at test suite start to fail fast if misconfigured
     */
    public boolean verifyBucketAccess() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(BUCKET_NAME)
                    .build());
            log.info("S3 bucket accessible: {}", BUCKET_NAME);
            return true;
        } catch (NoSuchBucketException e) {
            log.error("S3 bucket does not exist: {}", BUCKET_NAME);
            return false;
        } catch (S3Exception e) {
            log.error("Cannot access S3 bucket {}: {}", BUCKET_NAME, e.getMessage());
            return false;
        }
    }

    /**
     * List all reports for this environment
     * Useful for audit reviews — show all historical test runs
     */
    public List<String> listReports() {
        List<String> reports = new ArrayList<>();
        String prefix = BASE_PREFIX + "/" + environment + "/";

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            response.contents().forEach(obj -> reports.add(obj.key()));

            log.info("Found {} reports in S3 for environment: {}",
                    reports.size(), environment);

        } catch (S3Exception e) {
            log.error("Failed to list S3 reports: {}", e.getMessage());
        }

        return reports;
    }

    /**
     * Build organized S3 key path
     * Format: performance-reports/{env}/{date}/{filename}
     */
    private String buildS3Key(String testName, String fileName) {
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return String.format("%s/%s/%s/%s",
                BASE_PREFIX, environment, date, fileName);
    }

    /**
     * Determine content type from file extension
     */
    private String getContentType(String fileName) {
        if (fileName.endsWith(".jtl")) return "text/csv";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".xml")) return "application/xml";
        return "application/octet-stream";
    }

    /**
     * Get all keys uploaded in this session
     */
    public List<String> getUploadedKeys() { return uploadedKeys; }

    /**
     * Close S3 client — call in @AfterSuite
     */
    public void close() {
        s3Client.close();
        log.info("S3 client closed. Total uploads this session: {}",
                uploadedKeys.size());
    }
}