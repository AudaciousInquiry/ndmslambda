package com.audaciousinquiry.saner.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.audaciousinquiry.saner.Constants;
import com.audaciousinquiry.saner.Utility;
import com.audaciousinquiry.saner.exceptions.SanerLambdaException;
import com.audaciousinquiry.saner.models.Job;
import com.audaciousinquiry.saner.records.Oauth2;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MethodistCsvReader implements RequestHandler<Void, List<Job>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(MethodistCsvReader.class);

    @Override
    public List<Job> handleRequest(Void unused, Context context) {
        List<Job> returnValue = new ArrayList<>();

        log.info("MethodistCsvReader Lambda - Started");

        try {
            String secretName = System.getenv("API_AUTH_SECRET");
            Region region = Region.of(System.getenv("AWS_REGION"));
            String baseUrl = System.getenv("API_ENDPOINT");
            String locationId = System.getenv("LOCATION_ID");
            String apiUrl = Utility.templateReplacer(baseUrl, Constants.LOCATION_ID_PLACEHOLDER, locationId);
            String bucketName = System.getenv("BUCKET_NAME");
            String directoryName = System.getenv("DIRECTORY_NAME");
            String archiveDirectory = System.getenv("ARCHIVE_DIRECTORY");

            Oauth2 oauth2 = Oauth2.fromAwsSecret(region, secretName);
            log.info("Oauth2 Config Obtained From AWS Secret");

            AccessToken accessToken = Utility.getOauth2AccessToken(oauth2);
            log.info("Access Token Obtained");

            // We need to
            // 1. List all *.csv files in the bucket in the /home directory
            // 2. Read each csv, send to API
            // 3. Move the processed CSV to the /home/archive directory

            S3Client s3 = S3Client.builder()
                    .region(region)
                    .build();

            log.info("Getting list of CSV files in {} directory of bucket {}", directoryName, bucketName);
            List<String> csvFiles = getCsvList(s3, bucketName, directoryName);

            for (String csvFile : csvFiles) {

                log.info("Reading CSV file {}", csvFile);
                String csvContents = getCsvContents(s3, bucketName, csvFile);

                log.info("Submitting CSV to API: {}", apiUrl);
                HttpResponse<String> response = sendCsvToApi(apiUrl,csvContents, accessToken);
                if (response.statusCode() != HttpStatusCode.OK) {
                    throw new SanerLambdaException(String.format("Error calling API: %s", response.body()));
                }

                Job csvJob = objectMapper.readValue(response.body(), Job.class);
                returnValue.add(csvJob);

                log.info("API Call Status for file {}: {}, Saner Job ID: {}",
                        csvFile,
                        response.statusCode(),
                        csvJob.getId()
                );

                // Archive CSV
                log.info("Archiving CSV file to {}", archiveDirectory);
                archiveCsv(s3, bucketName, csvFile, directoryName, archiveDirectory);

            }

        } catch (URISyntaxException | IOException | ParseException ex) {
            throw new SanerLambdaException(ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SanerLambdaException(ex.getMessage());
        }

        return returnValue;
    }

    private String getCsvContents(S3Client s3, String bucketName, String fileName) {
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(fileName)
                .bucket(bucketName)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3.getObject(objectRequest, ResponseTransformer.toBytes());

        return objectBytes.asString(Charset.defaultCharset());
    }

    private void archiveCsv(S3Client s3, String bucketName, String fileName, String directoryName, String archiveDirectory) {

        String destinationFileName = fileName.replace(directoryName, archiveDirectory);

        CopyObjectRequest copyObjectRequest = CopyObjectRequest
                .builder()
                .sourceBucket(bucketName)
                .sourceKey(fileName)
                .destinationBucket(bucketName)
                .destinationKey(destinationFileName)
                .build();

        s3.copyObject(copyObjectRequest);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3.deleteObject(deleteRequest);
    }

    private HttpResponse<String> sendCsvToApi(String apiUrl, String csvContents, AccessToken accessToken) throws
            InterruptedException, IOException, URISyntaxException {

        try (HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Authorization", accessToken.toAuthorizationHeader())
                    .header("Content-Type", "text/csv")
                    .POST(HttpRequest.BodyPublishers.ofString(csvContents))
                    .build();

            return httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    private List<String> getCsvList(S3Client s3, String bucketName, String prefix) {
        // prefix is basically "directory"

        List<String> returnList = new ArrayList<>();

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .delimiter("/")
                .maxKeys(1)
                .build();

        ListObjectsV2Iterable listRes = s3.listObjectsV2Paginator(listReq);
        listRes.stream()
                .flatMap(r -> r.contents().stream())
                .filter(
                        content -> content.key().endsWith(".csv")
                )
                .forEach(
                        content -> returnList.add(content.key())
                );

        return returnList;
    }
}
