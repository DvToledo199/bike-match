package com.bikematch.bike.api;

import com.bikematch.bike.MarkedPhotoGeometry;
import com.bikematch.bike.MarkedPhotoPoint;
import com.bikematch.kinematics.api.PointDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FinalizeBikeAnalysisRequest(
        @NotNull @Min(1) @Max(100000) Integer imageWidth,
        @NotNull @Min(1) @Max(100000) Integer imageHeight,
        @NotEmpty @Size(min = 6, max = 6) List<@NotNull @Valid PointDto> points
) {

    MarkedPhotoGeometry toGeometry() {
        List<MarkedPhotoPoint> markedPoints = points.stream()
                .map(point -> new MarkedPhotoPoint(point.type(), point.x(), point.y()))
                .toList();
        return MarkedPhotoGeometry.create(imageWidth, imageHeight, markedPoints);
    }
}
