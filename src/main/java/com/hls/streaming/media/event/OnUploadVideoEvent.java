package com.hls.streaming.media.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnUploadVideoEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = -6458835340721236768L;

    private String videoId;
}
