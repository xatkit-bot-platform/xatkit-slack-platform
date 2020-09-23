package com.xatkit.plugins.slack.platform.action;

import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.xatkit.execution.StateContext;
import com.xatkit.plugins.slack.platform.SlackPlatform;
import lombok.NonNull;

import java.util.List;

public class ReplyLayoutBlocksMessage extends PostLayoutBlocksMessage {

    /**
     * Constructs a new {@link ReplyLayoutBlocksMessage} with the provided
     * {@code runtimePlatform}, {@code session}, and {@code layoutBlocks}.
     *
     * @param platform     the {@link SlackPlatform} containing this action
     * @param context      the {@link StateContext} associated to this action
     * @param layoutBlocks the {@link LayoutBlock} list to post
     * @see Reply#getChannel(StateContext)
     * @see PostMessage#PostMessage(SlackPlatform, StateContext, String, String, String)
     */
    public ReplyLayoutBlocksMessage(@NonNull SlackPlatform platform, @NonNull StateContext context,
                                    @NonNull List<LayoutBlock> layoutBlocks) {
        super(platform, context, layoutBlocks, Reply.getChannel(context), Reply.getTeamId(context));
    }

}
