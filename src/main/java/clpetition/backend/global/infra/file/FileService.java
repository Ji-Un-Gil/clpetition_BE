package clpetition.backend.global.infra.file;

import clpetition.backend.global.response.BaseException;
import clpetition.backend.global.response.BaseResponseStatus;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3 Upload File
     * */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException { // dirName의 디렉토리가 S3 Bucket 내부에 생성됨
        File uploadFile = convert(multipartFile)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.FILE_CONVERT_FAILED_ERROR));
        return upload(uploadFile, dirName);
    }

    /**
     * S3 Upload Many Files
     * */
    public List<String> uploadFiles(List<MultipartFile> multipartFileList, String dirName) {
        List<String> imageUrlList = new ArrayList<>();
        if (multipartFileList == null)
            return imageUrlList;
        for (MultipartFile multipartFile: multipartFileList) {
            try { // 파일 업로드
                String fileName = upload(multipartFile, dirName); // S3 버킷의 images 디렉토리 안에 저장됨
                imageUrlList.add(fileName);
            } catch (Exception e) {
                throw new BaseException(BaseResponseStatus.FILE_SERVER_ERROR);
            }
        }
        return imageUrlList;
    }

    /**
     * S3 Delete Many Files
     * */
    public void deleteFiles(List<String> imageUrlList) {
        for (String imageUrl: imageUrlList) {
            try {
                amazonS3Client.deleteObject(bucket, imageUrl.split("/")[4]);
            } catch (Exception e) {
                throw new BaseException(BaseResponseStatus.FILE_SERVER_ERROR);
            }
        }
    }

    private String upload(File uploadFile, String dirName) {
        String fileName = generateFileName(uploadFile, dirName);
        String uploadImageUrl = putS3(uploadFile, fileName);

        removeNewFile(uploadFile);  // convert()함수로 인해서 로컬에 생성된 File 삭제 (MultipartFile -> File 전환 하며 로컬에 파일 생성됨)

        return uploadImageUrl;      // 업로드된 파일의 S3 URL 주소 반환
    }

    private String generateFileName(File uploadFile, String dirName) {
        String originalFileName = uploadFile.getName();
        int dotIndex = originalFileName.lastIndexOf('.');

        // 파일 확장자 X
        if (dotIndex <= 0)
            throw new BaseException(BaseResponseStatus.FILE_CONVERT_FAILED_ERROR);

        String fileHead = "/clpetition-";   // 서비스명 부여
        String uuid = UUID.randomUUID().toString(); // 파일명 중복 방지
        String extension = originalFileName.substring(dotIndex);    // 파일 확장자

        return dirName + fileHead + uuid + extension;
    }

    private String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(
                new PutObjectRequest(bucket, fileName, uploadFile)
                        .withCannedAcl(CannedAccessControlList.PublicRead)	// PublicRead 권한으로 업로드 됨
        );
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    private void removeNewFile(File targetFile) {
        if (targetFile.delete())
            log.info("파일이 삭제되었습니다.");
        else
            log.info("파일이 삭제되지 못했습니다.");
    }

    private Optional<File> convert(MultipartFile file) throws IOException {
        File convertFile = new File(file.getOriginalFilename()); // 업로드한 파일의 이름
        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }
}
