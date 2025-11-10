package fr.postiqa.agency.controller;

import fr.postiqa.features.postmanagement.adapter.in.rest.PostDtoMapper;
import fr.postiqa.features.postmanagement.domain.model.Channel;
import fr.postiqa.features.postmanagement.domain.vo.ChannelId;
import fr.postiqa.features.postmanagement.usecase.GetChannelUseCase;
import fr.postiqa.features.postmanagement.usecase.ListChannelsUseCase;
import fr.postiqa.shared.dto.postmanagement.ChannelDto;
import fr.postiqa.shared.enums.SocialPlatform;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for channel management (Agency API).
 * All operations are scoped to a specific client.
 */
@RestController
@RequestMapping("/api/agency/clients/{clientId}/channels")
public class AgencyChannelController {

    private final GetChannelUseCase getChannelUseCase;
    private final ListChannelsUseCase listChannelsUseCase;
    private final PostDtoMapper dtoMapper;

    public AgencyChannelController(
        GetChannelUseCase getChannelUseCase,
        ListChannelsUseCase listChannelsUseCase,
        PostDtoMapper dtoMapper
    ) {
        this.getChannelUseCase = getChannelUseCase;
        this.listChannelsUseCase = listChannelsUseCase;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ChannelDto> getChannel(
        @PathVariable String clientId,
        @PathVariable String channelId
    ) {
        Channel channel = getChannelUseCase.execute(new GetChannelUseCase.GetChannelQuery(ChannelId.of(channelId)));
        return ResponseEntity.ok(dtoMapper.toDto(channel));
    }

    @GetMapping
    public ResponseEntity<List<ChannelDto>> listChannels(
        @PathVariable String clientId,
        @RequestParam(required = false) SocialPlatform platform,
        @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        ListChannelsUseCase.ListChannelsQuery query = new ListChannelsUseCase.ListChannelsQuery(platform, activeOnly);
        List<Channel> channels = listChannelsUseCase.execute(query);

        List<ChannelDto> dtos = channels.stream()
            .map(dtoMapper::toDto)
            .toList();

        return ResponseEntity.ok(dtos);
    }
}
