package ch.mimo.netty.handler.codec.icap;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

public abstract class IcapMessageDecoder extends ReplayingDecoder<StateEnum> {

    protected final int maxInitialLineLength;
    protected final int maxIcapHeaderSize;
    protected final int maxHttpHeaderSize;
    protected final int maxChunkSize;
    
    private StateEnum previousState;
    
	protected IcapMessage message;
	
    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength (4096}}, {@code maxIcapHeaderSize (8192)}, {@code maxHttpHeaderSize (8192)}, and
     * {@code maxChunkSize (8192)}.
     */
    protected IcapMessageDecoder() {
        this(4096,8192,8192,8192);
    }
    
    /**
     * Creates a new instance with the specified parameters.
     */
    protected IcapMessageDecoder(int maxInitialLineLength, int maxIcapHeaderSize, int maxHttpHeaderSize, int maxChunkSize) {
        super(StateEnum.SKIP_CONTROL_CHARS,true);
        if (maxInitialLineLength <= 0) {
            throw new IllegalArgumentException("maxInitialLineLength must be a positive integer: " + maxInitialLineLength);
        }
        if (maxIcapHeaderSize <= 0) {
            throw new IllegalArgumentException("maxIcapHeaderSize must be a positive integer: " + maxIcapHeaderSize);
        }
        if (maxChunkSize < 0) {
            throw new IllegalArgumentException("maxChunkSize must be a positive integer: " + maxChunkSize);
        }
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxIcapHeaderSize = maxIcapHeaderSize;
        this.maxHttpHeaderSize = maxHttpHeaderSize;
        this.maxChunkSize = maxChunkSize;
    }

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, StateEnum stateEnumValue) throws Exception {
		State state = stateEnumValue.getState();
		state.onEntry(buffer,this,previousState);
		StateReturnValue returnValue = state.execute(buffer,this,previousState);
		StateEnum nextState = state.onExit(buffer,this,previousState);
		if(nextState != null) {
			checkpoint(nextState);
			previousState = stateEnumValue;
		} else {
			checkpoint();
		}
		if(returnValue.isRelevant()) {
			return returnValue.getValue();
		}
		return null;
	}
	
	public abstract boolean isDecodingRequest();
	
	protected abstract IcapMessage createMessage(String[] initialLine);
}
