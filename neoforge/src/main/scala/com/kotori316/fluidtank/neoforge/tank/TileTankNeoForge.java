package com.kotori316.fluidtank.neoforge.tank;

import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidConnection;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.neoforge.message.FluidTankContentMessageNeoForge;
import com.kotori316.fluidtank.neoforge.message.PacketHandler;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.kotori316.fluidtank.tank.VisualTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TileTankNeoForge extends TileTank {
    public TileTankNeoForge(Tier tier, BlockPos p, BlockState s) {
        super(tier, p, s);
    }

    public TileTankNeoForge(BlockPos p, BlockState s) {
        super(p, s);
    }

    @NotNull
    private IFluidHandler fluidHandler = createHandler();
    public final VisualTank visualTank = new VisualTank();
    private long lastUpdate = -1;
    private boolean updateScheduled = false;

    @Override
    public void onTickLoading() {
        super.onTickLoading();
        if (this.updateScheduled && this.level != null && !this.level.isClientSide) {
            long now = level.getGameTime();
            var minDelay = PlatformConfigAccess.getInstance().getConfig().minUpdateDelay();
            if (now - lastUpdate >= minDelay) {
                PacketHandler.sendToClient(new FluidTankContentMessageNeoForge(this), level);
                this.lastUpdate = now;
                this.updateScheduled = false;
            }
        }
    }

    @Override
    public void setConnection(FluidConnection c) {
        super.setConnection(c);
        this.invalidateCapabilities();
        this.fluidHandler = createHandler();
    }

    @Override
    public void setTank(Tank<FluidLike> tank) {
        if (this.getTank().equals(tank)) return;
        super.setTank(tank);
        if (this.level != null && !this.level.isClientSide) { // In server side
            long now = level.getGameTime();
            var minDelay = PlatformConfigAccess.getInstance().getConfig().minUpdateDelay();
            if (lastUpdate >= 0 && now - lastUpdate < minDelay) { // Throttle quick tank updates
                this.updateScheduled = true;
                return;
            }
            PacketHandler.sendToClient(new FluidTankContentMessageNeoForge(this), level);
            this.lastUpdate = now;
            this.updateScheduled = false;
        } else {
            // In client side
            // If level is null, it is the instance in RenderItemTank
            visualTank.updateContent(tank.capacity(), tank.amount(), tank.content().isGaseous());
        }
    }

    @NotNull
    public IFluidHandler getCapability(@Nullable Direction ignored) {
        return this.fluidHandler;
    }

    @NotNull
    private IFluidHandler createHandler() {
        return new ConnectionHandler(this.getConnection());
    }

}
