package fr.postiqa.agency.controller;

import fr.postiqa.features.postmanagement.adapter.in.rest.PostDtoMapper;
import fr.postiqa.features.postmanagement.domain.model.Post;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.domain.vo.PostId;
import fr.postiqa.features.postmanagement.usecase.*;
import fr.postiqa.shared.dto.postmanagement.*;
import fr.postiqa.shared.enums.PostStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for post management (Agency API).
 * All operations are scoped to a specific client.
 */
@RestController
@RequestMapping("/api/agency/clients/{clientId}/posts")
public class AgencyPostController {

    private final CreatePostUseCase createPostUseCase;
    private final UpdatePostUseCase updatePostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final GetPostUseCase getPostUseCase;
    private final ListPostsUseCase listPostsUseCase;
    private final SchedulePostUseCase schedulePostUseCase;
    private final CancelScheduleUseCase cancelScheduleUseCase;
    private final PostDtoMapper dtoMapper;

    public AgencyPostController(
        CreatePostUseCase createPostUseCase,
        UpdatePostUseCase updatePostUseCase,
        DeletePostUseCase deletePostUseCase,
        GetPostUseCase getPostUseCase,
        ListPostsUseCase listPostsUseCase,
        SchedulePostUseCase schedulePostUseCase,
        CancelScheduleUseCase cancelScheduleUseCase,
        PostDtoMapper dtoMapper
    ) {
        this.createPostUseCase = createPostUseCase;
        this.updatePostUseCase = updatePostUseCase;
        this.deletePostUseCase = deletePostUseCase;
        this.getPostUseCase = getPostUseCase;
        this.listPostsUseCase = listPostsUseCase;
        this.schedulePostUseCase = schedulePostUseCase;
        this.cancelScheduleUseCase = cancelScheduleUseCase;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(
        @PathVariable String clientId,
        @Valid @RequestBody CreatePostRequest request
    ) {
        List<ChannelId> channelIds = request.channelIds().stream()
            .map(ChannelId::of)
            .toList();

        CreatePostUseCase.CreatePostCommand command = new CreatePostUseCase.CreatePostCommand(
            request.content(),
            channelIds,
            request.scheduledFor()
        );

        PostId postId = createPostUseCase.execute(command);
        Post post = getPostUseCase.execute(new GetPostUseCase.GetPostQuery(postId));

        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toDto(post));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(
        @PathVariable String clientId,
        @PathVariable String postId
    ) {
        Post post = getPostUseCase.execute(new GetPostUseCase.GetPostQuery(PostId.of(postId)));
        return ResponseEntity.ok(dtoMapper.toDto(post));
    }

    @GetMapping
    public ResponseEntity<List<PostDto>> listPosts(
        @PathVariable String clientId,
        @RequestParam(required = false) PostStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        ListPostsUseCase.ListPostsQuery query = new ListPostsUseCase.ListPostsQuery(status, page, size);
        List<Post> posts = listPostsUseCase.execute(query);

        List<PostDto> dtos = posts.stream()
            .map(dtoMapper::toDto)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDto> updatePost(
        @PathVariable String clientId,
        @PathVariable String postId,
        @Valid @RequestBody UpdatePostRequest request
    ) {
        List<ChannelId> channelIds = request.channelIds() != null
            ? request.channelIds().stream().map(ChannelId::of).toList()
            : null;

        UpdatePostUseCase.UpdatePostCommand command = new UpdatePostUseCase.UpdatePostCommand(
            PostId.of(postId),
            request.content(),
            channelIds
        );

        updatePostUseCase.execute(command);
        Post post = getPostUseCase.execute(new GetPostUseCase.GetPostQuery(PostId.of(postId)));

        return ResponseEntity.ok(dtoMapper.toDto(post));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
        @PathVariable String clientId,
        @PathVariable String postId
    ) {
        deletePostUseCase.execute(new DeletePostUseCase.DeletePostCommand(PostId.of(postId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/schedule")
    public ResponseEntity<PostDto> schedulePost(
        @PathVariable String clientId,
        @PathVariable String postId,
        @Valid @RequestBody SchedulePostRequest request
    ) {
        SchedulePostUseCase.SchedulePostCommand command = new SchedulePostUseCase.SchedulePostCommand(
            PostId.of(postId),
            request.scheduledFor()
        );

        schedulePostUseCase.execute(command);
        Post post = getPostUseCase.execute(new GetPostUseCase.GetPostQuery(PostId.of(postId)));

        return ResponseEntity.ok(dtoMapper.toDto(post));
    }

    @PostMapping("/{postId}/cancel")
    public ResponseEntity<PostDto> cancelSchedule(
        @PathVariable String clientId,
        @PathVariable String postId
    ) {
        cancelScheduleUseCase.execute(new CancelScheduleUseCase.CancelScheduleCommand(PostId.of(postId)));
        Post post = getPostUseCase.execute(new GetPostUseCase.GetPostQuery(PostId.of(postId)));

        return ResponseEntity.ok(dtoMapper.toDto(post));
    }
}
