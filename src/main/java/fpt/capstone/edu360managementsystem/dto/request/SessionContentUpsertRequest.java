package fpt.capstone.edu360managementsystem.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SessionContentUpsertRequest {

    private List<Long> chapterIds;
    private List<Long> lessonIds;
}
