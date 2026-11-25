package local.terminal.mixin;

import local.terminal.TerminalOrders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This EMI build dispatches recipe clicks from MouseHandler before normal screen events.
@Mixin(value = MouseHandler.class, priority = 1500, remap = false)
abstract class TerminalMouseMixin {
    @Unique private boolean ae2emiterminal$ordered;

    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"), cancellable = true)
    private void ae2emiterminal$order(long handle, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (button.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        var client = Minecraft.getInstance();
        var window = client.getWindow();
        if (window.handle() != handle) return;
        if (action == GLFW.GLFW_PRESS) {
            var mouse = client.mouseHandler;
            ae2emiterminal$ordered = TerminalOrders.tryOrder(client, (int) mouse.getScaledXPos(window), (int) mouse.getScaledYPos(window));
            if (ae2emiterminal$ordered) ci.cancel();
        } else if (action == GLFW.GLFW_RELEASE && ae2emiterminal$ordered) {
            ae2emiterminal$ordered = false;
            ci.cancel();
        }
    }
}
