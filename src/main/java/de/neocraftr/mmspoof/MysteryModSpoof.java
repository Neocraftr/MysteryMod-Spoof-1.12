package de.neocraftr.mmspoof;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.PluginMessageEvent;
import net.labymod.settings.elements.SettingsElement;
import net.minecraft.network.PacketBuffer;

import java.util.List;

public class MysteryModSpoof extends LabyModAddon {

    @Override
    public void onEnable() {
        getApi().getEventManager().register(new PluginMessageEvent() {
            @Override
            public void receiveMessage(String channel, PacketBuffer packetBufferOrig) {
                ByteBuf packetBuffer = packetBufferOrig.copy();

                if(channel.equals("mysterymod:mm")) {
                    if(packetBuffer.readableBytes() <= 0) return;
                    String messageKey = readStringFromBuffer(32767, packetBuffer);

                    if(packetBuffer.readableBytes() <= 0) return;
                    String message = readStringFromBuffer(32767, packetBuffer);

                    // echo back message from server
                    if(messageKey.equals("mysterymod_user_check")) {
                        sendMysteryModMessage(message);
                    }
                }
            }
        });
    }

    @Override
    public void loadConfig() {}

    @Override
    protected void fillSettings(List<SettingsElement> list) {}

    private void sendMysteryModMessage(String message) {
        PacketBuffer responseBuffer = new PacketBuffer(Unpooled.buffer());
        responseBuffer.writeString(message);
        getApi().sendPluginMessage("mysterymod:mm", responseBuffer);
    }

    private String readStringFromBuffer(int maxLength, ByteBuf packetBuffer) {
        int i = this.readVarIntFromBuffer(packetBuffer);
        if (i > maxLength * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
        } else if (i < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        } else {
            ByteBuf byteBuf = packetBuffer.readBytes(i);
            byte[] bytes;
            if (byteBuf.hasArray()) {
                bytes = byteBuf.array();
            } else {
                bytes = new byte[byteBuf.readableBytes()];
                byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            }

            String s = new String(bytes, Charsets.UTF_8);
            if (s.length() > maxLength) {
                throw new DecoderException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
            } else {
                return s;
            }
        }
    }

    private int readVarIntFromBuffer(ByteBuf packetBuffer) {
        int i = 0;
        int j = 0;

        byte b0;
        do {
            b0 = packetBuffer.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b0 & 128) == 128);

        return i;
    }
}
