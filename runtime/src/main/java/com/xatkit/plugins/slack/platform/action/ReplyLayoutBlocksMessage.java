package com.xatkit.plugins.slack.platform.action;

import java.util.List;

import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.xatkit.core.session.RuntimeContexts;
import com.xatkit.core.session.XatkitSession;
import com.xatkit.plugins.slack.platform.SlackPlatform;

public class ReplyLayoutBlocksMessage extends PostLayoutBlocksMessage  {

    /**
     * Constructs a new {@link ReplyLayoutBlocksMessage} with the provided
     * {@code runtimePlatform}, {@code session}, and {@code layoutBlocks}.
     *
     * @param runtimePlatform the {@link SlackPlatform} containing this action
     * @param session         the {@link XatkitSession} associated to this action
     * @param layoutBlocks    the {@link LayoutBlock} list to post
     * @throws NullPointerException     if the provided {@code runtimePlatform} or
     *                                  {@code session} is {@code null}
     * @see Reply#getChannel(RuntimeContexts)
     * @see PostMessage#PostMessage(SlackPlatform, XatkitSession, String, String)
     */
    public ReplyLayoutBlocksMessage(SlackPlatform runtimePlatform, XatkitSession session, List<LayoutBlock> layoutBlocks) {
        super(runtimePlatform, session, layoutBlocks, Reply.getChannel(session.getRuntimeContexts()));
    }

}
