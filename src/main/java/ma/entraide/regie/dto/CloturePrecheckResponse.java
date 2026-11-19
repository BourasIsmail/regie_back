package ma.entraide.regie.dto;

import java.util.List;

public record CloturePrecheckResponse(
        Long provinceId,
        String provinceName,
        int annee,
        boolean cloturable,
        List<String> blocages
) {
}
