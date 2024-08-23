package clpetition.backend.member.docs;

import clpetition.backend.global.annotation.FindMember;
import clpetition.backend.global.response.BaseResponse;
import clpetition.backend.member.domain.Member;
import clpetition.backend.member.dto.response.GetProfileDetailsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "Member API", description = "사용자 API")
public interface GetProfileDetailsApiDocs {

    @Operation(summary = "프로필 정보 상세조회 API", description = "프로필 정보를 상세조회합니다.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "🟢 정상"),
                    @ApiResponse(
                            responseCode = "404", description = "❌ 프로필이 존재하지 않음",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = BaseResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                            {
                                                "code": "PROFILE_001",
                                                "message": "존재하지 않는 프로필입니다.",
                                                "result": null
                                            }
                                            """
                                    )
                            )
                    ),
            }
    )
    ResponseEntity<BaseResponse<GetProfileDetailsResponse>> getProfileDetails(
            @Parameter(hidden = true)
            @FindMember Member member
    );
}
