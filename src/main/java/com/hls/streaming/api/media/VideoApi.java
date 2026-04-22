package com.hls.streaming.api.media;

import com.hls.streaming.dtos.media.VideoUploadResponse;
import com.hls.streaming.security.constants.SecurityConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Video Api", description = "Video api")
@RequestMapping("/v1/videos")
public interface VideoApi {

    @Operation(summary = "Upload raw video and trigger processing")
    @PreAuthorize("hasRole('" + SecurityConstant.UserRole.USER + "')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    VideoUploadResponse uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description);
}
