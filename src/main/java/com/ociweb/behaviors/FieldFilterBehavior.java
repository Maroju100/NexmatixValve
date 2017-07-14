package com.ociweb.behaviors;

import com.ociweb.schema.FieldType;
import com.ociweb.schema.MessageScheme;
import com.ociweb.gl.api.MessageReader;
import com.ociweb.gl.api.PubSubListener;
import com.ociweb.iot.maker.FogCommandChannel;
import com.ociweb.iot.maker.FogRuntime;

public class FieldFilterBehavior implements PubSubListener {
    private final FogCommandChannel channel;
    private final FieldType fieldType;
    public final String publishTopic;

    private int intCache = Integer.MAX_VALUE;
    private String stringCache = null;

    public FieldFilterBehavior(FogRuntime runtime, String topic, int stationId, int valueId) {
        this.channel = runtime.newCommandChannel(DYNAMIC_MESSAGING);
        this.fieldType = MessageScheme.types[valueId];
        this.publishTopic = String.format("%s/%d/%d", topic, stationId, valueId);
    }

    @Override
    public boolean message(CharSequence charSequence, MessageReader messageReader) {
        final long timeStamp = messageReader.readLong();
        boolean publish = false;
        switch (fieldType) {
            case integer: {
                int newValue = messageReader.readInt();
                if (newValue != intCache) {
                    intCache = newValue;
                    publish = true;
                }
                break;
            }
            case string: {
                String newValue = messageReader.readUTF();
                if (!newValue.equals(stringCache)) {
                    stringCache = newValue;
                    publish = true;
                }
                break;
            }
        }
        if (publish) {
            channel.publishTopic(publishTopic, pubSubWriter -> {
                pubSubWriter.writeLong(timeStamp);
                System.out.println(String.format("D) Issued: Path:%s", this.publishTopic));
                switch (fieldType) {
                    case integer:
                        pubSubWriter.writeInt(intCache);
                        break;
                    case string:
                        pubSubWriter.writeUTF(stringCache);
                        break;
                }
            });
        }
        else {
            System.out.println(String.format("D) Dropped: Path:%s", this.publishTopic));
            System.out.println("D) Filtered");
        }
        return true;
    }
}
