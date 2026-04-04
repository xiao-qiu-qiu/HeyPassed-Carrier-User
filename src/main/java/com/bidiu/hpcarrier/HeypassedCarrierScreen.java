package com.bidiu.hpcarrier;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.tooltip.Tooltip;

public class HeypassedCarrierScreen extends Screen {
    private static final int MODE_DROPDOWN_WIDTH = 220;
    private static final int MODE_DROPDOWN_HEIGHT = 20;
    private static final int MODE_DROPDOWN_VISIBLE_ROWS = 6;
    private static final Text AUTO_JOIN_WINDOW_TOOLTIP = Text.literal(
            "开启后，自动接受组队邀请只会在最近一次发送IRC快捷消息后的10秒内生效。若这10秒内已经成功接受过一次邀请，会立刻停止继续自动接受；若10秒内一直没接到邀请，也会自动停止并在聊天栏提示。下次发送快捷消息后会重新开始计时。");
    private static final Text IN_GAME_SEND_CONFIRM_TOOLTIP = Text.literal(
            "默认开启。开启后，若检测到当前不在主城，发送快捷消息需要在0.5s内连按两次发送键。第一次按下只会在聊天栏提醒，第二次按下才会真正发送，防止局内误触。主城通过Tab列表顶部标题识别。");

    private final Screen parent;
    private HeypassedCarrierConfig config;
    private TextFieldWidget templateField;
    private boolean modeDropdownExpanded;
    private int modeDropdownScrollOffset;

    protected HeypassedCarrierScreen(Screen parent) {
        super(Text.literal("Heypassed Carrier"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.config = HeypassedCarrierClient.getConfig();
        this.modeDropdownExpanded = false;
        this.modeDropdownScrollOffset = 0;
        int centerX = this.width / 2;
        int top = this.height / 4;
        int templateFieldY = top + 88;
        int actionButtonsY = top + 122;

        this.templateField = new TextFieldWidget(this.textRenderer, centerX - 110, templateFieldY, 220, 20,
                Text.literal("消息格式"));
        this.templateField.setMaxLength(512);
        this.templateField.setText(config.messageTemplate);
        this.addDrawableChild(this.templateField);

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoJoinParty)
                .build(centerX - 110, top, 84, 20, Text.literal("自动接受组队"),
                        (button, value) -> config.autoJoinParty = value));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoJoinPartyRecentInviteWindow)
                .tooltip(value -> Tooltip.of(AUTO_JOIN_WINDOW_TOOLTIP))
                .build(centerX - 24, top, 134, 20, Text.literal("10秒接邀请窗口期"),
                        (button, value) -> config.autoJoinPartyRecentInviteWindow = value));

        this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.inGameSendConfirm)
            .tooltip(value -> Tooltip.of(IN_GAME_SEND_CONFIRM_TOOLTIP))
            .build(centerX - 110, top + 22, 220, 20, Text.literal("局内防误触模式"),
                (button, value) -> config.inGameSendConfirm = value));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("保存并关闭"), button -> {
            config.messageTemplate = this.templateField.getText();
            config.normalize();
            HeypassedCarrierClient.saveConfig();
            this.close();
        }).dimensions(centerX + 5, actionButtonsY, 105, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("恢复默认"), button -> {
            config.autoJoinParty = true;
            config.autoJoinPartyRecentInviteWindow = true;
            config.inGameSendConfirm = true;
            config.setSelectedMode(HeypassedCarrierConfig.GAME_MODES.getFirst());
            config.messageTemplate = ".irc chat $tell Bi_Diu .i [id] [mode]";
            config.normalize();
            HeypassedCarrierClient.saveConfig();
            this.client.setScreen(new HeypassedCarrierScreen(this.parent));
        }).dimensions(centerX - 110, actionButtonsY, 105, 20).build());
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
        if (this.modeDropdownExpanded) {
            return false;
        }
        return this.templateField.charTyped(chr, modifiers) || super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.modeDropdownExpanded) {
            this.modeDropdownExpanded = false;
            return true;
        }

        if (this.modeDropdownExpanded) {
            return false;
        }

        return this.templateField.keyPressed(keyCode, scanCode, modifiers)
                || this.templateField.isActive() && this.templateField.isFocused()
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.handleModeDropdownClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.modeDropdownExpanded || !this.isPointWithin(mouseX, mouseY, this.getModeDropdownX(),
                this.getModeDropdownY() + MODE_DROPDOWN_HEIGHT, MODE_DROPDOWN_WIDTH,
                this.getModeDropdownListHeight())) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int maxOffset = this.getModeDropdownMaxScrollOffset();
        if (maxOffset <= 0) {
            return true;
        }

        if (verticalAmount < 0) {
            this.modeDropdownScrollOffset = Math.min(this.modeDropdownScrollOffset + 1, maxOffset);
        } else if (verticalAmount > 0) {
            this.modeDropdownScrollOffset = Math.max(this.modeDropdownScrollOffset - 1, 0);
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int top = this.height / 4;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, top - 30, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("消息格式，支持 [id] 和 [mode] 占位符"), centerX - 110,
            top + 77,
                0xA0A0A0);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Love from Bi_Diu"), centerX,
                this.height - 26, 0xB8B8B8);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("dev: ChatGPT"), centerX,
                this.height - 15, 0x8F8F8F);
        this.templateField.render(context, mouseX, mouseY, delta);
        this.renderModeDropdown(context, mouseX, mouseY);
    }

    private void renderModeDropdown(DrawContext context, int mouseX, int mouseY) {
        int x = this.getModeDropdownX();
        int y = this.getModeDropdownY();
        String selectedMode = this.config.selectedMode;
        String selectedLabel = selectedMode + "  " + HeypassedCarrierConfig.getModeDisplayName(selectedMode);

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 200.0F);
        context.fill(x, y, x + MODE_DROPDOWN_WIDTH, y + MODE_DROPDOWN_HEIGHT, 0xFF1F1F1F);
        context.fill(x + 1, y + 1, x + MODE_DROPDOWN_WIDTH - 1, y + MODE_DROPDOWN_HEIGHT - 1, 0xFF3B3B3B);
        context.drawTextWithShadow(this.textRenderer, Text.literal("玩法: " + selectedLabel), x + 6, y + 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, this.modeDropdownExpanded ? Text.literal("^") : Text.literal("v"),
                x + MODE_DROPDOWN_WIDTH - 10, y + 6, 0xFFFFFF);

        if (!this.modeDropdownExpanded) {
            context.getMatrices().pop();
            return;
        }

        int listY = y + MODE_DROPDOWN_HEIGHT;
        int visibleRows = Math.min(MODE_DROPDOWN_VISIBLE_ROWS, HeypassedCarrierConfig.GAME_MODES.size());
        int listHeight = visibleRows * MODE_DROPDOWN_HEIGHT;

        context.fill(x, listY, x + MODE_DROPDOWN_WIDTH, listY + listHeight, 0xFF1F1F1F);
        context.enableScissor(x, listY, x + MODE_DROPDOWN_WIDTH, listY + listHeight);

        for (int i = 0; i < visibleRows; i++) {
            int modeIndex = this.modeDropdownScrollOffset + i;
            String mode = HeypassedCarrierConfig.GAME_MODES.get(modeIndex);
            String modeLabel = mode + "  " + HeypassedCarrierConfig.getModeDisplayName(mode);
            int optionY = listY + i * MODE_DROPDOWN_HEIGHT;
            boolean hovered = this.isPointWithin(mouseX, mouseY, x, optionY, MODE_DROPDOWN_WIDTH, MODE_DROPDOWN_HEIGHT);
            boolean selected = mode.equals(this.config.selectedMode);

            int bgColor = hovered ? 0xFF4A4A4A : 0xFF2E2E2E;
            if (selected) {
                bgColor = hovered ? 0xFF5D6F8A : 0xFF4A5B74;
            }

            context.fill(x, optionY, x + MODE_DROPDOWN_WIDTH, optionY + MODE_DROPDOWN_HEIGHT, 0xFF1F1F1F);
            context.fill(x + 1, optionY + 1, x + MODE_DROPDOWN_WIDTH - 1, optionY + MODE_DROPDOWN_HEIGHT - 1, bgColor);
            context.drawTextWithShadow(this.textRenderer, Text.literal(modeLabel), x + 6, optionY + 6, 0xFFFFFF);
        }

        context.disableScissor();
        this.renderDropdownScrollIndicator(context, x, listY, listHeight);
        context.getMatrices().pop();
    }

    private boolean handleModeDropdownClick(double mouseX, double mouseY) {
        int x = this.getModeDropdownX();
        int y = this.getModeDropdownY();

        if (this.isPointWithin(mouseX, mouseY, x, y, MODE_DROPDOWN_WIDTH, MODE_DROPDOWN_HEIGHT)) {
            this.modeDropdownExpanded = !this.modeDropdownExpanded;
            return true;
        }

        if (!this.modeDropdownExpanded) {
            return false;
        }

        int visibleRows = Math.min(MODE_DROPDOWN_VISIBLE_ROWS, HeypassedCarrierConfig.GAME_MODES.size());
        for (int i = 0; i < visibleRows; i++) {
            int modeIndex = this.modeDropdownScrollOffset + i;
            int optionY = y + MODE_DROPDOWN_HEIGHT + i * MODE_DROPDOWN_HEIGHT;
            if (this.isPointWithin(mouseX, mouseY, x, optionY, MODE_DROPDOWN_WIDTH, MODE_DROPDOWN_HEIGHT)) {
                this.config.setSelectedMode(HeypassedCarrierConfig.GAME_MODES.get(modeIndex));
                this.modeDropdownExpanded = false;
                return true;
            }
        }

        this.modeDropdownExpanded = false;
        return false;
    }

    private int getModeDropdownX() {
        return this.width / 2 - 110;
    }

    private int getModeDropdownY() {
        return this.height / 4 + 45;
    }

    private int getModeDropdownListHeight() {
        return Math.min(MODE_DROPDOWN_VISIBLE_ROWS, HeypassedCarrierConfig.GAME_MODES.size()) * MODE_DROPDOWN_HEIGHT;
    }

    private int getModeDropdownMaxScrollOffset() {
        return Math.max(0, HeypassedCarrierConfig.GAME_MODES.size() - MODE_DROPDOWN_VISIBLE_ROWS);
    }

    private void renderDropdownScrollIndicator(DrawContext context, int x, int y, int height) {
        int maxOffset = this.getModeDropdownMaxScrollOffset();
        if (maxOffset <= 0) {
            return;
        }

        int barX = x + MODE_DROPDOWN_WIDTH - 4;
        context.fill(barX, y + 1, barX + 2, y + height - 1, 0xFF1A1A1A);

        int thumbHeight = Math.max(12, height * MODE_DROPDOWN_VISIBLE_ROWS / HeypassedCarrierConfig.GAME_MODES.size());
        int thumbTravel = height - thumbHeight;
        int thumbY = y + (int) Math.round((double) thumbTravel * this.modeDropdownScrollOffset / maxOffset);
        context.fill(barX, thumbY + 1, barX + 2, thumbY + thumbHeight - 1, 0xFF9E9E9E);
    }

    private boolean isPointWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}