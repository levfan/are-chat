package com.smart.chat.platform.domain.notify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 「哪些渠道配齐了」的裁决与发送顺序（改造前长在 AdminNotifyService 的 if 里）。
 * 渠道的 wireName 会落进日志与 ChannelResult，是对外说法，逐条锁住。
 */
class EnabledNotifyChannelsTest {

    private record Config(String webhook, String appToken, String uids, String serverchanSendkey,
                          String xtuisSendkey) implements NotifyChannelConfig {
        @Override
        public String wecomWebhook() {
            return webhook;
        }

        @Override
        public String wxpusherAppToken() {
            return appToken;
        }

        @Override
        public String wxpusherUids() {
            return uids;
        }

        @Override
        public String serverchanSendkey() {
            return serverchanSendkey;
        }

        @Override
        public String xtuisSendkey() {
            return xtuisSendkey;
        }
    }

    private static final Config NOTHING = new Config(null, null, null, null, null);

    @Test
    void noChannelConfiguredIsNotAnError() {
        assertThat(EnabledNotifyChannels.of(NOTHING)).isEmpty();
        assertThat(EnabledNotifyChannels.noneConfigured(NOTHING)).isTrue();
        assertThat(EnabledNotifyChannels.noneConfigured(new Config("  ", "", "", "", "")))
                .as("空白值等于没配")
                .isTrue();
    }

    @Test
    void wecomNeedsOnlyTheWebhook() {
        Config config = new Config("https://qyapi.weixin.qq.com/hook", null, null, null, null);

        assertThat(EnabledNotifyChannels.of(config)).containsExactly(NotifyChannel.WECOM);
        assertThat(EnabledNotifyChannels.noneConfigured(config)).isFalse();
    }

    @Test
    void wxpusherNeedsTokenAndRecipientsTogether() {
        assertThat(EnabledNotifyChannels.isConfigured(NotifyChannel.WXPUSHER,
                new Config(null, "app-token", null, null, null))).isFalse();
        assertThat(EnabledNotifyChannels.isConfigured(NotifyChannel.WXPUSHER,
                new Config(null, null, "uid-1", null, null))).isFalse();
        assertThat(EnabledNotifyChannels.isConfigured(NotifyChannel.WXPUSHER,
                new Config(null, "app-token", "uid-1,uid-2", null, null))).isTrue();
    }

    @Test
    void sendOrderIsWeComThenWxpusherThenServerchanThenXtuis() {
        Config all = new Config("hook", "token", "uid", "sct-key", "xt-key");

        assertThat(EnabledNotifyChannels.of(all)).containsExactly(
                NotifyChannel.WECOM, NotifyChannel.WXPUSHER, NotifyChannel.SERVERCHAN, NotifyChannel.XTUIS);
        assertThat(EnabledNotifyChannels.of(all)).extracting(NotifyChannel::wireName)
                .containsExactly("wecom", "wxpusher", "serverchan", "xtuis");
    }

    @Test
    void partiallyConfiguredChannelsKeepTheRelativeOrder() {
        List<NotifyChannel> enabled = EnabledNotifyChannels.of(new Config(null, null, null, "sct-key", "xt-key"));

        assertThat(enabled).containsExactly(NotifyChannel.SERVERCHAN, NotifyChannel.XTUIS);
    }

    @Test
    void wireNamesMatchTheHistoricalChannelStrings() {
        assertThat(NotifyChannel.values()).extracting(NotifyChannel::wireName)
                .containsExactly("wecom", "wxpusher", "serverchan", "xtuis");
    }
}
