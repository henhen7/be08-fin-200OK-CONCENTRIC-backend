package ok.backend.friendship.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
@Getter
public class FriendshipResponseDto {

    @JsonProperty
    private Long id;

    @JsonProperty
    private String nickname;

    @JsonProperty
    private LocalDate createDate;

    @JsonProperty
    private String backgroundImage;

    @JsonProperty
    private String profileImage;

    @JsonProperty
    private String content;
}
