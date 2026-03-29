package com.bidiu.hpcarrier;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class HeypassedCarrierScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget templateField;

    protected HeypassedCarrierScreen(Screen parent) {
        super(Text.literal("Heypassed Carrier"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HeypassedCarrierConfig config = HeypassedCarrierClient.getConfig();
        int centerX = this.width / 2;
        int top = this.height / 4;

        this.templateField = new TextFieldWidget(this.textRenderer, centerX - 110, top + 70, 220, 20,
                Text.literal("消息格式"));
        this.templateField.setMaxLength(512);
        this.templateField.setText(config.messageTemplate);
        this.addSelectableChild(this.templateField);

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoJoinParty)
                .build(centerX - 110, top, 220, 20, Text.literal("自动接受队伍邀请"),
                        (button, value) -> config.autoJoinParty = value));

        this.addDrawableChild(CyclingButtonWidget.<String>builder(mode -> Text.literal("玩法: " + mode))
                .values(HeypassedCarrierConfig.GAME_MODES)
                .initially(config.selectedMode)
                .build(centerX - 110, top + 30, 220, 20, Text.literal("玩法"),
                        (button, value) -> config.setSelectedMode(value)));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("保存并关闭"), button -> {
            config.messageTemplate = this.templateField.getText();
            config.normalize();
            HeypassedCarrierClient.saveConfig();
            this.close();
        }).dimensions(centerX - 110, top + 100, 105, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("恢复默认"), button -> {
            config.autoJoinParty = true;
            config.setSelectedMode(HeypassedCarrierConfig.GAME_MODES.getFirst());
            config.messageTemplate = ".irc chat $tell Bi_Diu .i [id] [mode]";
            config.normalize();
            HeypassedCarrierClient.saveConfig();
            this.client.setScreen(new HeypassedCarrierScreen(this.parent));
        }).dimensions(centerX + 5, top + 100, 105, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.templateField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.templateField.keyPressed(keyCode, scanCode, modifiers)
                || this.templateField.isActive() && this.templateField.isFocused()
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int top = this.height / 4;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top - 30, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("消息格式，支持 [id] 和 [mode] 占位符"), centerX - 110,
                top + 58,
                0xA0A0A0);
        this.templateField.render(context, mouseX, mouseY, delta);
    }
}