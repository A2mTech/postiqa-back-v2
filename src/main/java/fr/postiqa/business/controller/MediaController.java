package fr.postiqa.business.controller;

import fr.postiqa.features.postmanagement.adapter.in.rest.PostDtoMapper;
import fr.postiqa.features.postmanagement.domain.vo.Media;
import fr.postiqa.features.postmanagement.domain.vo.MediaId;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import fr.postiqa.features.postmanagement.usecase.DeleteMediaUseCase;
import fr.postiqa.features.postmanagement.usecase.UploadMediaUseCase;
import fr.postiqa.shared.dto.postmanagement.UploadMediaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for media upload/delete (Business API).
 */
@RestController
@RequestMapping("/api/business/posts/{postId}/media")
public class MediaController {

    private final UploadMediaUseCase uploadMediaUseCase;
    private final DeleteMediaUseCase deleteMediaUseCase;
    private final PostDtoMapper dtoMapper;

    public MediaController(
        UploadMediaUseCase uploadMediaUseCase,
        DeleteMediaUseCase deleteMediaUseCase,
        PostDtoMapper dtoMapper
    ) {
        this.uploadMediaUseCase = uploadMediaUseCase;
        this.deleteMediaUseCase = deleteMediaUseCase;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Upload media to a post
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadMediaResponse> uploadMedia(
        @PathVariable String postId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        UploadMediaUseCase.UploadMediaCommand command = new UploadMediaUseCase.UploadMediaCommand(
            PostId.of(postId),
            file.getBytes(),
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize()
        );

        Media media = uploadMediaUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toUploadResponse(media));
    }

    /**
     * Delete media from a post
     */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
        @PathVariable String postId,
        @PathVariable String mediaId
    ) {
        DeleteMediaUseCase.DeleteMediaCommand command = new DeleteMediaUseCase.DeleteMediaCommand(
            PostId.of(postId),
            MediaId.of(mediaId)
        );

        deleteMediaUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }
}
